package app.service.whisper;

import app.model.TranscriptResult;
import app.model.TranscriptionConfig;

public interface SpeechToTextEngine {
    TranscriptResult transcribe(
            float[] audioSamples,
            TranscriptionConfig config
    ) throws Exception;

    void close();
}
