package app.model;

public class ChunkingConfig {
    public final int targetSeconds;
    public final int maxSeconds;

    public ChunkingConfig(int targetSeconds, int maxSeconds) {
        this.targetSeconds = targetSeconds;
        this.maxSeconds = maxSeconds;
    }
}