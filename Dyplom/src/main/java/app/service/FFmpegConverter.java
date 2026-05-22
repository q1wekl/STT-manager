package app.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FFmpegConverter {
    private final String ffmpegPath = "bin/ffmpeg.exe";

    public List<File> convertAndSplitToWav(File input) {
        File tempDir = new File("temp/chunks_" + System.currentTimeMillis());
        tempDir.mkdirs();

        File outputPattern = new File(tempDir, "chunk_%03d.wav");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-i", input.getAbsolutePath(),
                    "-ar", "16000",
                    "-ac", "1",
                    "-c:a", "pcm_s16le",
                    "-f", "segment",
                    "-segment_time", "15",
                    "-reset_timestamps", "1",
                    outputPattern.getAbsolutePath()
            );

            Process process = pb.start();


            StringBuilder errorOutput = new StringBuilder();
            Thread stderrReader = new Thread(() -> {
                try {
                    errorOutput.append(
                            new String(process.getErrorStream().readAllBytes())
                    );
                } catch (Exception ignored) {}
            });
            stderrReader.start();

            int code = process.waitFor();
            stderrReader.join();
            if (code != 0) {
                throw new RuntimeException("FFmpeg failed (code " + code + "):\n" + errorOutput);
            }

            File[] files = tempDir.listFiles((dir, name) -> name.endsWith(".wav"));
            if (files == null || files.length == 0) {
                throw new RuntimeException("No chunks generated. FFmpeg output:\n" + errorOutput);
            }
            Arrays.sort(files);
            return new ArrayList<>(Arrays.asList(files));

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg error: ", e);
        }
    }


}
