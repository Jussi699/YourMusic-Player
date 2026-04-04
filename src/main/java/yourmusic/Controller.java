package yourmusic;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;
import yourmusic.code.*;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;

public class Controller {
    private HashMap<Integer, String> musicData = new HashMap<>();
    public static final double volume = 400.0;

    private final ObservableList<String> masterSongs = FXCollections.observableArrayList();
    private final FilteredList<String> filteredSongs = new FilteredList<>(masterSongs, _ -> true);

    private final Deque<String> playHistory = new ArrayDeque<>();
    private boolean internalSelectionChange = false;
    private String currentSong = null;

    @FXML
    private Slider timeLineMusic;

    @FXML
    public Label labelTimeStart;

    @FXML
    public Label labelTimeEnd;

    @FXML
    private Pane bottomPanelPane;

    @FXML
    private Button btnPauseUnpause;

    @FXML
    private Button btnFolder;

    @FXML
    private ImageView imageMusic;

    @FXML
    private ListView<String> listView;

    @FXML
    private AnchorPane mainAnchorPane;

    @FXML
    private Pane mainPane;

    @FXML
    private Slider volumeMusic;

    @FXML
    private Button btnPreviousMusic;

    @FXML
    private Button btnNextMusic;

    @FXML
    private ToggleButton btnRepeatMusic;

    @FXML
    private ToggleButton btnRandomMusic;

    @FXML
    private TextField fieldSearch;

    @FXML
    void btnFolder(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File folder = FolderMusic.choiserFile(stage);

        if (folder != null && folder.exists() && folder.isDirectory()) {
            MediaPlayer currentPlayer = (MediaPlayer) listView.getUserData();

            if (currentPlayer != null) {
                currentPlayer.stop();
                currentPlayer.dispose();
                listView.setUserData(null);
            }

            listView.getSelectionModel().clearSelection();
            musicData.clear();
            playHistory.clear();
            currentSong = null;
            internalSelectionChange = false;

            musicData = FolderMusic.getIndexAndPathMusic(folder);
            reloadSongsFromMusicData();
        } else {
            ErrorLogger.log(203, ErrorLogger.Level.WARN, " In: Class: " + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        }
    }

    public void changeVolume() {
        Info.save("volume", String.valueOf(volumeMusic.getValue()));
    }

    @FXML
    void initialize() {
        assert bottomPanelPane != null : "fx:id=\"bottomPanelPane\"";
        assert btnPauseUnpause != null : "fx:id=\"btnPauseUnpause\"";
        assert btnFolder != null : "fx:id=\"btnFolder\"";
        assert imageMusic != null : "fx:id=\"imageMusic\"";
        assert listView != null : "fx:id=\"listView\"";
        assert mainAnchorPane != null : "fx:id=\"mainAnchorPane\"";
        assert mainPane != null : "fx:id=\"mainPane\"";
        assert volumeMusic != null : "fx:id=\"volumeMusic\"";
        assert timeLineMusic != null : "fx:id=\"timeLineMusic\"";
        assert labelTimeStart != null : "fx:id=\"labelTimeStart\"";
        assert labelTimeEnd != null : "fx:id=\"labelTimeEnd\"";
        assert btnNextMusic != null : "fx:id=\"btnNextMusic\"";
        assert btnPreviousMusic != null : "fx:id=\"btnPreviousMusic\"";
        assert btnRepeatMusic != null : "fx:id=\"btnRepeatMusic\"";
        assert btnRandomMusic != null : "fx:id=\"btnRandomMusic\"";
        assert fieldSearch != null : "fx:id=\"fieldSearch\"";

        listView.setItems(filteredSongs);

        reInitialize();
        setupTimelineBehavior();
        setupSliderVisual(volumeMusic, "#800080", "#696c6e");

        volumeMusic.valueProperty().addListener((_, _, newVal) -> {
            MediaPlayer current = (MediaPlayer) listView.getUserData();
            if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                current.setVolume(newVal.doubleValue() / volume);
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((_, _, selectedSong) -> {
            if (selectedSong == null || selectedSong.isBlank()) {
                return;
            }

            if (currentSong != null && !currentSong.equals(selectedSong) && !internalSelectionChange) {
                playHistory.push(currentSong);
            }

            MediaPlayer oldPlayer = (MediaPlayer) listView.getUserData();
            if (oldPlayer != null) {
                oldPlayer.stop();
                oldPlayer.dispose();
            }

            String path = findPathByDisplayedName(selectedSong);
            if (path == null) {
                ErrorLogger.log(215, ErrorLogger.Level.WARN, " Song path not found for selected item: " + selectedSong);
                return;
            }

            MediaPlayer newPlayer = MusicPlayer.createPlayer(path);
            if (newPlayer == null) {
                ErrorLogger.log(216, ErrorLogger.Level.WARN, " MediaPlayer is null for path: " + path);
                return;
            }

            currentSong = selectedSong;
            internalSelectionChange = false;

            listView.setUserData(newPlayer);
            newPlayer.setVolume(volumeMusic.getValue() / volume);

            updateButtonIcon("/image/play.png", btnPauseUnpause, 20, 20);

            newPlayer.setOnReady(() -> {
                timeLineMusic.setMax(newPlayer.getTotalDuration().toSeconds());
                labelTimeEnd.setText(MusicPlayer.formatTimeForEndLabel(newPlayer.getTotalDuration()));
                newPlayer.play();
            });

            newPlayer.currentTimeProperty().addListener((_, _, newTime) -> {
                if (!timeLineMusic.isValueChanging()) {
                    timeLineMusic.setValue(newTime.toSeconds());
                }
                labelTimeStart.setText(
                        MusicPlayer.formatTimeForStartLabel(newTime, newPlayer.getTotalDuration())
                );
            });

            newPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                PauseTransition delay = new PauseTransition(Duration.seconds(1));
                delay.setOnFinished(_ -> handleTrackEnd(newPlayer));
                delay.play();
            }));

            newPlayer.setOnError(() -> ErrorLogger.log(217, ErrorLogger.Level.WARN, " MediaPlayer error: " + newPlayer.getError()));
        });

        btnNextMusic.setOnAction(_ -> playNext());
        btnPreviousMusic.setOnAction(_ -> playPrevious());
        btnPauseUnpause.setOnAction(_ -> togglePlay());

        btnRepeatMusic.setOnMouseClicked(_ -> {
            if (btnRepeatMusic.isSelected()) {
                updateButtonIcon("/image/repeatOn.png", btnRepeatMusic, 15, 20);
            } else {
                updateButtonIcon("/image/repeatOff.png", btnRepeatMusic, 15, 20);
            }
        });

        btnRandomMusic.setOnMouseClicked(_ -> {
            if (btnRandomMusic.isSelected()) {
                updateButtonIcon("/image/randomOn.png", btnRandomMusic, 15, 20);
            } else {
                updateButtonIcon("/image/randomOff.png", btnRandomMusic, 15, 20);
            }
        });

        fieldSearch.textProperty().addListener((_, _, newValue) ->
                filteredSongs.setPredicate(song -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    return song.toLowerCase().startsWith(newValue.toLowerCase());
                })
        );

