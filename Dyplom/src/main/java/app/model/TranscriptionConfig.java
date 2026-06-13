package app.model;

public class TranscriptionConfig {
    private final String accelerationMode;
    private final String language;
    private final String modelId;
    private final boolean speedUp;

    public TranscriptionConfig(String accelerationMode, String language, String modelId, boolean speedUp) {
        this.accelerationMode = accelerationMode;
        this.language = language;
        this.modelId = modelId;
        this.speedUp = speedUp;
    }

    public String getModelId() { return modelId; }
    public String getAccelerationMode() { return accelerationMode; }
    public String getLanguage() { return language; }
    public boolean isSpeedUp() { return speedUp; }
}