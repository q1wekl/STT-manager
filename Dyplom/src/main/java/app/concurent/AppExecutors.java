package app.concurent;

import app.model.*;
import app.service.*;
import app.service.whisper.SpeechToTextEngine;
import app.service.whisper.WhisperCppEngine;


import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

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

            try {
                FFmpegConverter ffmpeg = new FFmpegConverter();
                File wavFile = ffmpeg.convertToWav(inputFile);

                SilenceDetectorService detector = new SilenceDetectorService();
                detector.detect(wavFile);

                onStatus.accept("Ініціалізація engine...");

                SpeechToTextEngine engine =
                        new WhisperCppEngine(
                                config.getModelPath(),
                                config.getAccelerationMode()
                        );

                ResultManager manager =
                        new ResultManager(engine, config, onStatus);

                AudioChunkProducer producer =
                        new AudioChunkProducer(
                                wavFile,
                                manager.getChunkQueue(),
                                decoder,
                                chunker,
                                detector
                        );

                onStatus.accept("Запуск паралельної обробки...");

                TranscriptResult result = manager.process(wavFile, producer);

                if (wavFile.exists()) {
                    wavFile.delete();
                }

                return result;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }

    public void shutdown() {
        System.out.println("Shutting down AppExecutors");

        ioExecutor.shutdownNow();
        sttExecutor.shutdownNow();
    }
}