package app.model;

import java.io.File;

public class MediaFile {
    private final File file;
    private final String format;

    public MediaFile(File file, String format) {
        this.file = file;
        this.format = format;
    }

    public File getFile() {
        return file;
    }

    public String getFormat() {
        return format;
    }
}
