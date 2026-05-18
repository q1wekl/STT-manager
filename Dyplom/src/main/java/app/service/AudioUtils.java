package app.utils;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioUtils {

    public static float[] loadAudio(String path)
            throws UnsupportedAudioFileException,
            IOException {

        File file = new File(path);

        AudioInputStream ais =
                AudioSystem.getAudioInputStream(file);

        AudioFormat baseFormat = ais.getFormat();

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000,
                16,
                1,
                2,
                16000,
                false
        );

        AudioInputStream pcmStream =
                AudioSystem.getAudioInputStream(
                        targetFormat,
                        ais
                );

        byte[] audioBytes = pcmStream.readAllBytes();

        float[] audioData =
                new float[audioBytes.length / 2];

        for (int i = 0; i < audioData.length; i++) {

            int low = audioBytes[i * 2] & 0xff;
            int high = audioBytes[i * 2 + 1];

            short sample = (short) ((high << 8) | low);

            audioData[i] = sample / 32768.0f;
        }

        return audioData;
    }
}