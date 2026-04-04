package yourmusic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import yourmusic.code.ErrorLogger;
import java.io.IOException;

public class YourMusic extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(YourMusic.class.getResource("/view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 360, 600);
        stage.setTitle("Your Music!");
        stage.setResizable(false);
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/image/mainImage.png")));
        }
        catch (NullPointerException e){
            ErrorLogger.log(214, ErrorLogger.Level.WARN, " In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName() +
                    " | Exception: " + e.getMessage());
        }

        scene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
        stage.setScene(scene);
        stage.show();
    }
}