        Platform.runLater(() -> mainAnchorPane.getScene().addEventFilter(
                javafx.scene.input.KeyEvent.KEY_PRESSED,
                event -> {
                    MediaPlayer current = (MediaPlayer) listView.getUserData();

                    switch (event.getCode()) {
                        case LEFT, RIGHT -> {
                            if (current == null || current.getStatus() == MediaPlayer.Status.UNKNOWN) return;

                            double step = 5.0;
                            double newValue = timeLineMusic.getValue();

                            if (event.getCode() == javafx.scene.input.KeyCode.LEFT) {
                                newValue = Math.max(timeLineMusic.getMin(), newValue - step);
                            } else {
                                newValue = Math.min(timeLineMusic.getMax(), newValue + step);
                            }

                            timeLineMusic.setValue(newValue);
                            current.seek(Duration.seconds(newValue));
                            event.consume();
                        }
                        case UP -> {
                            double newVol = Math.min(volumeMusic.getMax(), volumeMusic.getValue() + 5);
                            volumeMusic.setValue(newVol);
                            event.consume();
                        }
                        case DOWN -> {
                            double newVol = Math.max(volumeMusic.getMin(), volumeMusic.getValue() - 5);
                            volumeMusic.setValue(newVol);
                            event.consume();
                        }
                    }
                }
        ));
    }

    private void reInitialize() {
        updateButtonIcon("/image/pause.png", btnPauseUnpause, 20, 20);
        updateButtonIcon("/image/prevMusic.png", btnPreviousMusic, 30, 30);
        updateButtonIcon("/image/nextMusic.png", btnNextMusic, 30, 30);
        updateButtonIcon("/image/repeatOff.png", btnRepeatMusic, 15, 20);
        updateButtonIcon("/image/randomOff.png", btnRandomMusic, 15, 20);

        btnRepeatMusic.setSelected(false);
        btnRandomMusic.setSelected(false);

        labelTimeEnd.getStyleClass().add("labelTimeLine");
        labelTimeStart.getStyleClass().add("labelTimeLine");
        btnNextMusic.getStyleClass().add("btnNavigationMusic");
        btnPreviousMusic.getStyleClass().add("btnNavigationMusic");
        btnRandomMusic.getStyleClass().add("btnNavigationMusic");
        btnRepeatMusic.getStyleClass().add("btnNavigationMusic");
        labelTimeStart.setText("00:00");
        labelTimeEnd.setText("00:00");

        playHistory.clear();
        currentSong = null;
        internalSelectionChange = false;

        String savedVolume = Info.get("volume");
        try {
            volumeMusic.setValue(Double.parseDouble(savedVolume));
        } catch (NumberFormatException e) {
            volumeMusic.setValue(10.0);
            Info.save("volume", "10.0");
        }

        String userHome = System.getProperty("user.home");
        File folder = new File(userHome + File.separator + "Music");

        if (folder.exists() && folder.isDirectory()) {
            musicData = FolderMusic.getIndexAndPathMusic(folder);
            reloadSongsFromMusicData();
        } else {
            ErrorLogger.log(205, ErrorLogger.Level.WARN, " In: Class: " + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        }
    }

    private void reloadSongsFromMusicData() {
        masterSongs.clear();

        for (int i = 0; i < musicData.size(); i++) {
            String path = musicData.get(i);
            if (path == null) continue;

            File file = new File(path);
            String fileName = file.getName();

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = fileName.substring(0, dotIndex);
            }

            masterSongs.add(fileName);
        }
    }

    private String findPathByDisplayedName(String selectedSong) {
        for (int i = 0; i < musicData.size(); i++) {
            String musicPath = musicData.get(i);
            if (musicPath == null) continue;

            File file = new File(musicPath);
            String fileName = file.getName();

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = fileName.substring(0, dotIndex);
            }

            if (fileName.equals(selectedSong)) {
                return musicPath;
            }
        }
        return null;
    }

    private void updateButtonIcon(String path, Labeled btn, int height, int width) {
        try {
            var resource = getClass().getResourceAsStream(path);
            if (resource == null) {
                ErrorLogger.log(207, ErrorLogger.Level.WARN, " File not found | In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
                return;
            }

            Image icon = new Image(resource);
            ImageView view;

            if (btn.getGraphic() instanceof ImageView) {
                view = (ImageView) btn.getGraphic();
            } else {
                view = new ImageView();
                btn.setGraphic(view);
            }

            view.setImage(null);
            view.setImage(icon);
            view.setFitHeight(height);
            view.setFitWidth(width);

        } catch (NullPointerException e) {
            ErrorLogger.log(208, ErrorLogger.Level.WARN, " In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        } catch (Exception e) {
            ErrorLogger.log(209, ErrorLogger.Level.WARN, " In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        }
    }

    private void playNext() {
        if (listView.getItems().isEmpty()) return;

        if (currentSong != null && !currentSong.isBlank()) {
            playHistory.push(currentSong);
        }

        if (btnRandomMusic.isSelected()) {
            playRandomSong();
            return;
        }

        int currentIndex = listView.getSelectionModel().getSelectedIndex();
        int nextIndex;

        if (currentIndex < 0) {
            nextIndex = 0;
        } else {
            nextIndex = currentIndex + 1;
            if (nextIndex >= listView.getItems().size()) {
                nextIndex = 0;
            }
        }

        internalSelectionChange = true;
        listView.getSelectionModel().clearSelection();

        int finalNextIndex = nextIndex;
        Platform.runLater(() -> listView.getSelectionModel().select(finalNextIndex));
    }

    private void playPrevious() {
        if (listView.getItems().isEmpty()) return;

        while (!playHistory.isEmpty()) {
            String previousSong = playHistory.pop();

            if (previousSong != null
                    && !previousSong.isBlank()
                    && !previousSong.equals(currentSong)
                    && listView.getItems().contains(previousSong)) {

                internalSelectionChange = true;
                listView.getSelectionModel().clearSelection();

                Platform.runLater(() -> listView.getSelectionModel().select(previousSong));
                return;
            }
        }

        int currentIndex = listView.getSelectionModel().getSelectedIndex();
        int prevIndex;

        if (currentIndex < 0) {
            prevIndex = 0;
        } else {
            prevIndex = currentIndex - 1;
            if (prevIndex < 0) {
                prevIndex = listView.getItems().size() - 1;
            }
        }

        if (prevIndex == currentIndex && currentIndex >= 0) {
            return;
        }

        String fallbackSong = listView.getItems().get(prevIndex);
        if (fallbackSong.equals(currentSong)) {
            return;
        }

        internalSelectionChange = true;
        listView.getSelectionModel().clearSelection();

        Platform.runLater(() -> listView.getSelectionModel().select(fallbackSong));
    }

    private void playRandomSong() {
        playRandomSong(true);
    }

    private void playRandomSong(boolean saveCurrentToHistory) {
        if (listView.getItems().isEmpty()) return;

        if (saveCurrentToHistory && currentSong != null && !currentSong.isBlank()) {
            if (playHistory.isEmpty() || !currentSong.equals(playHistory.peek())) {
                playHistory.push(currentSong);
            }
        }

        int currentIndex = listView.getSelectionModel().getSelectedIndex();
        int randomIndex;

        if (listView.getItems().size() == 1) {
            randomIndex = 0;
        } else {
            do {
                randomIndex = (int) (Math.random() * listView.getItems().size());
            } while (randomIndex == currentIndex);
        }

        internalSelectionChange = true;
        listView.getSelectionModel().clearSelection();

        int finalRandomIndex = randomIndex;
        Platform.runLater(() -> listView.getSelectionModel().select(finalRandomIndex));
    }

    private void handleTrackEnd(MediaPlayer player) {
        if (btnRepeatMusic.isSelected()) {
            player.seek(Duration.ZERO);
            player.play();
            return;
        }

        if (btnRandomMusic.isSelected()) {
            playRandomSong(true);
            return;
        }

        int nextIndex = listView.getSelectionModel().getSelectedIndex() + 1;
        if (nextIndex >= listView.getItems().size()) {
            nextIndex = 0;
        }

        if (currentSong != null && !currentSong.isBlank()) {
            playHistory.push(currentSong);
        }

        internalSelectionChange = true;
        listView.getSelectionModel().clearSelection();

        int finalNextIndex = nextIndex;
        Platform.runLater(() -> listView.getSelectionModel().select(finalNextIndex));
    }

    private void togglePlay() {
        MediaPlayer player = (MediaPlayer) listView.getUserData();
        if (player != null) {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                updateButtonIcon("/image/pause.png", btnPauseUnpause, 20, 20);
            } else {
                player.play();
                updateButtonIcon("/image/play.png", btnPauseUnpause, 20, 20);
            }
        }
    }

    private void setupSliderVisual(Slider slider, String activeColor, String inactiveColor) {
        Runnable update = () -> {
            double percentage = (slider.getValue() / slider.getMax()) * 100;
            Node track = slider.lookup(".track");
            if (track != null) {
                track.setStyle(String.format(
                        Locale.US,
                        "-fx-background-color: linear-gradient(to right, %s %f%%, %s %f%%);",
                        activeColor, percentage, inactiveColor, percentage
                ));
            }
        };

        slider.valueProperty().addListener((_, _, _) -> update.run());
        Platform.runLater(update);
    }

    private void setupTimelineBehavior() {
        setupSliderVisual(timeLineMusic, "#800080", "#696c6e");

        timeLineMusic.valueChangingProperty().addListener((_, _, isChanging) -> {
            if (!isChanging) {
                MediaPlayer current = (MediaPlayer) listView.getUserData();
                if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                    current.seek(Duration.seconds(timeLineMusic.getValue()));
                }
            }
        });

        timeLineMusic.setOnMousePressed(event -> {
            MediaPlayer current = (MediaPlayer) listView.getUserData();
            if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                double min = timeLineMusic.getMin();
                double max = timeLineMusic.getMax();
                double newValue = min + (max - min) * (event.getX() / timeLineMusic.getWidth());
                newValue = Math.max(min, Math.min(max, newValue));

                timeLineMusic.setValue(newValue);
                current.seek(Duration.seconds(newValue));
            }
        });
    }
}