package yourmusic.model;

import java.io.File;

public record TrackEntry(String path, String name) {
    public TrackEntry(String path) {
        this(path, displayName(path));
    }

    public boolean isAvailable() {
        return new File(path).isFile();
    }

    private static String displayName(String path) {
        String fileName = new File(path).getName();
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
    }

    @Override
    public String toString() {
        return name;
    }
}
