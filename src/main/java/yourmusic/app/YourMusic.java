package yourmusic.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import yourmusic.logger.ErrorLogger;
import yourmusic.utility.AppSettings;
import yourmusic.utility.AppSettingsStore;

import java.io.IOException;
import java.util.Objects;

public class YourMusic extends Application {
    private static final double WINDOW_WIDTH = 360;
    private static final double WINDOW_HEIGHT = 640;

    @Override
    public void start(Stage stage) throws IOException {
        AppSettings appSettings = AppSettingsStore.loadOrCreate();
        FXMLLoader fxmlLoader = new FXMLLoader(YourMusic.class.getResource("/view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
        Controller controller = fxmlLoader.getController();
        controller.setAppScene(scene);
        controller.setAppSettings(appSettings);
        stage.setTitle("Your Music!");
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/mainImage.png"))));
        }
        catch (NullPointerException e){
            ErrorLogger.log(214, ErrorLogger.Level.WARN, e.getMessage());
        }

        scene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
