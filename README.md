# 🎵 Your Music Player

A desktop music player built with Java 25 and JavaFX.

<p align="left">
  <a href="https://www.oracle.com/java/technologies/downloads/"><img src="https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25" /></a>
  <a href="https://gluonhq.com/products/javafx/"><img src="https://img.shields.io/badge/JavaFX-25-blue?style=flat-square" alt="JavaFX" /></a>
  <a href="https://maven.apache.org/download.cgi"><img src="https://img.shields.io/badge/Maven-3.8+-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven" /></a>
</p>

## ✨ Features

- Play, pause, skip, repeat, and shuffle tracks.
- Scan MP3 files in selected folders and their subfolders.
- Search by the start of a track name, with a fallback search through
  later words when there are no direct matches.
- Save favorite tracks and remove unavailable favorites from the list.
- Filter the library by folders, a keyword, or a regular expression.
- Adjust playback volume and keep the last selected level.
- Configure keyboard shortcuts. Global shortcuts are supported on Windows.
- Manage folders, filters, playback mode, and shortcuts in the in-app
  settings page.

## Run

The project uses Maven and requires JDK 25.

```bash
mvn javafx:run
```

Alternatively, open the project in an IDE and run
`yourmusic.app.YourMusic`.

## Data and settings

The application stores `settings.xml` and `library.properties` in its
user data directory:

- Windows: `%APPDATA%/your-music`
- macOS: `~/Library/Application Support/your-music`
- Linux: `~/.local/share/your-music`

Settings include volume, playback mode, and keyboard shortcuts.
The library file stores selected folders, favorites, and filters.

## Technologies

- Java 25
- JavaFX 25
- Maven
- JNA for Windows global shortcuts
