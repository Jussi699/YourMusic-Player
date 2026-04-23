package yourmusic.model;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import yourmusic.logger.ErrorLogger;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
        File[] files = folder.listFiles((dir, name) ->
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

    public static Image getIconFile(File path){
        try {
            Icon icon = FileSystemView.getFileSystemView().getSystemIcon(path);

            BufferedImage bufferedImage = new BufferedImage(
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2d = bufferedImage.createGraphics();
            icon.paintIcon(null, g2d, 0, 0);
            g2d.dispose();

            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (IllegalArgumentException e) {
            ErrorLogger.logError(103, "Incorrect icon size for file " + path.getName(), e);
            return null;

        } catch (NullPointerException e) {
            ErrorLogger.logError(104, "NullPointerException when receiving an icon for " + path.getName(), e);
            return null;

        } catch (Exception e) {
            ErrorLogger.logError(105, "Unexpected exception error while getting icon for " + path.getName(), e);
            return null;
        }
    }
}
