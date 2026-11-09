package yourmusic.utility;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.EnumMap;
import java.util.Map;

public record AppSettings(boolean folderRandom, Map<HotkeyAction, HotkeyBinding> hotkeys, double volume) {
    public AppSettings {
        EnumMap<HotkeyAction, HotkeyBinding> copy = new EnumMap<>(HotkeyAction.class);
        copy.putAll(hotkeys);
        for (HotkeyAction action : HotkeyAction.values()) {
            copy.putIfAbsent(action, HotkeyBinding.defaultFor(action));
        }
        hotkeys = Map.copyOf(copy);
        if (!Double.isFinite(volume)) {
            volume = 10.0;
        }
        volume = Math.clamp(volume, 0.0, 100.0);
        volume = Math.round(volume * 100.0) / 100.0;
    }

    public static AppSettings defaults() {
        EnumMap<HotkeyAction, HotkeyBinding> defaults = new EnumMap<>(HotkeyAction.class);
        for (HotkeyAction action : HotkeyAction.values()) {
            defaults.put(action, HotkeyBinding.defaultFor(action));
        }
        return new AppSettings(false, defaults, 10.0);
    }

    public enum HotkeyAction {
        PLAY_PAUSE("Play / pause"),
        NEXT_TRACK("Next track"),
        PREVIOUS_TRACK("Previous track"),
        SHUFFLE("Shuffle"),
        REPEAT("Repeat track"),
        CURRENT_TRACK("Scroll to current track"),
        VOLUME_UP("Volume up"),
        VOLUME_DOWN("Volume down"),
        SEEK_FORWARD("Seek forward"),
        SEEK_BACKWARD("Seek backward");

        private final String label;

        HotkeyAction(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record HotkeyBinding(String keyCode, boolean control, boolean alt, boolean shift) {
        public HotkeyBinding {
            KeyCode.valueOf(keyCode);
        }

        public static HotkeyBinding defaultFor(HotkeyAction action) {
            return switch (action) {
                case PLAY_PAUSE -> new HotkeyBinding("SPACE", true, true, false);
                case NEXT_TRACK -> new HotkeyBinding("RIGHT", true, true, false);
                case PREVIOUS_TRACK -> new HotkeyBinding("LEFT", true, true, false);
                case SHUFFLE -> new HotkeyBinding("R", true, true, false);
                case REPEAT -> new HotkeyBinding("T", true, true, false);
                case CURRENT_TRACK -> new HotkeyBinding("C", true, true, false);
                case VOLUME_UP -> new HotkeyBinding("UP", true, true, false);
                case VOLUME_DOWN -> new HotkeyBinding("DOWN", true, true, false);
                case SEEK_FORWARD -> new HotkeyBinding("PAGE_UP", true, true, false);
                case SEEK_BACKWARD -> new HotkeyBinding("PAGE_DOWN", true, true, false);
            };
        }

        public static HotkeyBinding from(KeyEvent event) {
            return new HotkeyBinding(
                    event.getCode().name(),
                    event.isControlDown(),
                    event.isAltDown(),
                    event.isShiftDown());
        }

        public boolean hasModifier() {
            return control || alt || shift;
        }

        public String displayText() {
            StringBuilder text = new StringBuilder();
            if (control) text.append("Ctrl+");
            if (alt) text.append("Alt+");
            if (shift) text.append("Shift+");
            text.append(KeyCode.valueOf(keyCode).getName());
            return text.toString();
        }

        public boolean matches(KeyEvent event) {
            return event.getCode() == KeyCode.valueOf(keyCode)
                    && event.isControlDown() == control
                    && event.isAltDown() == alt
                    && event.isShiftDown() == shift;
        }
    }
}
