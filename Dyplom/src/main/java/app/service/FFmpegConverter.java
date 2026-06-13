package app.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FFmpegConverter {

    private final String ffmpegPath = "bin/ffmpeg.exe";

    public File convertToWav(File input) {
        File tempDir = new File("temp");
        tempDir.mkdirs();
        File output = new File(tempDir, "audio_" + System.currentTimeMillis() + ".wav");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-i", input.getAbsolutePath(),
                    "-ar", "16000",
                    "-ac", "1",
                    "-c:a", "pcm_s16le",
                    output.getAbsolutePath()
            );

            Process process = pb.start();
            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException("FFmpeg failed");
            }

            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public File convertToMp3(File inputFile) throws Exception {
        Path output = Files.createTempFile("audio_export_", ".mp3");
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath, "-y",
                "-i", inputFile.getAbsolutePath(),
                "-q:a", "2",
                output.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int code = process.waitFor();
        if (code != 0) {
            throw new RuntimeException("FFmpeg failed with code: " + code);
        }
        return output.toFile();
    }
}