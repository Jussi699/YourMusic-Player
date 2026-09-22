package yourmusic.utility;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;

public class SetupItems {
    private SetupItems() {
        /* This utility class should not be instantiated */
    }

    public static void bindSliderToProgressBar(Slider slider, ProgressBar progressBar) {
        if (slider == null || progressBar == null) return;

        progressBar.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            double max = slider.getMax();
            double min = slider.getMin();
            double range = max - min;
            if (range <= 0) {
                return 0.0;
            }
            return Math.clamp((slider.getValue() - min) / range, 0.0, 1.0);
        }, slider.valueProperty(), slider.minProperty(), slider.maxProperty()));
    }
}
