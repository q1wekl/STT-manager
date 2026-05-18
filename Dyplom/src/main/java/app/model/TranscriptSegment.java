package app.model;

public class TranscriptSegment {
    private final String text;
    private final long startTime;
    private final long endTime;

    public TranscriptSegment(String text, long startTime, long endTime) {
        this.text = text;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getText() {
        return text;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
