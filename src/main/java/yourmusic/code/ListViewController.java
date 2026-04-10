package yourmusic.code;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import yourmusic.Controller;

import java.io.File;
import java.util.List;

public final class ListViewController {
    private ListViewController() {
    }

    public static void setItems(File folder, List<String> musicData, ListView<String> listView) {
        if (folder != null) {
            ObservableList<String> items = FXCollections.observableArrayList();

            for (String fullPath : musicData) {
                if (fullPath != null) {
                    items.add(new File(fullPath).getName());
                }
            }

            listView.setItems(items);
        } else {
            ErrorLogger.log(
                    207,
                    ErrorLogger.Level.WARN,
                    " In: Class: " + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName()
            );
        }
    }
}