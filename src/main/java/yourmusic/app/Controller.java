package yourmusic.app;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import yourmusic.logger.ErrorLogger;
import yourmusic.model.FolderMusic;
import yourmusic.model.MusicPlayer;
import yourmusic.model.TrackEntry;
import yourmusic.utility.SetupItems;
import yourmusic.utility.LibraryStore;
import yourmusic.utility.AppSettings;
import yourmusic.utility.AppSettingsStore;
import yourmusic.utility.GlobalHotkeyManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Controller {
    private static final Pattern SEARCH_WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private List<String> musicData = new ArrayList<>();
    private List<String> scannedMusicData = new ArrayList<>();
    private final Set<String> favoritePaths = new LinkedHashSet<>();

    private static final double VOLUME_SLIDER_MAX = 100.0;
    private static final double MAX_MEDIA_GAIN = 0.40;
    private boolean cleanupBound = false;
    private boolean internalSelectionChange = false;
    private boolean updatingSearchFilter = false;
    private TrackEntry currentSong = null;

    private final ObservableList<TrackEntry> masterSongs = FXCollections.observableArrayList();
    private ChangeListener<Duration> currentTimeListener;
    private EventHandler<KeyEvent> keyPressedHandler;
    private final Deque<TrackEntry> playHistory = new ArrayDeque<>();

    private final FilteredList<TrackEntry> filteredSongs = new FilteredList<>(masterSongs, _ -> true);
    private MediaPlayer currentPlayer;
    private PauseTransition trackEndDelay;
    private final Random random = new Random();

    @FXML private Slider timeLineMusic;
    @FXML private ProgressBar progressTimeline;
    @FXML public Label labelTimeStart;
    @FXML public Label labelTimeEnd;
    @FXML private ListView<TrackEntry> listView;
    @FXML private StackPane mainStackPane;
    @FXML private Button btnFolderButton;
    @FXML private Button btnCurrentTrack;
    @FXML private Slider volumeMusic;
    @FXML private ProgressBar progressVolume;
    @FXML private Button btnPreviousMusic;
    @FXML private Button btnNextMusic;
    @FXML private ToggleButton btnRepeatMusic, btnPauseUnpause;
    @FXML private ToggleButton btnRandomMusic;
    @FXML private TextField fieldSearch;
    @FXML private SVGPath iconPlayPause;

    private static final String ICON_PLAY = "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z";
    private static final String ICON_PAUSE = "M6 3 h3 a1 1 0 0 1 1 1 v16 a1 1 0 0 1 -1 1 h-3 a1 1 0 0 1 -1 -1 v-16 a1 1 0 0 1 1 -1 z M15 3 h3 a1 1 0 0 1 1 1 v16 a1 1 0 0 1 -1 1 h-3 a1 1 0 0 1 -1 -1 v-16 a1 1 0 0 1 1 -1 z";
    private Scene appScene;
    private Parent settingsPage;
    private boolean favoritesView;
    private LibraryStore.FilterSettings activeFilters =
            new LibraryStore.FilterSettings(false, List.of(), "", "");
    private AppSettings appSettings = AppSettings.defaults();
    private final GlobalHotkeyManager globalHotkeyManager = new GlobalHotkeyManager();

    void setAppScene(Scene appScene) {
        this.appScene = appScene;
    }

    void setAppSettings(AppSettings appSettings) {
        this.appSettings = appSettings;
        volumeMusic.setValue(appSettings.volume());
        globalHotkeyManager.register(appSettings, this::executeGlobalHotkey);
    }

    private void executeGlobalHotkey(AppSettings.HotkeyAction action) {
        if (appScene.getWindow() != null && appScene.getWindow().isFocused()) {
            return;
        }
        executeHotkey(action);
    }

    @FXML
    private void openSettings() {
        if (settingsPage == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/settings.fxml"));
                settingsPage = loader.load();
                SettingsController settingsController = loader.getController();
                settingsController.setCallbacks(
                        () -> appScene.setRoot(mainStackPane),
                        this::reloadMusicLibrary,
                        this::showFavorites,
                        this::updateFolderFilters,
                        this::setAppSettings,
                        appSettings);
            } catch (IOException e) {
                ErrorLogger.logError(218, "Failed to load settings page", e);
                return;
            }
        }

        appScene.setRoot(settingsPage);
    }

    @FXML
    void btnFolder(ActionEvent event) {
        if (favoritesView) {
            favoritesView = false;
            updateFolderButton(false);
            setActivePlaylist(scannedMusicData);
            return;
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFolder = FolderMusic.choiserFile(stage);
        if (selectedFolder == null) {
            return;
        }
        try {
            String folderPath = selectedFolder.toPath().toAbsolutePath().normalize().toString();
            LibraryStore.addMusicFolder(folderPath);
            if (activeFilters.configured() && !activeFilters.folders().contains(folderPath)) {
                List<String> includedFolders = new ArrayList<>(activeFilters.folders());
                includedFolders.add(folderPath);
                activeFilters = new LibraryStore.FilterSettings(
                        true, includedFolders, "", "");
                LibraryStore.saveFilterSettings(activeFilters);
            }
            reloadMusicLibrary();
            boolean foundTracks = scannedMusicData.stream().anyMatch(path ->
                    Path.of(path).startsWith(Path.of(folderPath)));
            if (!foundTracks) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "The folder was added, but no MP3 files were found in it or its subfolders.");
                alert.initOwner(stage);
                alert.setTitle("Music folder added");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
        } catch (IOException e) {
            ErrorLogger.logError(301, "Could not add music folder", e);
            showLibraryError(e);
        }
    }

    private void reloadMusicLibrary() {
        try {
            LinkedHashSet<String> paths = new LinkedHashSet<>();
            for (String folderPath : LibraryStore.getMusicFolders()) {
                File folder = Path.of(folderPath).toFile();
                if (!folder.isDirectory()) {
                    ErrorLogger.log(203, ErrorLogger.Level.WARN, "Music folder is unavailable: " + folderPath);
                    continue;
                }
                for (String path : FolderMusic.getMusicPaths(folder)) {
                    paths.add(Path.of(path).toAbsolutePath().normalize().toString());
                }
            }
            scannedMusicData = new ArrayList<>(paths);
            if (favoritesView) {
                showFavoritesPlaylist();
            } else {
                setActivePlaylist(scannedMusicData);
            }
        } catch (IOException | RuntimeException e) {
            ErrorLogger.logError(302, "Could not read music library", e);
            showLibraryError(e);
        }
    }

    private void showLibraryError(Exception e) {
        new Alert(Alert.AlertType.ERROR, "Could not update the music library:\n" + e.getMessage()).showAndWait();
    }

    private void showFavorites() {
        appScene.setRoot(mainStackPane);
        favoritesView = true;
        updateFolderButton(true);
        showFavoritesPlaylist();
    }

    private void updateFolderButton(boolean showingFavorites) {
        btnFolderButton.setText(showingFavorites ? "Back to library" : "Add music folder");
        btnFolderButton.setTooltip(new Tooltip(showingFavorites
                ? "Return to the music library"
                : "Add a music folder to your library"));
    }

    private void showFavoritesPlaylist() {
        try {
            favoritePaths.clear();
            favoritePaths.addAll(LibraryStore.getFavorites());
            setActivePlaylist(new ArrayList<>(favoritePaths));
        } catch (IOException e) {
            ErrorLogger.logError(303, "Could not load favorites", e);
            showLibraryError(e);
        }
    }

    private void setActivePlaylist(List<String> paths) {
        disposeCurrentPlayer();
        listView.getSelectionModel().clearSelection();
        playHistory.clear();
        currentSong = null;
        internalSelectionChange = false;
        musicData = new ArrayList<>(paths);
        reloadSongsFromMusicData();
    }

    private void toggleFavorite(TrackEntry track) {
        try {
            if (favoritePaths.contains(track.path())) {
                LibraryStore.removeFavorite(track.path());
                favoritePaths.remove(track.path());
            } else {
                LibraryStore.addFavorite(track.path());
                favoritePaths.add(track.path());
            }
            if (favoritesView) {
                showFavoritesPlaylist();
            } else {
                listView.refresh();
            }
        } catch (IOException e) {
            ErrorLogger.logError(304, "Could not update favorites", e);
            showLibraryError(e);
        }
    }

    public void changeVolume() {
        double volume = Math.round(volumeMusic.getValue() * 100.0) / 100.0;
        volumeMusic.setValue(volume);
        try {
            AppSettingsStore.saveVolume(volume);
            appSettings = new AppSettings(
                    appSettings.folderRandom(), appSettings.hotkeys(), volume);
        } catch (IOException e) {
            ErrorLogger.logError(219, "Could not save volume setting", e);
            new Alert(Alert.AlertType.ERROR, "Could not save volume setting:\n" + e.getMessage()).showAndWait();
        }
    }

    @FXML
    void initialize() {
        listView.setItems(filteredSongs);
        listView.setCellFactory(_ -> createTrackCell());

        reInitialize();
        setupTimelineBehavior();
        SetupItems.bindSliderToProgressBar(volumeMusic, progressVolume);
        SetupItems.bindSliderToProgressBar(timeLineMusic, progressTimeline);

        initListenerListView();
        initListenerVolumeMusic();

        btnNextMusic.setOnAction(_ -> playNext());
        btnPreviousMusic.setOnAction(_ -> playPrevious());
        btnPauseUnpause.setOnAction(_ -> togglePlay());
        initListenerFieldSearch();

        Platform.runLater(() -> {
            bindSceneCleanup();

            keyPressedHandler = event -> {
                if (event.getTarget() instanceof TextInputControl) {
                    return;
                }
                if (event.getCode() == KeyCode.SPACE
                        && !event.isControlDown() && !event.isAltDown() && !event.isShiftDown()) {
                    executeHotkey(AppSettings.HotkeyAction.PLAY_PAUSE);
                    event.consume();
                    return;
                }
                for (Map.Entry<AppSettings.HotkeyAction, AppSettings.HotkeyBinding> entry
                        : appSettings.hotkeys().entrySet()) {
                    if (entry.getValue().matches(event)) {
                        executeHotkey(entry.getKey());
                        event.consume();
                        break;
                    }
                }
            };

            mainStackPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
        });
    }

    private void initListenerFieldSearch() {
        fieldSearch.textProperty().addListener((_, _, newValue)
                -> applyListPredicate(newValue));
    }

    private void applyListPredicate(String searchText) {
        updatingSearchFilter = true;
        try {
            String query = searchText == null ? "" : searchText.trim();
            List<TrackEntry> candidates = masterSongs.stream()
                    .filter(this::matchesActiveFilters)
                    .toList();

            Pattern searchPattern = compileSearchPattern(query);
            List<TrackEntry> matches;
            if (searchPattern != null) {
                matches = candidates.stream()
                        .filter(track -> searchPattern.matcher(track.name()).find())
                        .toList();
            } else {
                String normalizedQuery = query.toLowerCase(Locale.ROOT);
                matches = candidates.stream()
                        .filter(track -> normalizedQuery.isEmpty()
                                || track.name().toLowerCase(Locale.ROOT).startsWith(normalizedQuery))
                        .toList();

                if (!normalizedQuery.isEmpty() && matches.isEmpty()) {
                    String[] tokens = SEARCH_WORD_SEPARATOR.split(normalizedQuery);
                    int firstFallbackToken = tokens.length == 1 ? 0 : 1;
                    for (int i = firstFallbackToken; i < tokens.length; i++) {
                        String token = tokens[i];
                        if (token.isBlank()) {
                            continue;
                        }
                        List<TrackEntry> tokenMatches = candidates.stream()
                                .filter(track -> containsWordStartingWith(track.name(), token))
                                .toList();
                        if (!tokenMatches.isEmpty()) {
                            matches = tokenMatches;
                            break;
                        }
                    }
                }
            }
            filteredSongs.setPredicate(matches::contains);

            if (currentSong != null && filteredSongs.contains(currentSong)) {
                listView.getSelectionModel().select(currentSong);
            } else {
                listView.getSelectionModel().clearSelection();
            }
        } finally {
            updatingSearchFilter = false;
        }
        listView.refresh();
    }

    private static Pattern compileSearchPattern(String query) {
        if (query.isEmpty() || query.chars().noneMatch(character -> "\\.^$|?*+()[]{}".indexOf(character) >= 0)) {
            return null;
        }
        try {
            return Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (PatternSyntaxException ignored) {
            return null;
        }
    }

    private boolean matchesActiveFilters(TrackEntry track) {
        return isInSelectedFolders(track);
    }

    private static boolean containsWordStartingWith(String trackName, String queryToken) {
        return SEARCH_WORD_SEPARATOR
                .splitAsStream(trackName.toLowerCase(Locale.ROOT))
                .anyMatch(word -> word.startsWith(queryToken));
    }

    private boolean isInSelectedFolders(TrackEntry track) {
        if (favoritesView || !activeFilters.configured()) {
            return true;
        }
        for (String folder : activeFilters.folders()) {
            if (Path.of(track.path()).toAbsolutePath().normalize()
                    .startsWith(Path.of(folder).toAbsolutePath().normalize())) {
                return true;
            }
        }
        return false;
    }

    private void updateFolderFilters(LibraryStore.FilterSettings filters) {
        activeFilters = filters;
        applyListPredicate(fieldSearch.getText());
    }

    private void executeHotkey(AppSettings.HotkeyAction action) {
        switch (action) {
            case PLAY_PAUSE -> togglePlay();
            case NEXT_TRACK -> playNext();
            case PREVIOUS_TRACK -> playPrevious();
            case SHUFFLE -> btnRandomMusic.setSelected(!btnRandomMusic.isSelected());
            case REPEAT -> btnRepeatMusic.setSelected(!btnRepeatMusic.isSelected());
            case CURRENT_TRACK -> scrollToCurrentTrack();
            case VOLUME_UP -> changeVolumeBy(5);
            case VOLUME_DOWN -> changeVolumeBy(-5);
            case SEEK_FORWARD -> seekBy(5);
            case SEEK_BACKWARD -> seekBy(-5);
        }
    }

    private void changeVolumeBy(double change) {
        volumeMusic.setValue(Math.clamp(volumeMusic.getValue() + change, volumeMusic.getMin(), volumeMusic.getMax()));
        changeVolume();
    }

    private void seekBy(double seconds) {
        MediaPlayer current = currentPlayer;
        if (current == null || current.getStatus() == MediaPlayer.Status.UNKNOWN) {
            return;
        }
        double newValue = Math.clamp(timeLineMusic.getValue() + seconds, timeLineMusic.getMin(), timeLineMusic.getMax());
        timeLineMusic.setValue(newValue);
        current.seek(Duration.seconds(newValue));
    }

    @FXML
    private void scrollToCurrentTrack() {
        if (currentSong == null) {
            return;
        }
        if (!filteredSongs.contains(currentSong) && !fieldSearch.getText().isBlank()) {
            fieldSearch.clear();
        }
        Platform.runLater(() -> {
            int index = filteredSongs.indexOf(currentSong);
            if (index >= 0) {
                listView.getSelectionModel().select(currentSong);
                listView.scrollTo(index);
            }
        });
    }

    private void initListenerVolumeMusic() {
        volumeMusic.valueProperty().addListener((_, _, newVal) -> {
            MediaPlayer current = currentPlayer;
            if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                current.setVolume(toMediaGain(newVal.doubleValue()));
            }
        });
    }

    private static double toMediaGain(double sliderValue) {
        double normalized = Math.clamp(sliderValue / VOLUME_SLIDER_MAX, 0.0, 1.0);
        return MAX_MEDIA_GAIN * Math.pow(normalized, 0.65);
    }

    private void initListenerListView() {
        listView.getSelectionModel().selectedItemProperty().addListener((_, _, selectedSong) -> {
            if (updatingSearchFilter || selectedSong == null || selectedSong.name().isBlank()) {
                return;
            }

            if (currentSong != null && !currentSong.equals(selectedSong) && !internalSelectionChange) {
                playHistory.push(currentSong);
            }

            disposeCurrentPlayer();

            String path = selectedSong.path();
            if (!selectedSong.isAvailable()) {
                ErrorLogger.log(305, ErrorLogger.Level.WARN, "Cannot play missing favorite: " + path);
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
            newPlayer.setVolume(toMediaGain(volumeMusic.getValue()));

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
        btnCurrentTrack.setFocusTraversable(false);
        listView.setFocusTraversable(false);

        btnRepeatMusic.setSelected(false);
        btnRandomMusic.setSelected(false);

        labelTimeStart.setText("00:00");
        labelTimeEnd.setText("00:00");

        playHistory.clear();
        currentSong = null;
        internalSelectionChange = false;

        try {
            favoritePaths.clear();
            favoritePaths.addAll(LibraryStore.getFavorites());
            activeFilters = LibraryStore.getFilterSettings();
            List<String> folders = LibraryStore.getMusicFolders();
            if (folders.isEmpty()) {
                File defaultFolder = Path.of(System.getProperty("user.home"), "Music").toFile();
                if (defaultFolder.isDirectory()) {
                    LibraryStore.addMusicFolder(defaultFolder.getPath());
                }
            }
        } catch (IOException e) {
            ErrorLogger.logError(306, "Could not load music library settings", e);
            showLibraryError(e);
            activeFilters = new LibraryStore.FilterSettings(false, List.of(), "", "");
        }
        reloadMusicLibrary();
    }

    private void reloadSongsFromMusicData() {
        masterSongs.clear();

        for (String path : musicData) {
            if (path == null) {
                continue;
            }

            masterSongs.add(new TrackEntry(path));
        }
        applyListPredicate(fieldSearch.getText());
    }

    private ListCell<TrackEntry> createTrackCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(TrackEntry track, boolean empty) {
                super.updateItem(track, empty);
                if (empty || track == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label name = new Label(track.name());
                name.getStyleClass().add("track-name");
                if (!track.isAvailable()) {
                    name.getStyleClass().add("track-unavailable");
                }

                Button action = new Button();
                action.getStyleClass().add("track-action");
                boolean missingFavorite = !track.isAvailable() && favoritesView;
                if (missingFavorite) {
                    action.setText("×");
                    action.setAccessibleText("Remove missing favorite");
                    action.setOnAction(_ -> removeMissingFavorite(track));
                } else {
                    boolean favorite = favoritePaths.contains(track.path());
                    action.setText(favorite ? "★" : "☆");
                    action.setAccessibleText(favorite ? "Remove from favorites" : "Add to favorites");
                    action.setOnAction(_ -> toggleFavorite(track));
                }
                action.setOnMousePressed(Event::consume);
                action.setFocusTraversable(false);

                HBox row = missingFavorite
                        ? new HBox(8, name, action)
                        : new HBox(8, action, name);
                HBox.setHgrow(name, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                name.setMaxWidth(Double.MAX_VALUE);
                setText(null);
                setGraphic(row);
            }
        };
    }

    private void removeMissingFavorite(TrackEntry track) {
        if (track.isAvailable()) {
            return;
        }
        try {
            LibraryStore.removeFavorite(track.path());
            favoritePaths.remove(track.path());
            showFavoritesPlaylist();
        } catch (IOException e) {
            ErrorLogger.logError(307, "Could not remove missing favorite", e);
            showLibraryError(e);
        }
    }

    private void playNext() {
        if (listView.getItems().isEmpty()) return;

        if (currentSong != null) {
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
            TrackEntry previousSong = playHistory.pop();

            if (previousSong != null
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

        TrackEntry fallbackSong = listView.getItems().get(prevIndex);
        if (fallbackSong.equals(currentSong)) {
            return;
        }

        internalSelectionChange = true;
        listView.getSelectionModel().clearSelection();

        Platform.runLater(() -> listView.getSelectionModel().select(fallbackSong));
    }

    private void playRandomSong() {
        if (listView.getItems().isEmpty()) return;

        if (currentSong != null) {
            if (playHistory.isEmpty() || !currentSong.equals(playHistory.peek())) {
                playHistory.push(currentSong);
            }
        }

        int currentIndex = listView.getSelectionModel().getSelectedIndex();
        Path currentDirectory = currentSong == null ? null : Path.of(currentSong.path()).getParent();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < listView.getItems().size(); i++) {
            TrackEntry candidate = listView.getItems().get(i);
            if (!candidate.isAvailable() || i == currentIndex) {
                continue;
            }
            if (appSettings.folderRandom() && currentDirectory != null
                    && !Objects.equals(currentDirectory, Path.of(candidate.path()).getParent())) {
                continue;
            }
            candidates.add(i);
        }
        if (candidates.isEmpty() && currentIndex >= 0) {
            for (int i = 0; i < listView.getItems().size(); i++) {
                if (i != currentIndex && listView.getItems().get(i).isAvailable()) {
                    candidates.add(i);
                }
            }
        }
        if (candidates.isEmpty()) {
            if (currentIndex >= 0 || !listView.getItems().getFirst().isAvailable()) {
                return;
            }
            candidates.add(0);
        }
        int randomIndex = candidates.get(random.nextInt(candidates.size()));

        internalSelectionChange = true;
        listView.getSelectionModel().clearSelection();

        Platform.runLater(() -> listView.getSelectionModel().select(randomIndex));
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

        if (currentSong != null) {
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
            globalHotkeyManager.close();
            disposeCurrentPlayer();
        });
    }

    @FXML
    private void reloadMusic() {
        reloadMusicLibrary();
    }
}
