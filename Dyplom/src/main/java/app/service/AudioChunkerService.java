package app.service;

import app.model.AudioChunk;
import app.model.SilenceRegion;

import java.util.ArrayList;
import java.util.List;

public class AudioChunkerService {
    private static final int SAMPLE_RATE = 16000;
    private static final int TARGET_CHUNK_SECONDS = 45;
    private static final int MAX_CHUNK_SECONDS = 60;


    public List<AudioChunk> createChunks(float[] audio, List<SilenceRegion> silences){
        List<AudioChunk> chunks = new ArrayList<>();
        double audioLengthSec = (double) audio.length / SAMPLE_RATE;
        double chunkStartSec = 0;

        while(chunkStartSec < audioLengthSec){
            double targetSec = chunkStartSec + TARGET_CHUNK_SECONDS;
            double maxSec = chunkStartSec + MAX_CHUNK_SECONDS;
            double cutSec = Math.min(maxSec,audioLengthSec);
            for (SilenceRegion silence : silences) {
                double silenceCenter = (silence.getStartSec() + silence.getEndSec()) / 2.0;
                if (silenceCenter >= targetSec && silenceCenter <= maxSec) {
                    cutSec = silenceCenter;
                    break;
                }
            }
            int startSample = (int) (chunkStartSec * SAMPLE_RATE);
            int endSample = (int) (cutSec * SAMPLE_RATE);
            if (endSample <= startSample) {
                chunkStartSec += TARGET_CHUNK_SECONDS;
                continue;
            }
            if (startSample >= audio.length) {
                break;
            }
            if (endSample > audio.length) {
                endSample = audio.length;
            }

            int length = endSample - startSample;

            if (length <= 0) {
                chunkStartSec = cutSec;
                continue;
            }

            float[] chunk = new float[endSample - startSample];
            System.arraycopy(audio,startSample,chunk, 0,chunk.length);
            long startMs = (long) (chunkStartSec * 1000);
            long endMs = (long) (cutSec * 1000);

            System.out.println(
                    "Chunk " + (chunks.size() + 1)
                            + " | start=" + chunkStartSec
                            + "s"
                            + " | end=" + cutSec
                            + "s"
                            + " | len=" + (cutSec - chunkStartSec)
                            + "s"
            );

            chunks.add(new AudioChunk(chunk, startMs,endMs));
            chunkStartSec= cutSec;
        }
        return chunks;
    }

}
