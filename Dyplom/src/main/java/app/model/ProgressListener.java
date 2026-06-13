package app.model;

@FunctionalInterface
public interface ProgressListener {
    void onProgress(String message, int completed, int total);
}