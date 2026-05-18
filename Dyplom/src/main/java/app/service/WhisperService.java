package app.service;
import app.model.TranscriptResult;
import app.model.TranscriptSegment;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WhisperService {

    private final WhisperJNI whisper;
    private WhisperContext context;

    public WhisperService(String modelPath) {
        try {
            WhisperJNI.loadLibrary();
            this.whisper = new WhisperJNI();
            this.context = whisper.init(Path.of(modelPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TranscriptResult transcribe(float[] audio){
            List<TranscriptSegment> segmentsList= new ArrayList<>();

            WhisperFullParams params = new WhisperFullParams();
            int result = whisper.full(context, params, audio, audio.length);

            if (result != 0) {
                throw new RuntimeException("Whisper failed" + result);
            }

            StringBuilder sb = new StringBuilder();

            int segmentsCount = whisper.fullNSegments(context);
            for (int i = 0; i < segmentsCount; i++) {
                String text = whisper.fullGetSegmentText(context,i);
                long t0 = whisper.fullGetSegmentTimestamp0(context, i);
                long t1 = whisper.fullGetSegmentTimestamp1(context, i);
                TranscriptSegment segment =
                        new TranscriptSegment(text, t0, t1);
                segmentsList.add(segment);
                sb.append(text).append("\n");
            }

        return new TranscriptResult(
                segmentsList,
                sb.toString()
        );


    }

}
