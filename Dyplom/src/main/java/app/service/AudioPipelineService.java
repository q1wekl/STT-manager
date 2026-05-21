package app.service;

import ai.onnxruntime.OrtException;
import app.model.TranscriptResult;
import app.model.TranscriptionConfig;

import java.io.File;

public class AudioPipelineService {
    private final AudioDecoder decoder = new AudioDecoder();
    private OnnxWhisperService whisper;

    public TranscriptResult processChunk(File wavChunk, TranscriptionConfig config) throws OrtException {
        System.out.println("[Pipeline] processChunk викликано: " + wavChunk.getAbsolutePath());
        System.out.println("[Pipeline] Файл існує: " + wavChunk.exists() + ", розмір: " + wavChunk.length());

        if (whisper == null) {
            System.out.println("[Pipeline] Створюємо OnnxWhisperService...");
            this.whisper = new OnnxWhisperService(config.getModelPath());
            System.out.println("[Pipeline] OnnxWhisperService створено");
        }

        System.out.println("[Pipeline] Декодуємо аудіо...");
        float[] audio = decoder.decode(wavChunk);
        System.out.println("[Pipeline] Декодовано семплів: " + audio.length);

        return whisper.transcribe(audio, config);
    }
    public void reset() {
        if (whisper != null) {
            try { whisper.close(); } catch (Exception e) { e.printStackTrace(); }
            whisper = null;
        }
    }
}