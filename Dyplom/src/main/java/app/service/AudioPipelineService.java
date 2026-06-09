package app.service;

import app.model.AudioChunk;
import app.model.TranscriptResult;
import app.model.TranscriptionConfig;
import app.service.whisper.SpeechToTextEngine;
import app.service.whisper.WhisperCppEngine;

public class AudioPipelineService {

    private final AudioDecoder decoder = new AudioDecoder();

    public AudioPipelineService(String modelPath) {
    }

    public TranscriptResult processChunk(AudioChunk chunk, TranscriptionConfig config) {
        float[] audio = chunk.getSamples();
        if (isSilent(audio)) {
            return new TranscriptResult(java.util.List.of(), "");
        }
        SpeechToTextEngine engine = new WhisperCppEngine(config.getModelPath(), config.getAccelerationMode());

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