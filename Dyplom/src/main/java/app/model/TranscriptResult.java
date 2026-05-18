package app.model;

import java.util.List;

public class TranscriptResult {
    private final List<TranscriptSegment> segments;
    private final String fullText;

    public TranscriptResult(List<TranscriptSegment> segments, String fullText) {
        this.segments = segments;
        this.fullText = fullText;
    }

    public List<TranscriptSegment> getSegments() {
        return segments;
    }

    public String getFullText() {
        return fullText;
    }
}
