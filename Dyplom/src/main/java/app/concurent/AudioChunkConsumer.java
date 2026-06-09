package app.concurent;

import app.model.AudioChunk;
import app.model.TranscriptResult;
import app.model.TranscriptionConfig;
import app.service.whisper.SpeechToTextEngine;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class AudioChunkConsumer implements Runnable {

    private final SpeechToTextEngine engine;
    private final BlockingQueue<AudioChunk> chunkQueue;
    private final BlockingQueue<TranscriptResult> resultQueue;
    private final TranscriptionConfig config;
    private final Consumer<String> onStatus;

    public AudioChunkConsumer(BlockingQueue<AudioChunk> chunkQueue, BlockingQueue<TranscriptResult> resultQueue,
            SpeechToTextEngine engine, TranscriptionConfig config, Consumer<String> onStatus) {
        this.chunkQueue = chunkQueue;
        this.resultQueue = resultQueue;
        this.engine = engine;
        this.config = config;
        this.onStatus = onStatus;
    }

    @Override
    public void run() {
        try {
            while (true) {
                AudioChunk chunk = chunkQueue.take();
                if (chunk == AudioChunk.poison) {
                    break;
                }
                if (chunk.getSamples() == null || chunk.getSamples().length == 0) {
                    continue;
                }

                TranscriptResult result = engine.transcribe(chunk.getSamples(), config);
                onStatus.accept("Чанк оброблено потоком " + Thread.currentThread().getName());
                resultQueue.put(result);
            }

        } catch (Exception e) {
            throw new RuntimeException("Consumer failed", e);
        }
    }
}