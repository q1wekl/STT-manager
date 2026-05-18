package app.service;

import java.nio.FloatBuffer;

public class AudioFeatureExtractor {
    public static FloatBuffer prepareAudioBuffer(float[] audio) {

        FloatBuffer buffer = FloatBuffer.allocate(audio.length);
        buffer.put(audio);
        buffer.flip();

        return buffer;
    }
}
