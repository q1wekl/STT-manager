package app.service;

import org.jtransforms.fft.FloatFFT_1D;

public class MelSpectrogramExtractor {

    private static final int SAMPLE_RATE = 16000;
    private static final int N_FFT = 400;
    private static final int HOP_LENGTH = 160;
    private static final int MEL_BINS = 80;
    private static final int MAX_FRAMES = 3000;

    public static float[] extract(float[] audio) {

        int frames =
                Math.min(
                        MAX_FRAMES,
                        audio.length / HOP_LENGTH
                );

        float[] melFeatures =
                new float[MEL_BINS * MAX_FRAMES];

        FloatFFT_1D fft =
                new FloatFFT_1D(N_FFT);

        for (int frame = 0; frame < frames; frame++) {

            int start = frame * HOP_LENGTH;

            float[] window = new float[N_FFT * 2];

            for (int i = 0; i < N_FFT; i++) {

                if (start + i < audio.length) {

                    // Hann window
                    float hann =
                            (float)(
                                    0.5 *
                                            (1 - Math.cos(
                                                    2 * Math.PI * i / (N_FFT - 1)
                                            ))
                            );

                    window[i] =
                            audio[start + i] * hann;
                }
            }

            fft.realForwardFull(window);

            for (int mel = 0; mel < MEL_BINS; mel++) {

                int fftIndex =
                        mel * (N_FFT / 2) / MEL_BINS;

                float real =
                        window[2 * fftIndex];

                float imag =
                        window[2 * fftIndex + 1];

                float magnitude =
                        (float)Math.sqrt(
                                real * real +
                                        imag * imag
                        );

                float logMel =
                        (float)Math.log10(
                                magnitude + 1e-6
                        );

                melFeatures[mel * MAX_FRAMES + frame] =
                        logMel;
            }
        }

        return melFeatures;
    }
}