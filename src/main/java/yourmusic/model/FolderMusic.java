package yourmusic.model;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.file.attribute.BasicFileAttributes;
import yourmusic.logger.ErrorLogger;

public class FolderMusic {
    private FolderMusic() {
        /* This utility class should not be instantiated */
    }

    public static File choiserFile(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Add music folder");

        return directoryChooser.showDialog(stage);
    }

    public static List<String> getMusicPaths(File folder) throws IOException {
        List<String> music = new ArrayList<>();
        Path root = folder.toPath();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isRegularFile()
                        && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                    music.add(file.toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) {
                ErrorLogger.log(204, ErrorLogger.Level.WARN,
                        "Could not scan music folder entry " + file + ": " + exception.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        return music;
    }
}
