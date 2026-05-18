package app.service;

import app.model.TranscriptResult;
import app.model.TranscriptionConfig;

import java.io.File;

public class AudioPipelineService {
    private final AudioDecoder decoder = new AudioDecoder();
    private WhisperService whisper;
    private String currentModelPath = "";

    public TranscriptResult processChunk(File wavChunk, TranscriptionConfig config){
        if(whisper ==null|| !currentModelPath.equals(config.getModelPath())){
            this.whisper = new WhisperService(config.getModelPath());
            this.currentModelPath= config.getModelPath();
        }
        float[] audio = decoder.decode(wavChunk);
        return whisper.transcribe(audio,config);
    }
}
