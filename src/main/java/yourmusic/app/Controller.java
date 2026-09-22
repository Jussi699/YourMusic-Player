package yourmusic.app;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import yourmusic.logger.ErrorLogger;
import yourmusic.model.FolderMusic;
import yourmusic.model.MusicPlayer;
import yourmusic.utility.Info;
import yourmusic.utility.Util;
import yourmusic.utility.SetupItems;

import java.io.File;
import java.util.*;

public class Controller {
    private List<String> musicData = new ArrayList<>();

    public static final double VOLUME = 400.0;
    private boolean cleanupBound = false;
    private boolean internalSelectionChange = false;
    private String currentSong = null;

    private final ObservableList<String> masterSongs = FXCollections.observableArrayList();
    private ChangeListener<Duration> currentTimeListener;
    private EventHandler<KeyEvent> keyPressedHandler;
    private final Deque<String> playHistory = new ArrayDeque<>();

    private final FilteredList<String> filteredSongs = new FilteredList<>(masterSongs, _ -> true);
    private MediaPlayer currentPlayer;
    private PauseTransition trackEndDelay;
    private final Random random = new Random();

    @FXML private Slider timeLineMusic;
    @FXML public Label labelTimeStart;
    @FXML public Label labelTimeEnd;
    @FXML private ListView<String> listView;
    @FXML private StackPane mainStackPane;
    @FXML private Slider volumeMusic;
    @FXML private Button btnPreviousMusic;
    @FXML private Button btnNextMusic;
    @FXML private ToggleButton btnRepeatMusic, btnPauseUnpause;
    @FXML private ToggleButton btnRandomMusic;
    @FXML private TextField fieldSearch;
    @FXML private SVGPath iconPlayPause;

    private static final String ICON_PLAY = "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z";
    private static final String ICON_PAUSE = "M6 3 h3 a1 1 0 0 1 1 1 v16 a1 1 0 0 1 -1 1 h-3 a1 1 0 0 1 -1 -1 v-16 a1 1 0 0 1 1 -1 z M15 3 h3 a1 1 0 0 1 1 1 v16 a1 1 0 0 1 -1 1 h-3 a1 1 0 0 1 -1 -1 v-16 a1 1 0 0 1 1 -1 z";
    private final String KEY = "volume";
    private File currentFolder;

    @FXML
    void btnFolder(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentFolder = FolderMusic.choiserFile(stage);

        updateMusic(currentFolder);
    }

