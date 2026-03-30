package yourmusic.code;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;

public class MusicPlayer {
    public static MediaPlayer createPlayer(String path){
        File file = new File(path);
        Media media = new Media(file.toURI().toString());
        return new MediaPlayer(media);
    }

    public static String formatTimeForStartLabel(Duration elapsed, Duration total) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed % 60;

        if (total.greaterThan(Duration.ZERO)) {
            return String.format("%02d:%02d",
                    elapsedMinutes, elapsedSeconds);
        } else {
            return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
        }
    }

    public static String formatTimeForEndLabel(Duration total) {
        int totalMinutes = 0;
        int totalSeconds = 0;

        if (total.greaterThan(Duration.ZERO)) {
            int intTotal = (int) Math.floor(total.toSeconds());
            totalMinutes = intTotal / 60;
            totalSeconds = intTotal % 60;
            return String.format("%02d:%02d",
                   totalMinutes, totalSeconds);
        } else {
            return String.format("%02d:%02d", totalMinutes, totalSeconds);
        }
    }
}
