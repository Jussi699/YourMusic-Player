package yourmusic.utility;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Slider;

import java.util.Locale;

public class SetupItems {
    private SetupItems() {
        /* This utility class should not be instantiated */
    }

    public static void setupSliderVisual(Slider slider) {
        Runnable update = () -> {
            double max = slider.getMax();
            double percentage = max > 0 ? (slider.getValue() / max) * 100 : 0;

            Node track = slider.lookup(".track");
            if (track != null) {
                track.setStyle(String.format(
                        Locale.US,
                        "-fx-background-color: linear-gradient(to right, %s %.2f%%, %s %.2f%%);",
                        "#1f7e1f", percentage, "#696c6e", percentage
                ));
            }
        };

        slider.valueProperty().addListener((_, _, _) -> update.run());
        slider.maxProperty().addListener((_, _, _) -> update.run());
        Platform.runLater(update);
    }
}
