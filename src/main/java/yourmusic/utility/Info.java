package yourmusic.utility;

import yourmusic.logger.ErrorLogger;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 The Info class writes user data to a file for later use.
 */
public class Info {
    private static final String FILE_NAME = System.getProperty("user.home") + File.separator + "settings.info";
    private static final String DEFAULT_VOLUME = "10.0";

    public static void save(String key, String value) {
        File file = new File(FILE_NAME);
        Properties props = new Properties();

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            } else {
                file.createNewFile();
            }

            props.setProperty(key, value);

            try (OutputStream out = new FileOutputStream(file)) {
                props.store(out, "User Settings");
            }

        } catch (IOException e) {
            ErrorLogger.log(215, ErrorLogger.Level.WARN, " In: Class: " + Info.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName() +
                    " | Exception: " + e.getMessage());
        }
    }


    public static String get(String key) {
        File file = new File(FILE_NAME);
        Properties tempProps = new Properties();

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
                save("volume", DEFAULT_VOLUME);
                return DEFAULT_VOLUME;
            }

            try (InputStream in = new FileInputStream(file)) {
                tempProps.load(in);
            }

            String value = tempProps.getProperty(key);

            if (value == null || value.isBlank()) {
                if ("volume".equals(key)) {
                    save(key, DEFAULT_VOLUME);
                    return DEFAULT_VOLUME;
                }
                return "";
            }
            return value;
        } catch (IOException e) {
            ErrorLogger.log(215, ErrorLogger.Level.WARN, " In: Class: " + Info.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName() +
                    " | Exception: " + e.getMessage());
            return DEFAULT_VOLUME;
        }
    }
}
