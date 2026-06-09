package app.model;

public class TranscriptionConfig {
    private final String modelPath;
    private final int threads;
    private final String language;
    private final boolean speedUp;
    private String accelerationMode;

    public TranscriptionConfig(String accelerationMode, String language, int threads, String modelPath, boolean speedUp) {
        this.accelerationMode = accelerationMode;
        this.language = language;
        this.threads = threads;
        this.modelPath = modelPath;
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

    public String getAccelerationMode() {return accelerationMode;}
}
