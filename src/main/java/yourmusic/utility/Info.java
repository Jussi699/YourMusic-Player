package yourmusic.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

final class Info {
     private static final String FILE_NAME = System.getProperty("user.home") + File.separator + "settings.info";

     private Info() {
     }

     static String readLegacyVolume() throws IOException {
         File file = new File(FILE_NAME);
         if (!file.isFile()) {
             return null;
         }
        Properties properties = new Properties();
         try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        }
        return properties.getProperty("volume");
     }
}
