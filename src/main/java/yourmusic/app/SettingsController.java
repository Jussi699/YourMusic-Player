package yourmusic.app;

import javafx.collections.FXCollections;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import yourmusic.utility.AppSettings;
import yourmusic.utility.AppSettingsStore;
import yourmusic.utility.LibraryStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SettingsController {
    @FXML private ListView<FolderOption> folderList;
    @FXML private Button btnAddFolder;
    @FXML private Label statusLabel;
    @FXML private CheckBox folderRandomToggle;
    @FXML private VBox hotkeyList;

    private Runnable onBack;
    private Runnable onLibraryChanged;
    private Runnable onOpenFavorites;
    private Consumer<LibraryStore.FilterSettings> onFolderFiltersChanged;
    private Consumer<AppSettings> onAppSettingsChanged;
    private final Map<AppSettings.HotkeyAction, TextField> hotkeyFields =
            new EnumMap<>(AppSettings.HotkeyAction.class);
    private AppSettings appSettings = AppSettings.defaults();
    private LibraryStore.FilterSettings savedFilters = new LibraryStore.FilterSettings(false, List.of(), "", "");
    private final Set<String> knownFolderPaths = new LinkedHashSet<>();
    private boolean initialFoldersLoaded;

    @FXML
    private void initialize() {
        folderList.setItems(FXCollections.observableArrayList());
        folderList.getStyleClass().add("settings-folder-list");
        folderList.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(FolderOption folder, boolean empty) {
                super.updateItem(folder, empty);
                if (empty || folder == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    CheckBox checkBox = new CheckBox(new File(folder.path).getName());
                    checkBox.setSelected(folder.included.get());
                    checkBox.setOnAction(_ -> folder.included.set(checkBox.isSelected()));
                    setGraphic(checkBox);
                    setText(null);
                    setTooltip(new Tooltip(folder.path));
                }
            }
        });
        createHotkeyFields();
        try {
            savedFilters = LibraryStore.getFilterSettings();
        } catch (IOException e) {
            showStoreError(e);
        }
        refreshFolders();
    }

    void setCallbacks(
            Runnable onBack,
            Runnable onLibraryChanged,
            Runnable onOpenFavorites,
            Consumer<LibraryStore.FilterSettings> onFolderFiltersChanged,
            Consumer<AppSettings> onAppSettingsChanged,
            AppSettings appSettings) {
        this.onBack = onBack;
        this.onLibraryChanged = onLibraryChanged;
        this.onOpenFavorites = onOpenFavorites;
        this.onFolderFiltersChanged = onFolderFiltersChanged;
        this.onAppSettingsChanged = onAppSettingsChanged;
        this.appSettings = appSettings;
        folderRandomToggle.setSelected(appSettings.folderRandom());
        appSettings.hotkeys().forEach((action, binding) -> hotkeyFields.get(action).setText(binding.displayText()));
    }

    @FXML
    private void addFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose music folder");
        File selectedFolder = chooser.showDialog(btnAddFolder.getScene().getWindow());
        if (selectedFolder == null) {
            return;
        }

        try {
            LibraryStore.addMusicFolder(selectedFolder.getPath());
            boolean refreshed = refreshFolders();
            onLibraryChanged.run();
            if (refreshed) {
                statusLabel.setText("");
            }
        } catch (IOException e) {
            showStoreError(e);
        }
    }

    @FXML
    private void removeFolder() {
        FolderOption selected = folderList.getSelectionModel().getSelectedItem();
        String selectedFolder = selected == null ? null : selected.path;
        if (selectedFolder == null) {
            statusLabel.setText("Select a folder to remove.");
            return;
        }

        try {
            LibraryStore.removeMusicFolder(selectedFolder);
            boolean refreshed = refreshFolders();
            onLibraryChanged.run();
            if (refreshed) {
                statusLabel.setText("");
            }
        } catch (IOException e) {
            showStoreError(e);
        }
    }

    @FXML
    private void refreshLibrary() {
        onLibraryChanged.run();
        statusLabel.setText("");
    }

    @FXML
    private void applyFolderFilter() {
        LibraryStore.FilterSettings filters = new LibraryStore.FilterSettings(
                true,
                folderList.getItems().stream()
                        .filter(FolderOption::isIncluded)
                        .map(option -> option.path)
                        .toList(),
                "",
                "");
        try {
            LibraryStore.saveFilterSettings(filters);
            savedFilters = filters;
            onFolderFiltersChanged.accept(filters);
            statusLabel.setText("Folder selection applied.");
        } catch (IOException e) {
            showStoreError(e);
        }
    }

    @FXML
    private void openFavorites() {
        onOpenFavorites.run();
    }

    private void createHotkeyFields() {
        for (AppSettings.HotkeyAction action : AppSettings.HotkeyAction.values()) {
            Label label = new Label(action.label());
            label.getStyleClass().add("settings-hint");
            TextField field = new TextField();
            field.getStyleClass().add("hotkey-field");
            field.setEditable(false);
            field.setPromptText("Click and press keys");
            field.setOnMouseClicked(_ -> field.requestFocus());
            field.addEventFilter(KeyEvent.KEY_PRESSED, event -> captureHotkey(event, field));
            HBox row = new HBox(8, label, field);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(label, Priority.ALWAYS);
            hotkeyFields.put(action, field);
            hotkeyList.getChildren().add(row);
        }
    }

    private void captureHotkey(KeyEvent event, TextField field) {
        KeyCode code = event.getCode();
        if (code == KeyCode.CONTROL || code == KeyCode.ALT || code == KeyCode.SHIFT
            || code == KeyCode.META || code == KeyCode.WINDOWS || code == KeyCode.UNDEFINED) {
            event.consume();
            return;
        }
        AppSettings.HotkeyBinding binding = AppSettings.HotkeyBinding.from(event);
        field.setText(binding.displayText());
        field.getProperties().put("binding", binding);
        event.consume();
    }

    @FXML
    private void saveAppSettings() {
        EnumMap<AppSettings.HotkeyAction, AppSettings.HotkeyBinding> bindings =
                new EnumMap<>(AppSettings.HotkeyAction.class);
        Set<AppSettings.HotkeyBinding> uniqueBindings = new LinkedHashSet<>();
        for (AppSettings.HotkeyAction action : AppSettings.HotkeyAction.values()) {
            Object stored = hotkeyFields.get(action).getProperties().get("binding");
            AppSettings.HotkeyBinding binding = stored instanceof AppSettings.HotkeyBinding saved
                    ? saved : appSettings.hotkeys().get(action);
            if (!binding.hasModifier()) {
                statusLabel.setText("Every hotkey needs Ctrl, Alt, or Shift for Windows global registration.");
                return;
            }
            if (!uniqueBindings.add(binding)) {
                statusLabel.setText("Each action needs a different hotkey.");
                return;
            }
            bindings.put(action, binding);
        }

        AppSettings updated = new AppSettings(folderRandomToggle.isSelected(), bindings, appSettings.volume());
        try {
            AppSettingsStore.save(updated);
            appSettings = updated;
            onAppSettingsChanged.accept(updated);
            statusLabel.setText("Playback and hotkeys saved.");
        } catch (IOException e) {
            showStoreError(e);
        }
    }

    @FXML
    private void resetHotkeysToDefaults() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(hotkeyList.getScene().getWindow());
        confirmation.setTitle("Reset hotkeys");
        confirmation.setHeaderText("Restore default hotkeys?");
        confirmation.setContentText("Your current hotkey assignments will be replaced with the defaults.");
        confirmation.getDialogPane().getStyleClass().add("settings-confirmation");
        confirmation.getDialogPane().getStylesheets().addAll(hotkeyList.getScene().getStylesheets());

        if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        AppSettings defaults = AppSettings.defaults();
        AppSettings updated = new AppSettings(appSettings.folderRandom(), defaults.hotkeys(), appSettings.volume());
        try {
            AppSettingsStore.save(updated);
            appSettings = updated;
            for (AppSettings.HotkeyAction action : AppSettings.HotkeyAction.values()) {
                TextField field = hotkeyFields.get(action);
                field.setText(updated.hotkeys().get(action).displayText());
                field.getProperties().remove("binding");
            }
            onAppSettingsChanged.accept(updated);
            statusLabel.setText("Hotkeys restored to defaults.");
        } catch (IOException e) {
            showStoreError(e);
        }
    }

    private boolean refreshFolders() {
        try {
            List<String> folders = LibraryStore.getMusicFolders();
            Set<String> previouslyIncluded = new LinkedHashSet<>();
            for (FolderOption option : folderList.getItems()) {
                if (option.isIncluded()) {
                    previouslyIncluded.add(option.path);
                }
            }
            List<FolderOption> options = new ArrayList<>();
            for (String folder : folders) {
                boolean included;
                if (knownFolderPaths.contains(folder)) {
                    included = previouslyIncluded.contains(folder);
                } else if (initialFoldersLoaded) {
                    included = true;
                } else {
                    included = !savedFilters.configured() || savedFilters.folders().contains(folder);
                }
                options.add(new FolderOption(folder, included));
            }
            knownFolderPaths.clear();
            knownFolderPaths.addAll(folders);
            initialFoldersLoaded = true;
            folderList.getItems().setAll(options);
            return true;
        } catch (IOException e) {
            showStoreError(e);
            return false;
        }
    }

    private void showStoreError(IOException e) {
        statusLabel.setText("Could not save library: " + e.getMessage());
    }

    private static final class FolderOption {
        private final String path;
        private final BooleanProperty included;

        private FolderOption(String path, boolean included) {
            this.path = path;
            this.included = new SimpleBooleanProperty(included);
        }

        private boolean isIncluded() {
            return included.get();
        }
    }

    @FXML
    private void showPlayer() {
        onBack.run();
    }
}
