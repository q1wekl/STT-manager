package app.service;

import java.io.File;

public class AudioDecoder {
    public float[] decode(File wavFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "bin/ffmpeg.exe",
                    "-i", wavFile.getAbsolutePath(),
                    "-f", "f32le",
                    "-acodec", "pcm_f32le",
                    "-ac", "1",
                    "-ar", "16000",
                    "pipe:1"
            );

            Process process = pb.start();

            byte[] bytes = process.getInputStream().readAllBytes();
            process.waitFor();

            int samples = bytes.length / 4;
            float[] audio = new float[samples];

            for (int i = 0; i < samples; i++) {
                int j = i * 4;

                int asInt =
                        (bytes[j] & 0xff) |
                                ((bytes[j + 1] & 0xff) << 8) |
                                ((bytes[j + 2] & 0xff) << 16) |
                                ((bytes[j + 3] & 0xff) << 24);

                audio[i] = Float.intBitsToFloat(asInt);
            }

            return audio;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
