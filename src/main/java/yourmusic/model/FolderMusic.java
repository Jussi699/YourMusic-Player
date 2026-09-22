package yourmusic.model;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class FolderMusic {
    public static File choiserFile(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            return selectedDirectory;
        }
        return Paths.get(System.getProperty("user.home"), "Music").toFile();
    }

    public static List<String> getMusicPaths(File folder) {
        File[] files = folder.listFiles((_, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(".mp3"));

        List<String> music = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    music.add(file.getPath());
                }
            }
        }

        return music;
    }
}
