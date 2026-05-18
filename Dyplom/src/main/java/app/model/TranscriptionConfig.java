package app.model;

public class TranscriptionConfig {
    private final String modelPath;
    private final int threads;
    private final String language;
    private final boolean speedUp;

    public TranscriptionConfig(String modelPath, int threads, String language, boolean speedUp) {
        this.modelPath = modelPath;
        this.threads = threads;
        this.language = language;
        this.speedUp = speedUp;
    }

    public String getModelPath() {
        return modelPath;
    }

    public int getThreads() {
        return threads;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isSpeedUp() {
        return speedUp;
    }
}
