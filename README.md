# 🎵 Your Music Player

A modern and functional audio player developed using **JavaFX**. The project combines a minimalist design, convenient media library management, and flexible playback settings.

[JavaFX](https://openjfx.io/)
[Java](https://www.java.com/)

## ✨ Features

-   **Playback Control**: Play/Pause, track switching (Next/Previous).
-   **Playback Modes**:
    -   🔂 **Repeat**: Loop the current track.
    -   🚏 **Shuffle**: Randomly select the next song without repeats.
-   **Interactive Timeline**: Song seeking via a slider with a visual progress display (Gradient Track).
-   **Volume Management**: Intuitive slider with automatic saving of the last volume level.
-   **File Management**: Quick music folder selection and automatic playlist generation.

## 🛠 Tech Stack

-   **Language**: Java 25
-   **GUI**: JavaFX (FXML + CSS)

## 🚀 How to Run

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/Jussi699/YourMusic-Player.git
    ```
2.  **Open the project** in your IDE (IntelliJ IDEA recommended).
3.  **Ensure** that you have the JavaFX SDK installed (or added via Maven/Gradle).
4.  **Run** the `Main` class.  
    OR
1. Run YourMusic.exe

## 📂 Project Structure

-   `yourmusic.app`: Application core (Player logic, file handling, controller).
-   `yourmusic.logger`: Logging.
-   `resources.image`: Graphical assets (icons and design elements).
-   `resources`: FXML / CSS.

## 📝 Settings

The application automatically creates a `settings.info` file to store:
-   The last selected volume level.

---
*Developed with ❤️ using JavaFX.*
