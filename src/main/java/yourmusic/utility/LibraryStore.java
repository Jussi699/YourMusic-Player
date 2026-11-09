package yourmusic.utility;

import yourmusic.logger.ErrorLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class LibraryStore {
    private static final String FOLDERS_COUNT = "folders.count";
    private static final String FAVORITES_COUNT = "favorites.count";
    private static final String FILTER_FOLDERS_COUNT = "filter.folders.count";

    private LibraryStore() {
    }

    public static synchronized List<String> getMusicFolders() throws IOException {
        return readList("folder.", FOLDERS_COUNT);
    }

    public static synchronized List<String> getFavorites() throws IOException {
        return readList("favorite.", FAVORITES_COUNT);
    }

    public static synchronized FilterSettings getFilterSettings() throws IOException {
        Properties properties = load();
        boolean configured = Boolean.parseBoolean(properties.getProperty("filter.configured", "false"));
        List<String> folders = readList(properties, "filter.folder.", FILTER_FOLDERS_COUNT);
        return new FilterSettings(
                configured,
                folders,
                properties.getProperty("filter.regex", ""),
                properties.getProperty("filter.keyword", ""));
    }

    public static synchronized void saveFilterSettings(FilterSettings settings) throws IOException {
        Properties properties = load();
        Set<String> obsoleteKeys = new LinkedHashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("filter.folder.")) {
                obsoleteKeys.add(key);
            }
        }
        obsoleteKeys.forEach(properties::remove);
        properties.setProperty("filter.configured", "true");
        properties.setProperty(FILTER_FOLDERS_COUNT, String.valueOf(settings.folders().size()));
        for (int i = 0; i < settings.folders().size(); i++) {
            properties.setProperty("filter.folder." + i, settings.folders().get(i));
        }
        properties.setProperty("filter.regex", settings.regex());
        properties.setProperty("filter.keyword", settings.keyword());
        write(properties);
    }

    public static synchronized void addMusicFolder(String folder) throws IOException {
        List<String> folders = new ArrayList<>(getMusicFolders());
        String normalized = normalize(folder);
        if (!folders.contains(normalized)) {
            folders.add(normalized);
            writeList("folder.", FOLDERS_COUNT, folders);
        }
    }

    public static synchronized void removeMusicFolder(String folder) throws IOException {
        List<String> folders = new ArrayList<>(getMusicFolders());
        if (folders.remove(normalize(folder))) {
            writeList("folder.", FOLDERS_COUNT, folders);
        }
    }

    public static synchronized void addFavorite(String path) throws IOException {
        List<String> favorites = new ArrayList<>(getFavorites());
        String normalized = normalize(path);
        if (!favorites.contains(normalized)) {
            favorites.add(normalized);
            writeList("favorite.", FAVORITES_COUNT, favorites);
        }
    }

    public static synchronized void removeFavorite(String path) throws IOException {
        List<String> favorites = new ArrayList<>(getFavorites());
        if (favorites.remove(normalize(path))) {
            writeList("favorite.", FAVORITES_COUNT, favorites);
        }
    }

    private static List<String> readList(String keyPrefix, String countKey) throws IOException {
        return readList(load(), keyPrefix, countKey);
    }

    private static List<String> readList(Properties properties, String keyPrefix, String countKey) throws IOException {
        int count;
        try {
            count = Integer.parseInt(properties.getProperty(countKey, "0"));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid library data: " + countKey, e);
        }
        if (count < 0) {
            throw new IOException("Invalid library data: negative " + countKey);
        }

        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String value = properties.getProperty(keyPrefix + i);
            if (value == null || value.isBlank()) {
                ErrorLogger.log(301, ErrorLogger.Level.WARN, "Ignoring empty library entry " + keyPrefix + i);
                continue;
            }
            values.add(value);
        }
        return values;
    }

    private static void writeList(String keyPrefix, String countKey, List<String> values) throws IOException {
        Properties properties = load();
        Set<String> obsoleteKeys = new LinkedHashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(keyPrefix)) {
                obsoleteKeys.add(key);
            }
        }
        obsoleteKeys.forEach(properties::remove);
        properties.setProperty(countKey, String.valueOf(values.size()));
        for (int i = 0; i < values.size(); i++) {
            properties.setProperty(keyPrefix + i, values.get(i));
        }

        write(properties);
    }

    private static void write(Properties properties) throws IOException {
        Path file = dataFile();
        Files.createDirectories(file.getParent());
        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "Your Music library");
        }
    }

    private static Properties load() throws IOException {
        Properties properties = new Properties();
        Path file = dataFile();
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            }
        }
        return properties;
    }

    private static String normalize(String value) {
        return Path.of(value).toAbsolutePath().normalize().toString();
    }

    public static Path dataDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("mac") || os.contains("darwin")) {
            return Path.of(home, "Library", "Application Support", "your-music");
        }
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path roaming = appData == null || appData.isBlank()
                    ? Path.of(home, "AppData", "Roaming")
                    : Path.of(appData);
            return roaming.resolve("your-music");
        }
        return Path.of(home, ".local", "share", "your-music");
    }

    private static Path dataFile() {
        return dataDirectory().resolve("library.properties");
    }

    public record FilterSettings(boolean configured, List<String> folders, String regex, String keyword) {
        public FilterSettings {
            folders = List.copyOf(folders);
            regex = regex == null ? "" : regex;
            keyword = keyword == null ? "" : keyword;
        }
    }
}
