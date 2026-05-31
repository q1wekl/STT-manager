package app.service;

import app.model.AudioChunk;

import java.util.ArrayList;
import java.util.List;

public class AudioChunkerService {
    private static final int SAMPLE_RATE = 16000;

    public List<AudioChunk> createChunks(float[] audio, int chunkSeconds, int overlapSeconds){
        List<AudioChunk> chunks = new ArrayList<>();
        int chunkSize = chunkSeconds * SAMPLE_RATE;
        int overlapSize = overlapSeconds * SAMPLE_RATE;
        int start = 0;

        while (start < audio.length) {
            int end = Math.min(start + chunkSize, audio.length);
            float[] chunk = new float[end - start];
            System.arraycopy(audio, start, chunk, 0, chunk.length);

            long startMs = start * 1000L / SAMPLE_RATE;
            long endMs = end * 1000L / SAMPLE_RATE;
            long overlapStartMs = startMs + (overlapSeconds * 1000L);
            chunks.add(new AudioChunk(chunk,overlapStartMs, startMs, endMs));
            start += chunkSize - overlapSize;
        }
        return chunks;
    }
}
