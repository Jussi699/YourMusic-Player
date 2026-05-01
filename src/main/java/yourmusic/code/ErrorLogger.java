package yourmusic.code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLogger {
    private static final Logger logger = LoggerFactory.getLogger(ErrorLogger.class);

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public static void log(int errorCode, Level level, String message) {
        String msg = String.format("[%s] [%d] %s", level, errorCode, message);

        switch (level) {
            case DEBUG:
                logger.debug(msg);
                break;
            case INFO:
                logger.info(msg);
                break;
            case WARN:
                logger.warn(msg);
                break;
            case ERROR:
                logger.error(msg);
                break;
        }
    }

    public static void logError(int errorCode, String message, Throwable t) {
        String msg = String.format("[ERROR] [%d] %s", errorCode, message);
        logger.error(msg, t);
    }

    public static String getCurrentMethodName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return stack[2].getMethodName();
    }
}
