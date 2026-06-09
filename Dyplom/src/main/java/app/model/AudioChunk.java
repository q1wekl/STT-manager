package app.model;

public class AudioChunk {

    private final float[] samples;
    private final long startMs;
    private final long endMs;

    public static final AudioChunk poison = new AudioChunk(new float[0],-1,-1);

    public AudioChunk(float[] samples, long startMs, long endMs) {
        this.samples = samples;
;
        this.startMs = startMs;
        this.endMs = endMs;
    }

    public float[] getSamples() {
        return samples;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }
}
