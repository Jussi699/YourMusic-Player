module yourmusic {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.desktop;
    requires javafx.media;
    requires org.slf4j;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    opens yourmusic.app to javafx.fxml;
    exports yourmusic.app;
}