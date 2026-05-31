package app.concurent;

import app.model.AudioChunk;
import app.model.TranscriptResult;
import app.model.TranscriptSegment;
import app.model.TranscriptionConfig;
import app.service.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AppExecutors {

    private static final AppExecutors instance = new AppExecutors();

    private final ExecutorService sttExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private AudioPipelineService audioPipeline;
    private String currentModelPath;
    TextMergeService merger = new TextMergeService();

    private final AudioDecoder decoder = new AudioDecoder();
    private final AudioChunkerService chunker = new AudioChunkerService();

    public static AppExecutors getInstance() {
        return instance;
    }

    private synchronized void ensurePipeline(String modelPath) {
        if (audioPipeline == null || !modelPath.equals(currentModelPath)) {
            audioPipeline = new AudioPipelineService(modelPath);
            currentModelPath = modelPath;
            System.out.println("[Executors] Pipeline initialized: " + modelPath);
        }
    }

    public CompletableFuture<TranscriptResult> supplySpeechToText(
            File inputFile,
            TranscriptionConfig config,
            Consumer<String> onStatus
    ) {
        return CompletableFuture.supplyAsync(() -> {
                    onStatus.accept("Підготовка аудіо...");
                    FFmpegConverter ffmpeg = new FFmpegConverter();
                    File wavFile = ffmpeg.convertToWav(inputFile);
                    float[] audio = decoder.decode(wavFile);
                    List<AudioChunk> chunks= chunker.createChunks(audio,25,2);
                    wavFile.delete();
                    onStatus.accept("Чанків: " + chunks.size());
                    return chunks;
                }, ioExecutor).thenApplyAsync(chunks -> {
                    ensurePipeline(config.getModelPath());
                    String mergedText = "";
                    for (int i = 0; i < chunks.size(); i++) {
                        AudioChunk chunk = chunks.get(i);
                        try {
                            onStatus.accept("Обробка чанку (" + (i + 1) + "/" + chunks.size() + "): Chunk " + (i + 1));
                            TranscriptResult r = audioPipeline.processChunk(chunk, config);
                            String chunkText = r.getFullText();
                            if (chunkText != null && !chunkText.isBlank()) {
                                mergedText = merger.merge(mergedText, chunkText);
                            }
                        } catch (Exception e) {
                            System.err.println("Помилка обробки чанку Chunk " + (i + 1) + ": " + e.getMessage());
                        }
                    }
                    onStatus.accept("Готово");
                    return new TranscriptResult(List.of(), mergedText.trim());
                }, sttExecutor);
    }

    public void shutdown() {
        ioExecutor.shutdown();
        sttExecutor.shutdown();
    }
}