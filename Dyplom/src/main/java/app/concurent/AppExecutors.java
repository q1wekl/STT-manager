package app.concurent;

import app.model.TranscriptResult;
import app.service.AudioPipelineService;
import javafx.concurrent.Task;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final AppExecutors instance = new AppExecutors();
    private final ExecutorService sttExecutor = Executors.newFixedThreadPool(1);
    private final ExecutorService videoExecutor = Executors.newFixedThreadPool(2);
    private AudioPipelineService audioPipeline = new AudioPipelineService("models/whisper/ggml-small.bin");

    public static AppExecutors getInstance() {
        return instance;
    }
    public CompletableFuture<TranscriptResult> supplySpeechToText(File inputFile){
        return CompletableFuture.supplyAsync(()->{
                return audioPipeline.process(inputFile);
                },sttExecutor);
    }
    public void shutdown(){
        sttExecutor.shutdown();
        videoExecutor.shutdown();
    }
}
