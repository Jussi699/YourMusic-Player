package yourmusic.utility;

import java.io.File;
import java.util.List;

public class Util {
    public static String findPathByDisplayedName(String selectedSong, List<String> musicData) {
        for (String musicPath : musicData) {
            if (musicPath == null) {
                continue;
            }

            File file = new File(musicPath);
            String fileName = file.getName();

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = fileName.substring(0, dotIndex);
            }

            if (fileName.equals(selectedSong)) {
                return musicPath;
            }
        }
        return null;
    }
}
