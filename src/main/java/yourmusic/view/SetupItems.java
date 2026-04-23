package yourmusic.view;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import yourmusic.Controller;
import yourmusic.logger.ErrorLogger;

import java.io.InputStream;
import java.util.Locale;

public class SetupItems {
    public static void setupSliderVisual(Slider slider, String activeColor, String inactiveColor) {
        Runnable update = () -> {
            double max = slider.getMax();
            double percentage = max > 0 ? (slider.getValue() / max) * 100 : 0;

            Node track = slider.lookup(".track");
            if (track != null) {
                track.setStyle(String.format(
                        Locale.US,
                        "-fx-background-color: linear-gradient(to right, %s %.2f%%, %s %.2f%%);",
                        activeColor, percentage, inactiveColor, percentage
                ));
            }
        };

        slider.valueProperty().addListener((_, _, _) -> update.run());
        slider.maxProperty().addListener((_, _, _) -> update.run());
        Platform.runLater(update);
    }

    public static void updateButtonIcon(String path, Labeled btn, int height, int width) {
        try (InputStream resource = SetupItems.class.getResourceAsStream(path)) {
            if (resource == null) {
                ErrorLogger.log(207, ErrorLogger.Level.WARN, " File not found | In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
                return;
            }

            Image icon = new Image(resource);
            ImageView view;

            if (btn.getGraphic() instanceof ImageView) {
                view = (ImageView) btn.getGraphic();
            } else {
                view = new ImageView();
                btn.setGraphic(view);
            }

            view.setImage(null);
            view.setImage(icon);
            view.setFitHeight(height);
            view.setFitWidth(width);

        } catch (NullPointerException e) {
            ErrorLogger.log(208, ErrorLogger.Level.WARN, " In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        } catch (Exception e) {
            ErrorLogger.log(209, ErrorLogger.Level.WARN, " In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        }
    }
}
