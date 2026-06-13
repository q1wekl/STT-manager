package app.concurent;

import app.model.*;
import app.service.whisper.SpeechToTextEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class AudioChunkConsumer implements Runnable {

    private final SpeechToTextEngine engine;
    private final BlockingQueue<AudioChunk> chunkQueue;
    private final BlockingQueue<TranscriptResult> resultQueue;
    private final TranscriptionConfig config;

    public AudioChunkConsumer(BlockingQueue<AudioChunk> chunkQueue, BlockingQueue<TranscriptResult> resultQueue,
                              SpeechToTextEngine engine, TranscriptionConfig config) {
        this.chunkQueue = chunkQueue;
        this.resultQueue = resultQueue;
        this.engine = engine;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            while (true) {
                AudioChunk chunk = chunkQueue.take();
                if (chunk == AudioChunk.poison) break;
                if (chunk.getSamples() == null || chunk.getSamples().length == 0) continue;

                TranscriptResult result = engine.transcribe(chunk.getSamples(), config);

                long offset = chunk.getStartMs();
                List<TranscriptSegment> shifted = new ArrayList<>();
                for (TranscriptSegment s : result.getSegments()) {
                    shifted.add(new TranscriptSegment(s.getText(), s.getStartTime() + offset, s.getEndTime() + offset));
                }

                resultQueue.put(new TranscriptResult(shifted, result.getFullText(), chunk.getIndex()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Consumer failed", e);
        }
    }
}