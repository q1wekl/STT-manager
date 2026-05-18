package app.service;

import app.model.TranscriptResult;
import app.model.TranscriptionConfig;

import java.io.File;

public class AudioPipelineService {
    private final AudioDecoder decoder = new AudioDecoder();
    private OnnxWhisperService whisper;

    public TranscriptResult processChunk(File wavChunk, TranscriptionConfig config){
        if (whisper == null) {
            this.whisper = new OnnxWhisperService(config.getModelPath());
        }
        float[] audio = decoder.decode(wavChunk);
        return whisper.transcribe(audio, config);
    }
}