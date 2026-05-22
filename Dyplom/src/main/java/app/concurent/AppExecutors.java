package app.concurent;

import app.model.TranscriptResult;
import app.model.TranscriptionConfig;
import app.service.AudioPipelineService;
import app.service.FFmpegConverter;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AppExecutors {

    private static final AppExecutors instance = new AppExecutors();


    private final ExecutorService sttExecutor =
            Executors.newSingleThreadExecutor();

    private final ExecutorService ioExecutor =
            Executors.newSingleThreadExecutor();

    private AudioPipelineService audioPipeline;
    private String currentModelPath;

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
                    List<File> chunks = ffmpeg.convertAndSplitToWav(inputFile);
                    onStatus.accept("Чанків: " + chunks.size());
                    return chunks;
                }, ioExecutor).thenApplyAsync(chunks -> {
                    ensurePipeline(config.getModelPath());
                    onStatus.accept("Обробка на GPU (Vulkan)...");

                    StringBuilder fullText = new StringBuilder();
                    for (int i = 0; i < chunks.size(); i++) {
                        File chunk = chunks.get(i);
                        try {
                            onStatus.accept("Обробка чанку (" + (i + 1) + "/" + chunks.size() + "): " + chunk.getName());
                            TranscriptResult r = audioPipeline.processChunk(chunk, config);
                            if (r.getFullText() != null && !r.getFullText().isBlank()) {
                                fullText.append(r.getFullText()).append("\n");
                            }
                        } catch (Exception e) {
                            System.err.println("Помилка обробки чанку " + chunk.getName() + ": " + e.getMessage());
                        } finally {
                            chunk.delete();
                        }
                    }
                    onStatus.accept("Готово");
                    return new TranscriptResult(List.of(), fullText.toString().trim());
                }, sttExecutor);
    }

    public void shutdown() {
        ioExecutor.shutdown();
        sttExecutor.shutdown();
    }
}