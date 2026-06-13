package app.model;

public class AudioChunk {

    private final float[] samples;
    private final long startMs;
    private final long endMs;
    private final int index;

    public static final AudioChunk poison = new AudioChunk(new float[0],-1,-1,-1);

    public AudioChunk(float[] samples, long startMs, long endMs, int index) {
        this.samples = samples;
        this.startMs = startMs;
        this.endMs = endMs;
        this.index = index;
    }
    public int getIndex() { return index; }
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
