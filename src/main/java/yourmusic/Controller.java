package yourmusic;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import yourmusic.code.*;

public class Controller {
    private HashMap<Integer, String> musicData = new HashMap<>();
    public final static double volume = 400.0;

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
    private Pane centerPane;

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
    private Pane rightPane;

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
    void btnClickFolder(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File folder = FolderMusic.choiserFile(stage);

        if (folder.exists() && folder.isDirectory()) {
            MediaPlayer currentPlayer = (MediaPlayer) listView.getUserData();

            if (currentPlayer != null) {
                currentPlayer.stop();
                currentPlayer.dispose();
                listView.setUserData(null);
            }

            listView.getSelectionModel().clearSelection();
            musicData.clear();

            musicData = FolderMusic.getIndexAndPathMusic(folder);
            ListViewController.setItem(folder, musicData, listView);
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
        assert centerPane != null : "fx:id=\"centerPane\"";
        assert btnFolder != null : "fx:id=\"btnFolder\"";
        assert imageMusic != null : "fx:id=\"imageMusic\"";
        assert listView != null : "fx:id=\"listView\"";
        assert mainAnchorPane != null : "fx:id=\"mainAnchorPane\"";
        assert mainPane != null : "fx:id=\"mainPane\"";
        assert rightPane != null : "fx:id=\"rightPane\" ";
        assert volumeMusic != null : "fx:id=\"volumeMusic\"";
        assert timeLineMusic != null : "fx:id=\"timeLineMusic\"";
        assert labelTimeStart != null : "fx:id=\"labelTimeStart\"";
        assert labelTimeEnd != null : "fx:id=\"labelTimeEnd\"";
        assert btnNextMusic != null : "fx:id=\"btnNextMusic\"";
        assert btnPreviousMusic != null : "fx:id=\"btnPreviousMusic\"";
        assert btnRepeatMusic != null : "fx:id=\"btnRepeatMusic\"";
        assert btnRandomMusic != null : "fx:id=\"btnRandomMusic\"";

        reInitialize();
        setupTimelineBehavior();
        setupSliderVisual(volumeMusic, "#800080", "#696c6e");

        volumeMusic.valueProperty().addListener((observableValue, oldVal, newVal) -> {
            MediaPlayer current = (MediaPlayer) listView.getUserData();
            if (current != null && current.getStatus() != MediaPlayer.Status.UNKNOWN) {
                current.setVolume(newVal.doubleValue() / volume);
            }
        });

        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx.intValue() < 0) return;

            MediaPlayer oldPlayer = (MediaPlayer) listView.getUserData();
            if (oldPlayer != null) {
                oldPlayer.stop();
                oldPlayer.dispose();
            }

            String path = musicData.get(newIdx.intValue());
            MediaPlayer newPlayer = MusicPlayer.createPlayer(path);

            listView.setUserData(newPlayer);
            newPlayer.setVolume(volumeMusic.getValue() / volume);

            updateButtonIcon("/image/play.png", btnPauseUnpause, 20, 20);

            newPlayer.setOnReady(() -> {
                timeLineMusic.setMax(newPlayer.getTotalDuration().toSeconds());
                labelTimeEnd.setText(MusicPlayer.formatTimeForEndLabel(newPlayer.getTotalDuration()));
            });

            newPlayer.currentTimeProperty().addListener((o, oldTime, newTime) -> {
                if (!timeLineMusic.isValueChanging()) {
                    timeLineMusic.setValue(newTime.toSeconds());
                }
                labelTimeStart.setText(MusicPlayer.formatTimeForStartLabel(newTime, newPlayer.getTotalDuration()));
            });

            newPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    PauseTransition delay = new PauseTransition(Duration.seconds(1));

                    delay.setOnFinished(event -> {
                        if (btnRepeatMusic.isSelected()) {
                            newPlayer.seek(Duration.ZERO);
                            newPlayer.play();
                        }
                        else if (btnRandomMusic.isSelected() && musicData.size() > 1) {
                            int randomIndex;
                            do {
                                randomIndex = (int) (Math.random() * musicData.size());
                            } while (randomIndex == listView.getSelectionModel().getSelectedIndex());
                            listView.getSelectionModel().select(randomIndex); // -------------------------- //
                        }
                        else {
                            playNext();
                        }
                    });

                    delay.play();
                });
            });
            newPlayer.play();
        });

        btnNextMusic.setOnAction(e -> playNext());
        btnPreviousMusic.setOnAction(e -> playPrevious());
        btnPauseUnpause.setOnAction(e -> togglePlay());

        btnRepeatMusic.setOnMouseClicked((event -> {
               if (btnRepeatMusic.isSelected()) {
                   updateButtonIcon("/image/repeatOn.png", btnRepeatMusic, 15, 20);
               } else {
                   updateButtonIcon("/image/repeatOff.png", btnRepeatMusic, 15, 20);
               }
        }));

       btnRandomMusic.setOnMouseClicked((event -> {
           if (btnRandomMusic.isSelected()) {
               updateButtonIcon("/image/randomOn.png", btnRandomMusic, 15, 20);
           } else {
               updateButtonIcon("/image/randomOff.png", btnRandomMusic, 15, 20);
           }
       }));

        Platform.runLater(() -> {
            mainAnchorPane.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                MediaPlayer current = (MediaPlayer) listView.getUserData();
                if (current == null || current.getStatus() == MediaPlayer.Status.UNKNOWN) return;

                double step = 5.0;
                double newValue = timeLineMusic.getValue();

                switch (event.getCode()) {
                    case LEFT  -> newValue = Math.max(0, newValue - step);
                    case RIGHT -> newValue = Math.min(timeLineMusic.getMax(), newValue + step);
                    default    -> { return; }
                }

                timeLineMusic.setValue(newValue);
                current.seek(Duration.seconds(newValue));
                event.consume();
            });
        });
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

        String savedVolume = Info.get("volume");
        if (savedVolume != null && !savedVolume.isEmpty()) {
            volumeMusic.setValue(Double.parseDouble(savedVolume));
        } else {
            volumeMusic.setValue(10.0);
            Info.save("volume", "10.0");
        }

        String userHome = System.getProperty("user.home");
        File folder = new File(userHome + File.separator + "Music");

        if (folder != null) {
            musicData = FolderMusic.getIndexAndPathMusic(folder);
            ListViewController.setItem(folder, musicData, listView);
        } else {
            ErrorLogger.log(205, ErrorLogger.Level.WARN, " In: Class: " + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName());
        }

        try {
            imageMusic.setImage(new Image(getClass().getResourceAsStream("/image/mainImage.png")));
            Rectangle clip = new Rectangle(imageMusic.getFitWidth(), imageMusic.getFitHeight());
            clip.setArcWidth(180);
            clip.setArcHeight(180);
            imageMusic.setClip(clip);
        } catch (NullPointerException e){
            ErrorLogger.log(206, ErrorLogger.Level.WARN, " In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName() +
                    " | Exception: " + e.getMessage());
        }
        catch (Exception e){
            ErrorLogger.log(213, ErrorLogger.Level.WARN, " In: Class" + Controller.class.getName() + " Method: " + ErrorLogger.getCurrentMethodName() +
                    " | Exception: " + e.getMessage());
        }
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
        int nextIndex = listView.getSelectionModel().getSelectedIndex() + 1;
        if (nextIndex >= musicData.size()) nextIndex = 0;
        listView.getSelectionModel().select(nextIndex);
    }

    private void playPrevious() {
        int prevIndex = listView.getSelectionModel().getSelectedIndex() - 1;
        if (prevIndex < 0) prevIndex = musicData.size() - 1;
        listView.getSelectionModel().select(prevIndex);
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
                track.setStyle(String.format(Locale.US,
                        "-fx-background-color: linear-gradient(to right, %s %f%%, %s %f%%);",
                        activeColor, percentage, inactiveColor, percentage));
            }
        };

        slider.valueProperty().addListener((o, old, newVal) -> update.run());
        Platform.runLater(update);
    }

    private void setupTimelineBehavior() {
        setupSliderVisual(timeLineMusic, "#800080", "#696c6e");

        timeLineMusic.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
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