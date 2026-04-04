module yourmusic {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires java.desktop;
    requires javafx.media;
    requires javafx.swing;
    requires org.slf4j;

    opens yourmusic to javafx.fxml;
    exports yourmusic;
}