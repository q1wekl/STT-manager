package app.service;

import app.model.AudioChunk;
import app.model.TranscriptResult;
import app.model.TranscriptionConfig;
import app.service.whisper.SpeechToTextEngine;
import app.service.whisper.WhisperCppEngine;

import java.io.File;

public class AudioPipelineService {

    private final AudioDecoder decoder = new AudioDecoder();

    private final ThreadLocal<SpeechToTextEngine> engines;

    public AudioPipelineService(String modelPath) {
        this.engines = ThreadLocal.withInitial(() -> new WhisperCppEngine(modelPath));
    }

    public TranscriptResult processChunk(AudioChunk chunk, TranscriptionConfig config) {
        float[] audio = chunk.getSamples();
        if (isSilent(audio)) {
            return new TranscriptResult(java.util.List.of(), "");
        }

        SpeechToTextEngine engine = engines.get();
        try {
            return engine.transcribe(audio, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSilent(float[] audio) {
        double energy = 0.0;
        for (float v : audio) {
            energy += v * v;
        }
        double rms = Math.sqrt(energy / audio.length);

        return rms < 0.005;
    }
}