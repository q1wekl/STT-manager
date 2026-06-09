package app.model;

public class SilenceRegion {
    private final double startSec;
    private final double endSec;
    public SilenceRegion(double startSec, double endSec) {
        this.startSec = startSec;
        this.endSec = endSec;
    }

    public double getStartSec() {
        return startSec;
    }

    public double getEndSec() {
        return endSec;
    }
}
