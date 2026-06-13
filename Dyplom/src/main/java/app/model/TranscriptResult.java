package app.model;

import java.util.List;

public class TranscriptResult {
    private final List<TranscriptSegment> segments;
    private final String fullText;
    private final int chunkIndex;

    public TranscriptResult(List<TranscriptSegment> segments, String fullText, int chunkIndex) {
        this.segments = segments;
        this.fullText = fullText;
        this.chunkIndex = chunkIndex;
    }

    public List<TranscriptSegment> getSegments() {
        return segments;
    }
    public int getChunkIndex() { return chunkIndex; }
    public String getFullText() {
        return fullText;
    }
}
