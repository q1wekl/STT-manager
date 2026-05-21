package app.service.whisper;

import org.jtransforms.fft.FloatFFT_1D;

import java.io.File;

public class MelSpectrogram {

    private static final int SAMPLE_RATE = 16000;
    private static final int N_FFT       = 400;   // ← повертаємо 400
    private static final int HOP_LENGTH  = 160;
    private static final int N_MELS      = 80;
    private static final int N_FRAMES    = 3000;
    private static final int FFT_SIZE    = 512;    // ← степінь 2 для FFT алгоритму

    public static float[][][] compute(float[] audio) {
        System.out.println("[Mel] Починаємо, семплів: " + audio.length);

        float maxAmp = 0;
        for (float v : audio) if (Math.abs(v) > maxAmp) maxAmp = Math.abs(v);
        System.out.println("[Mel] Макс амплітуда: " + maxAmp);

        audio = padOrTrim(audio);
        System.out.println("[Mel] Padding готово");

        audio = reflectPad(audio, N_FFT / 2);

        float[][] magnitudes = stft(audio);
        System.out.println("[Mel] STFT готово, frames: " + magnitudes[0].length);

        float[][] melSpec = applyMelFilterbank(magnitudes);
        System.out.println("[Mel] Filterbank готово");

        float[][] logMel = logNormalize(melSpec);

        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE, sum = 0;
        for (float[] row : logMel)
            for (float v : row) {
                if (v < min) min = v;
                if (v > max) max = v;
                sum += v;
            }
        System.out.println("[Mel] LogNorm готово min=" + min + " max=" + max
                + " avg=" + (sum / (logMel.length * logMel[0].length)));

        return new float[][][]{logMel};
    }


    private static float[] padOrTrim(float[] audio) {
        int targetLength = N_FRAMES * HOP_LENGTH; // 480000
        float[] result = new float[targetLength];
        System.arraycopy(audio, 0, result, 0, Math.min(audio.length, targetLength));
        return result;
    }

    // Reflect padding — дзеркальне відображення країв
    private static float[] reflectPad(float[] audio, int pad) {
        // Після padding довжина = 480000 + 2*200 = 480400
        // STFT дасть: (480400 - 400) / 160 + 1 = 3001 — занадто багато
        // Тому обрізаємо щоб отримати рівно 3000 frames:
        // потрібна довжина = (3000 - 1) * 160 + 400 + 2*200 = 479840 + 400 + 400 = 480640...
        // Простіше: не обрізати після pad, а перевірити кількість frames в stft

        float[] result = new float[audio.length + 2 * pad];
        for (int i = 0; i < pad; i++) {
            result[i] = audio[pad - i];
        }
        System.arraycopy(audio, 0, result, pad, audio.length);
        for (int i = 0; i < pad; i++) {
            result[result.length - 1 - i] = audio[audio.length - 2 - i];
        }
        return result;
    }

    private static float[][] stft(float[] audio) {
        float[] window = hanningWindow(N_FFT);
        int numFrames = Math.min(N_FRAMES, (audio.length - N_FFT) / HOP_LENGTH + 1);
        float[][] magnitudes = new float[FFT_SIZE / 2 + 1][numFrames];

        FloatFFT_1D fft = new FloatFFT_1D(FFT_SIZE);

        for (int frame = 0; frame < numFrames; frame++) {
            int start = frame * HOP_LENGTH;
            float[] buf = new float[FFT_SIZE * 2];
            for (int i = 0; i < N_FFT && start + i < audio.length; i++) {
                buf[i] = audio[start + i] * window[i];
            }
            fft.realForwardFull(buf);
            for (int k = 0; k <= FFT_SIZE / 2; k++) {
                float re = buf[2 * k];
                float im = buf[2 * k + 1];
                // ← амплітуда замість потужності, і ділимо на N_FFT
                magnitudes[k][frame] = (float) Math.sqrt(re * re + im * im) / N_FFT;
            }
        }
        return magnitudes;
    }

    private static float[] hanningWindow(int size) {
        float[] w = new float[size];
        for (int i = 0; i < size; i++) {
            w[i] = (float) (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / size)));
        }
        return w;
    }

    private static float[][] applyMelFilterbank(float[][] magnitudes) {
        int numFrames = magnitudes[0].length;
        float[][] filters = buildMelFilterbank();
        float[][] mel = new float[N_MELS][numFrames];
        for (int m = 0; m < N_MELS; m++) {
            for (int f = 0; f < numFrames; f++) {
                float sum = 0f;
                for (int k = 0; k < filters[m].length; k++) {
                    sum += filters[m][k] * magnitudes[k][f];
                }
                mel[m][f] = sum;
            }
        }
        return mel;
    }

    private static float[][] buildMelFilterbank() {
        int numFreqs = FFT_SIZE / 2 + 1; // 257
        double fMin = 0.0;
        double fMax = SAMPLE_RATE / 2.0; // 8000.0

        // Точно як в librosa/whisper: N_MELS+2 точок в mel шкалі
        double melMin = hzToMel(fMin);
        double melMax = hzToMel(fMax);

        double[] melPoints = new double[N_MELS + 2];
        for (int i = 0; i < melPoints.length; i++) {
            melPoints[i] = melMin + i * (melMax - melMin) / (N_MELS + 1);
        }

        // Конвертуємо в Hz і потім в FFT bin індекси
        // Ключова різниця: ділимо на SAMPLE_RATE, не на N_FFT
        double[] hzPoints = new double[N_MELS + 2];
        for (int i = 0; i < melPoints.length; i++) {
            hzPoints[i] = melToHz(melPoints[i]);
        }

        // FFT bin для кожної hz точки — використовуємо FFT_SIZE
        int[] bins = new int[N_MELS + 2];
        for (int i = 0; i < hzPoints.length; i++) {
            bins[i] = (int) Math.floor(hzPoints[i] * FFT_SIZE / SAMPLE_RATE);
            bins[i] = Math.min(Math.max(bins[i], 0), numFreqs - 1);
        }

        float[][] filters = new float[N_MELS][numFreqs];
        for (int m = 0; m < N_MELS; m++) {
            int left   = bins[m];
            int center = bins[m + 1];
            int right  = bins[m + 2];

            for (int k = left; k <= right; k++) {
                if (k < numFreqs) {
                    if (k < center && center > left) {
                        filters[m][k] = (float)(k - left) / (center - left);
                    } else if (k >= center && right > center) {
                        filters[m][k] = (float)(right - k) / (right - center);
                    }
                }
            }
        }
        return filters;
    }

    private static float[][] logNormalize(float[][] mel) {
        int rows = mel.length;
        int cols = mel[0].length;
        float[][] log = new float[rows][cols];

        // ← Math.log замість Math.log10
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                log[i][j] = (float) Math.log(Math.max(mel[i][j], 1e-10f));

        float logMax = Float.NEGATIVE_INFINITY;
        for (float[] row : log)
            for (float v : row)
                if (v > logMax) logMax = v;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                log[i][j] = Math.max(log[i][j], logMax - 8.0f);
                log[i][j] = (log[i][j] + 4.0f) / 4.0f;
            }
        }

        return log;
    }

    private static double hzToMel(double hz) {
        return 2595.0 * Math.log10(1.0 + hz / 700.0);
    }

    private static double melToHz(double mel) {
        return 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0);
    }
}