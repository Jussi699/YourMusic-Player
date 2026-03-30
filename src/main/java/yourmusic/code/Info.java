package yourmusic.code;

import java.io.*;
import java.util.Properties;

public class Info {
    private static final String FILE_NAME = System.getProperty("user.dir") + File.separator + "settings.info";
    private static final Properties props = new Properties();

    public static void save(String key, String value) {
        try (OutputStream out = new FileOutputStream(FILE_NAME)) {
            props.setProperty(key, value);
            props.store(out, "User Settings");
        } catch (IOException e) {
            ErrorLogger.log(201, ErrorLogger.Level.WARN, " In: Class: " + Info.class.getName() +  " Method: " + ErrorLogger.getCurrentMethodName());
        }
    }

    public static String get(String key) {
        try (InputStream in = new FileInputStream(FILE_NAME)) {
            Properties tempProps = new Properties();
            tempProps.load(in);
            return tempProps.getProperty(key);
        } catch (IOException e) {
            ErrorLogger.log(202, ErrorLogger.Level.WARN, " In: Class: " + Info.class.getName() +  " Method: " + ErrorLogger.getCurrentMethodName());
            return "0-0-0";
        }
    }
}
