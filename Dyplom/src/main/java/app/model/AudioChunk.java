package app.model;

public class AudioChunk {
    private final float[] samples;

    private final long overlapStartMs;
    private final long startMs;
    private final long endMs;

    public AudioChunk(float[] samples, long overlapStartMs, long startMs, long endMs) {
        this.samples = samples;
        this.overlapStartMs = overlapStartMs;
        this.startMs = startMs;
        this.endMs = endMs;
    }

    public float[] getSamples() {
        return samples;
    }
    public long getOverlapStartMs() {
        return overlapStartMs;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }
}
