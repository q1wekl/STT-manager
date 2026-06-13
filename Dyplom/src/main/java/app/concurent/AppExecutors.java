package app.concurent;

import app.model.*;
import app.service.*;

import java.io.File;
import java.util.concurrent.*;

public class AppExecutors {

    private static final AppExecutors instance = new AppExecutors();

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final AudioDecoder decoder = new AudioDecoder();
    private final AudioChunkerService chunker = new AudioChunkerService();

    public static AppExecutors getInstance() {
        return instance;
    }

    public CompletableFuture<TranscriptResult> supplySpeechToText(
            File inputFile,
            TranscriptionConfig config,
            ProgressListener onStatus
    ) {
        return CompletableFuture.supplyAsync(() -> {
            onStatus.onProgress("Підготовка аудіо...", 0, 0);
            try {
                FFmpegConverter ffmpeg = new FFmpegConverter();
                File wavFile = ffmpeg.convertToWav(inputFile);

                SilenceDetectorService detector = new SilenceDetectorService();
                detector.detect(wavFile);

                onStatus.onProgress("Ініціалізація engine...", 0, 0);

                ResultManager manager = new ResultManager(config, onStatus);
                AudioChunkProducer producer = new AudioChunkProducer(
                        wavFile, manager.getChunkQueue(), decoder, chunker, detector
                );

                onStatus.onProgress("Запуск паралельної обробки...", 0, 0);
                TranscriptResult result = manager.process(wavFile, producer);

                if (wavFile.exists()) wavFile.delete();
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }

    public ExecutorService getIoExecutor() {
        return ioExecutor;
    }

    public void shutdown() {
        System.out.println("Shutting down AppExecutors");
        ioExecutor.shutdownNow();
    }
}