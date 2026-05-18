package app.service;

import app.model.TranscriptResult;

import java.io.File;

public class AudioPipelineService {
    private final FFmpegConverter ffmpeg = new FFmpegConverter();
    private final AudioDecoder decoder = new AudioDecoder();
    private final WhisperService whisper;

    public AudioPipelineService(String modelPath) {
        this.whisper = new WhisperService(modelPath);
    }
    public TranscriptResult process(File input){
        return processInternal(input);
    }
    public TranscriptResult processInternal(File input){
        File wav = ffmpeg.convertToWav(input);
        float[] audio = decoder.decode(wav);
        return whisper.transcribe(audio);
    }
}
