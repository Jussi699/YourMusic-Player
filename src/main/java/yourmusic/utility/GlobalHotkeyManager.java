package yourmusic.utility;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.application.Platform;
import yourmusic.logger.ErrorLogger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public final class GlobalHotkeyManager implements AutoCloseable {
    private static final int MOD_ALT = 0x0001;
    private static final int MOD_CONTROL = 0x0002;
    private static final int MOD_SHIFT = 0x0004;
    private volatile boolean running;
    private volatile int nativeThreadId;
    private Thread thread;

    public synchronized void register(
            AppSettings settings,
            Consumer<AppSettings.HotkeyAction> actionHandler) {
        close();
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }

        running = true;
        CountDownLatch queueReady = new CountDownLatch(1);
        thread = new Thread(() -> runMessageLoop(settings, actionHandler, queueReady), "YourMusic-Hotkeys");
        thread.setDaemon(true);
        thread.start();
        try {
            queueReady.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ErrorLogger.logError(310, "Interrupted while starting global hotkeys", e);
            close();
        }
    }

    private void runMessageLoop(
            AppSettings settings,
            Consumer<AppSettings.HotkeyAction> actionHandler,
            CountDownLatch queueReady) {
        Map<Integer, AppSettings.HotkeyAction> registered = new LinkedHashMap<>();
        try {
            WinUser.MSG message = new WinUser.MSG();
            User32.INSTANCE.PeekMessage(message, null, 0, 0, 0);
            nativeThreadId = Kernel32.INSTANCE.GetCurrentThreadId();
            queueReady.countDown();

            int id = 1;
            for (Map.Entry<AppSettings.HotkeyAction, AppSettings.HotkeyBinding> entry : settings.hotkeys().entrySet()) {
                AppSettings.HotkeyBinding binding = entry.getValue();
                if (!binding.hasModifier()) {
                    ErrorLogger.log(310, ErrorLogger.Level.WARN,
                            "Global hotkey requires Ctrl, Alt, or Shift: " + entry.getKey());
                    continue;
                }

                int modifiers = (binding.control() ? MOD_CONTROL : 0)
                        | (binding.alt() ? MOD_ALT : 0)
                        | (binding.shift() ? MOD_SHIFT : 0);
                int virtualKey = javafx.scene.input.KeyCode.valueOf(binding.keyCode()).getCode();
                if (!User32.INSTANCE.RegisterHotKey(null, id, modifiers, virtualKey)) {
                    ErrorLogger.log(310, ErrorLogger.Level.WARN,
                            "Windows could not register global hotkey " + binding.displayText()
                                    + " for " + entry.getKey());
                    continue;
                }
                registered.put(id++, entry.getKey());
            }

            while (running) {
                int result = User32.INSTANCE.GetMessage(message, null, 0, 0);
                if (result <= 0) {
                    break;
                }
                if (message.message == WinUser.WM_HOTKEY) {
                    AppSettings.HotkeyAction action = registered.get(message.wParam.intValue());
                    if (action != null) {
                        Platform.runLater(() -> actionHandler.accept(action));
                    }
                }
            }
        } catch (RuntimeException e) {
            ErrorLogger.logError(310, "Global hotkey registration failed", e);
        } catch (LinkageError e) {
            ErrorLogger.logError(310, "Windows global hotkey support could not be loaded", e);
        } finally {
            for (Integer id : registered.keySet()) {
                User32.INSTANCE.UnregisterHotKey(null, id);
            }
            nativeThreadId = 0;
            running = false;
            queueReady.countDown();
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        Thread activeThread = thread;
        int threadId = nativeThreadId;
        if (threadId != 0 && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            int posted = User32.INSTANCE.PostThreadMessage(
                    threadId,
                    WinUser.WM_QUIT,
                    new WinDef.WPARAM(0),
                    new WinDef.LPARAM(0));
            if (posted == 0) {
                ErrorLogger.log(310, ErrorLogger.Level.WARN, "Could not stop global hotkey message loop");
            }
        }
        if (activeThread != null && activeThread != Thread.currentThread()) {
            try {
                activeThread.join(1000);
                if (activeThread.isAlive()) {
                    ErrorLogger.log(310, ErrorLogger.Level.WARN, "Global hotkey thread did not stop in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ErrorLogger.logError(310, "Interrupted while stopping global hotkeys", e);
            }
        }
        thread = null;
    }
}
