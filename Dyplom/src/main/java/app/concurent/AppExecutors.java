package app.concurent;

import app.model.TranscriptResult;
import app.model.TranscriptionConfig;
import app.service.AudioPipelineService;
import app.service.FFmpegConverter;
import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AppExecutors {
    private static final AppExecutors instance = new AppExecutors();
    private final ExecutorService sttExecutor = Executors.newFixedThreadPool(1);
    private final ExecutorService videoExecutor = Executors.newFixedThreadPool(2);
    private final AudioPipelineService audioPipeline = new AudioPipelineService();

    public static AppExecutors getInstance() {
        return instance;
    }
    public CompletableFuture<TranscriptResult> supplySpeechToText(File inputFile, TranscriptionConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            FFmpegConverter ffmpeg = new FFmpegConverter();
            List<File> chunks = ffmpeg.convertAndSplitToWav(inputFile);

            List<CompletableFuture<TranscriptResult>> futures = chunks.stream()
                    .map(chunk -> CompletableFuture.supplyAsync(() -> {
                        TranscriptResult res = audioPipeline.processChunk(chunk, config);
                        chunk.delete();
                        return res;
                    }, sttExecutor))
                    .collect(Collectors.toList());

            List<TranscriptResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            StringBuilder finalText = new StringBuilder();
            for (TranscriptResult res : results) {
                finalText.append(res.getFullText()).append("\n");
            }
            if (!chunks.isEmpty()) {
                chunks.get(0).getParentFile().delete();
            }
            return new TranscriptResult(new ArrayList<>(), finalText.toString());
        });
    }

}
