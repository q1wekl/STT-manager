package app.concurent;

import ai.onnxruntime.OrtException;
import app.model.TranscriptResult;
import app.model.TranscriptionConfig;
import app.service.AudioPipelineService;
import app.service.FFmpegConverter;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AppExecutors {
    private static final AppExecutors instance = new AppExecutors();

    private final ExecutorService ioExecutor  = Executors.newFixedThreadPool(1); // FFmpeg
    private final ExecutorService sttExecutor = Executors.newFixedThreadPool(1); // Whisper
    private final AudioPipelineService audioPipeline = new AudioPipelineService();

    public static AppExecutors getInstance() { return instance; }

    public void resetWhisperService() {
        audioPipeline.reset();
    }

    public CompletableFuture<TranscriptResult> supplySpeechToText(

            File inputFile,
            TranscriptionConfig config,
            java.util.function.Consumer<String> onStatus  // ← callback для статусу
    ) {

        System.out.println("[Executors] supplySpeechToText викликано");
        return CompletableFuture.supplyAsync(() -> {
                    System.out.println("[Executors] ioExecutor: починаємо FFmpeg");
                    FFmpegConverter ffmpeg = new FFmpegConverter();
                    List<File> chunks = ffmpeg.convertAndSplitToWav(inputFile);
                    System.out.println("[Executors] FFmpeg готово, чанків: " + chunks.size()); // ← додай
                    return chunks;
                }, ioExecutor)

                .thenApplyAsync(chunks -> {
                    System.out.println("[Executors] sttExecutor: починаємо STT");

                    StringBuilder finalText = new StringBuilder();

                    // Послідовно — без вкладених CompletableFuture на тому ж executor
                    for (File chunk : chunks) {
                        try {
                            System.out.println("[Executors] Обробка: " + chunk.getName());
                            TranscriptResult res = audioPipeline.processChunk(chunk, config);
                            System.out.println("[Executors] Готово: " + chunk.getName());
                            finalText.append(res.getFullText()).append("\n");
                        } catch (OrtException e) {
                            throw new RuntimeException("STT помилка: " + chunk.getName(), e);
                        } finally {
                            chunk.delete();
                        }
                    }

                    if (!chunks.isEmpty()) {
                        chunks.get(0).getParentFile().delete();
                    }

                    return new TranscriptResult(java.util.List.of(), finalText.toString().trim());

                }, sttExecutor);
    }
}