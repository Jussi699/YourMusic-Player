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
    private Info() {
        /* This utility class should not be instantiated */
    }

    private static final String FILE_NAME = System.getProperty("user.home") + File.separator + "settings.info";
    private static final String DEFAULT_VOLUME = "10.0";

    public static void save(String key, String value) {
        File file = new File(FILE_NAME);
        Properties props = new Properties();

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if(!parentDir.mkdirs()) {
                    ErrorLogger.log(109, ErrorLogger.Level.ERROR, "An error occurred while creating directory!");
                }
            }

            if (file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            } else {
                try {
                    file.createNewFile();
                }
                catch (IOException e) {
                    ErrorLogger.log(108, ErrorLogger.Level.ERROR, e.getMessage());
                }


            }

            props.setProperty(key, value);

            try (OutputStream out = new FileOutputStream(file)) {
                props.store(out, "Your Music | User Settings ");
            }

        } catch (IOException e) {
            ErrorLogger.log(215, ErrorLogger.Level.WARN, e.getMessage());
        }
    }


    public static String get(String key) {
        File file = new File(FILE_NAME);
        Properties tempProps = new Properties();

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if(!parentDir.mkdirs()) {
                    ErrorLogger.log(110, ErrorLogger.Level.ERROR, "An error occurred while creating directory!");
                }
            }

            if (!file.exists() && file.createNewFile()) {
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
            ErrorLogger.log(215, ErrorLogger.Level.WARN, e.getMessage());
            return DEFAULT_VOLUME;
        }
    }
}