    private void updateMusic(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            disposeCurrentPlayer();

            listView.getSelectionModel().clearSelection();
            musicData.clear();
            playHistory.clear();
            currentSong = null;
            internalSelectionChange = false;

            musicData = FolderMusic.getMusicPaths(folder);
            reloadSongsFromMusicData();
        } else {
            ErrorLogger.log(203, ErrorLogger.Level.WARN, "Failed reload song from music data!");
        }
    }

    public void changeVolume() {
        Info.save(KEY, String.valueOf(volumeMusic.getValue()));
    }

    @FXML
    void initialize() {
        listView.setItems(filteredSongs);

        reInitialize();
        setupTimelineBehavior();
        SetupItems.setupSliderVisual(volumeMusic);

        initListenerListView();
        initListenerVolumeMusic();

        btnNextMusic.setOnAction(_ -> playNext());
        btnPreviousMusic.setOnAction(_ -> playPrevious());
        btnPauseUnpause.setOnAction(_ -> togglePlay());

        initListenerFieldSearch();

        Platform.runLater(() -> {
            bindSceneCleanup();

            keyPressedHandler = event -> {
                MediaPlayer current = currentPlayer;

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
                    case SPACE -> {
                        if (!fieldSearch.isFocused()) {
                            togglePlay();
                            event.consume();
                        }
                    }
                    default -> {}
                }
            };

            mainStackPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
        });
    }

    private void initListenerFieldSearch() {
        fieldSearch.textProperty().addListener((_, _, newValue) ->
                filteredSongs.setPredicate(song -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }

                    String query = newValue.toLowerCase(Locale.ROOT);
                    return song.toLowerCase(Locale.ROOT).startsWith(query);
                })
        );
    }

    private void initListenerVolumeMusic() {
        volumeMusic.valueProperty().addListener((_, _, newVal) -> {
            MediaPlayer current = currentPlayer;
            if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                current.setVolume(newVal.doubleValue() / VOLUME);
            }
        });

    }

    private void initListenerListView() {
        listView.getSelectionModel().selectedItemProperty().addListener((_, _, selectedSong) -> {
            if (selectedSong == null || selectedSong.isBlank()) {
                return;
            }

            if (currentSong != null && !currentSong.equals(selectedSong) && !internalSelectionChange) {
                playHistory.push(currentSong);
            }

            disposeCurrentPlayer();

            String path = Util.findPathByDisplayedName(selectedSong, musicData);
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

            currentPlayer = newPlayer;
            newPlayer.setVolume(volumeMusic.getValue() / VOLUME);

            iconPlayPause.setContent(ICON_PAUSE);

            newPlayer.setOnReady(() -> {
                timeLineMusic.setMax(newPlayer.getTotalDuration().toSeconds());
                labelTimeEnd.setText(MusicPlayer.formatTimeForEndLabel(newPlayer.getTotalDuration()));
                newPlayer.play();
            });

            currentTimeListener = (_, _, newTime) -> {
                if (!timeLineMusic.isValueChanging()) {
                    timeLineMusic.setValue(newTime.toSeconds());
                }
                labelTimeStart.setText(
                        MusicPlayer.formatTimeForStartLabel(newTime, newPlayer.getTotalDuration())
                );
            };
            newPlayer.currentTimeProperty().addListener(currentTimeListener);

            newPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                if (trackEndDelay != null) {
                    trackEndDelay.stop();
                }

                trackEndDelay = new PauseTransition(Duration.seconds(1));
                trackEndDelay.setOnFinished(_ -> handleTrackEnd(newPlayer));
                trackEndDelay.play();
            }));

            newPlayer.setOnError(() -> ErrorLogger.log(217, ErrorLogger.Level.WARN, " MediaPlayer error: " + newPlayer.getError()));
        });
    }

    private void reInitialize() {
        btnPauseUnpause.setFocusTraversable(false);
        btnNextMusic.setFocusTraversable(false);
        btnPreviousMusic.setFocusTraversable(false);
        btnRepeatMusic.setFocusTraversable(false);
        btnRandomMusic.setFocusTraversable(false);
        listView.setFocusTraversable(false);

        btnRepeatMusic.setSelected(false);
        btnRandomMusic.setSelected(false);

        labelTimeStart.setText("00:00");
        labelTimeEnd.setText("00:00");

        playHistory.clear();
        currentSong = null;
        internalSelectionChange = false;

        String savedVolume = Info.get(KEY);
        try {
            if(savedVolume == null || savedVolume.isBlank()) {
                throw new NumberFormatException("Volume is missing");
            }
            volumeMusic.setValue(Double.parseDouble(savedVolume));
        } catch (NumberFormatException _) {
            volumeMusic.setValue(10.0);
            Info.save(KEY, "10.0");
        }

        String userHome = System.getProperty("user.home");
        File folder = new File(userHome + File.separator + "Music");

        if (folder.exists() && folder.isDirectory()) {
            musicData = FolderMusic.getMusicPaths(folder);
            currentFolder = folder;
            reloadSongsFromMusicData();
        } else {
            ErrorLogger.log(205, ErrorLogger.Level.WARN, "Failed reload song from music data!");
        }
    }

    private void reloadSongsFromMusicData() {
        masterSongs.clear();

        for (String path : musicData) {
            if (path == null) {
                continue;
            }

            File file = new File(path);
            String fileName = file.getName();

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = fileName.substring(0, dotIndex);
            }

            masterSongs.add(fileName);
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

        if (prevIndex == currentIndex) {
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
        if (listView.getItems().isEmpty()) return;

        if (currentSong != null && !currentSong.isBlank()) {
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
                randomIndex = random.nextInt(listView.getItems().size());
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
            playRandomSong();
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
        MediaPlayer player = currentPlayer;
        if (player != null) {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                currentPlayer.pause();
                iconPlayPause.setContent(ICON_PLAY);
            } else {
                currentPlayer.play();
                iconPlayPause.setContent(ICON_PAUSE);
            }
        }
    }

    private void setupTimelineBehavior() {
        SetupItems.setupSliderVisual(timeLineMusic);

        timeLineMusic.valueChangingProperty().addListener((_, _, isChanging) -> {
            if (Boolean.FALSE.equals(isChanging)) {
                MediaPlayer current = currentPlayer;
                if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                    current.seek(Duration.seconds(timeLineMusic.getValue()));
                }
            }
        });

        timeLineMusic.setOnMousePressed(event -> {
            MediaPlayer current = currentPlayer;
            if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                double min = timeLineMusic.getMin();
                double max = timeLineMusic.getMax();
                double newValue = min + (max - min) * (event.getX() / timeLineMusic.getWidth());
                newValue = Math.clamp(newValue, min, max);

                timeLineMusic.setValue(newValue);
                current.seek(Duration.seconds(newValue));
            }
        });
    }

    private void disposeCurrentPlayer() {
        if (trackEndDelay != null) {
            trackEndDelay.stop();
            trackEndDelay.setOnFinished(null);
            trackEndDelay = null;
        }

        if (currentPlayer == null) {
            return;
        }

        if (currentTimeListener != null) {
            currentPlayer.currentTimeProperty().removeListener(currentTimeListener);
            currentTimeListener = null;
        }

        currentPlayer.setOnReady(null);
        currentPlayer.setOnEndOfMedia(null);
        currentPlayer.setOnError(null);
        currentPlayer.stop();
        currentPlayer.dispose();
        currentPlayer = null;
        timeLineMusic.setValue(0);
        labelTimeStart.setText("00:00");
        labelTimeEnd.setText("00:00");
    }

    private void bindSceneCleanup() {
        if (cleanupBound || mainStackPane.getScene() == null || mainStackPane.getScene().getWindow() == null) {
            return;
        }

        cleanupBound = true;
        Stage stage = (Stage) mainStackPane.getScene().getWindow();
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, _ -> {
            if (keyPressedHandler != null && mainStackPane.getScene() != null) {
                mainStackPane.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
                keyPressedHandler = null;
            }
            disposeCurrentPlayer();
        });
    }

    @FXML
    private void reloadMusic() {
        if (currentFolder != null) {
            updateMusic(currentFolder);
            reloadSongsFromMusicData();
        }
    }
}
