package app.concurent;

import app.model.AudioChunk;
import app.model.TranscriptResult;
import app.model.TranscriptSegment;
import app.model.TranscriptionConfig;
import app.service.whisper.SpeechToTextEngine;
import app.service.whisper.WhisperCppEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ResultManager {

    private final Consumer<String> onStatus;

    private final BlockingQueue<AudioChunk> chunkQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<TranscriptResult> resultQueue = new LinkedBlockingQueue<>();

    private final SpeechToTextEngine engine;
    private final TranscriptionConfig config;

    private final int consumerCount = 2;
    private final ExecutorService pool = Executors.newFixedThreadPool(consumerCount);

    private int totalChunks;
    private int completedChunks = 0;

    public ResultManager(SpeechToTextEngine engine, TranscriptionConfig config, Consumer<String> onStatus) {
        this.engine = engine;
        this.config = config;
        this.onStatus = onStatus;
    }

    public TranscriptResult process(File wavFile, AudioChunkProducer producer) throws Exception {
        for (int i = 0; i < consumerCount; i++) {

            SpeechToTextEngine engine = new WhisperCppEngine(config.getModelPath(), config.getAccelerationMode());

            pool.submit(new AudioChunkConsumer(
                    chunkQueue,
                    resultQueue,
                    engine,
                    config,
                    onStatus
            ));
        }

        int count = producer.call();
        totalChunks = count;

        for (int i = 0; i < consumerCount; i++) {
            chunkQueue.put(AudioChunk.poison);
        }

        List<TranscriptSegment> allSegments = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        StringBuilder result = new StringBuilder();
        int finished = 0;

        while (completedChunks < totalChunks) {
            TranscriptResult tr = resultQueue.take();
            completedChunks++;
            onStatus.accept("Оброблено чанків: " + completedChunks + "/" + totalChunks);

            allSegments.addAll(tr.getSegments());
            fullText.append(tr.getFullText()).append("\n");
        }

        pool.shutdown();

        return new TranscriptResult(allSegments, fullText.toString().trim());
    }

    public BlockingQueue<AudioChunk> getChunkQueue() {
        return chunkQueue;
    }
}