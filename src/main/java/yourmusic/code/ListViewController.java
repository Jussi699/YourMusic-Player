package yourmusic.code;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import yourmusic.Controller;

import java.io.File;
import java.util.HashMap;

public class ListViewController extends ListCell<String> {
    public static void setItem(File folder, HashMap<Integer, String> musicData, ListView<String> listView) {
        if (folder != null) {
            ObservableList<String> items = FXCollections.observableArrayList();

            for (int i = 0; i < musicData.size(); i++) {
                String fullPath = musicData.get(i);
                if (fullPath != null) {
                    items.add(new File(fullPath).getName());
                }
            }
            listView.setItems(items);
        }
        else {
            ErrorLogger.log(207, ErrorLogger.Level.WARN ," In: Class: " + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        }
    }
}