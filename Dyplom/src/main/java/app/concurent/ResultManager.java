package app.concurent;

import app.model.*;
import app.service.whisper.SpeechToTextEngine;
import app.service.whisper.WhisperCppEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class ResultManager {

    private final ProgressListener onStatus;
    private final BlockingQueue<AudioChunk> chunkQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<TranscriptResult> resultQueue = new LinkedBlockingQueue<>();
    private final TranscriptionConfig config;
    private final int consumerCount;
    private final ExecutorService pool;
    private final ModelConfig modelConfig;
    private int totalChunks;
    private int completedChunks = 0;


    public ResultManager(TranscriptionConfig config, ProgressListener onStatus) {
        this.config = config;
        this.onStatus = onStatus;
        this.modelConfig = ModelConfig.of(config.getModelId());
        this.consumerCount = modelConfig.consumerCount;
        this.pool = Executors.newFixedThreadPool(consumerCount);
    }

    public TranscriptResult process(File wavFile, AudioChunkProducer producer) throws Exception {
        String modelPath = modelConfig.path;

        for (int i = 0; i < consumerCount; i++) {
            SpeechToTextEngine engine = new WhisperCppEngine(modelPath, config.getAccelerationMode());
            pool.submit(new AudioChunkConsumer(chunkQueue, resultQueue, engine, config));
        }

        int count = producer.call();
        totalChunks = count;

        for (int i = 0; i < consumerCount; i++) {
            chunkQueue.put(AudioChunk.poison);
        }

        List<TranscriptResult> results = new ArrayList<>();
        while (results.size() < totalChunks) {
            results.add(resultQueue.take());
            completedChunks++;
            onStatus.onProgress("Оброблено сегменти", completedChunks, totalChunks);
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        results.sort(Comparator.comparingInt(TranscriptResult::getChunkIndex));

        List<TranscriptSegment> allSegments = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        for (TranscriptResult tr : results) {
            allSegments.addAll(tr.getSegments());
            fullText.append(tr.getFullText()).append("\n");
        }
        return new TranscriptResult(allSegments, fullText.toString().trim(), -1);
    }

    public BlockingQueue<AudioChunk> getChunkQueue() {
        return chunkQueue;
    }
}