# 🎵 Your Music Player

A modern and functional audio player developed using **JavaFX**. The project combines a minimalist design, convenient media library management, and flexible playback settings.

![JavaFX](https://openjfx.io/)
![Java](https://www.java.com/)

## ✨ Features

-   **Playback Control**: Play/Pause, track switching (Next/Previous).
-   **Playback Modes**: 
    -   🔂 **Repeat**: Loop the current track.
    -   🚏 **Shuffle**: Randomly select the next song without repeats.
-   **Interactive Timeline**: Song seeking via a slider with a visual progress display (Gradient Track).
-   **Volume Management**: Intuitive slider with automatic saving of the last volume level.
-   **File Management**: Quick music folder selection and automatic playlist generation.
-   **Error Logging**: Custom `ErrorLogger` system for monitoring application stability.

## 🛠 Tech Stack

-   **Language**: Java 25.0.2
-   **GUI**: JavaFX (FXML + CSS)
-   **Media Engine**: JavaFX Media Player
-   **Logging**: SLF4J
-   **Data Persistence**: Custom settings saving system in `settings.info`

## 🚀 How to Run

1.  **Clone the repository**:
    ```bash
    git clone https://github.com
    ```
2.  **Open the project** in your IDE (IntelliJ IDEA recommended).
3.  **Ensure** that you have the JavaFX SDK installed (or added via Maven/Gradle).
4.  **Run** the `Main` class.

## 🎨 Interface

-   **Dark Theme**: Optimized for comfortable long-term use.
> [!NOTE]
> The interface is still being improved.

## 📂 Project Structure

-   `yourmusic.code`: Application core (Player logic, file handling, logging).
-   `yourmusic`: Controller and UI components.
-   `resources.image`: Graphical assets (icons and design elements).

## 📝 Settings

The application automatically creates a `settings.info` file to store:
-   The last selected volume level.
-   The path to the last opened music folder.

---
*Developed with ❤️ using JavaFX.*
