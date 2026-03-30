package yourmusic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class YourMusic extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(YourMusic.class.getResource("/jussi699/view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        stage.setTitle("Your Music!");
        stage.setResizable(false);
        stage.getIcons().add(new Image(new File(System.getProperty("user.dir") + "/src/image/mainImage.png").toURI().toString()));
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/jussi699/style.css")));
        stage.setScene(scene);
        stage.show();
    }
}
