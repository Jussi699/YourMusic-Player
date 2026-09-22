module yourmusic {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.desktop;
    requires javafx.media;
    requires org.slf4j;

    opens yourmusic.app to javafx.fxml;
    exports yourmusic.app;
}