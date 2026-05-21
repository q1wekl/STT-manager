package app.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import app.model.TranscriptResult;
import app.model.TranscriptionConfig;
import app.service.whisper.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OnnxWhisperService implements AutoCloseable {

    private static final int MAX_TOKENS = 100;

    private final OrtEnvironment env;
    private final WhisperEncoder encoder;
    private final WhisperDecoder decoder;
    private final WhisperTokenizer tokenizer;

    public OnnxWhisperService(String modelBasePath) throws OrtException {
        System.out.println("[Whisper] Завантаження моделі: " + modelBasePath);
        this.env = OrtEnvironment.getEnvironment();
        System.out.println("Providers: " + OrtEnvironment.getAvailableProviders());
        this.encoder = new WhisperEncoder(env, modelBasePath + "-encoder.onnx");
        System.out.println("[Whisper] Encoder завантажено");
        this.decoder = new WhisperDecoder(env, modelBasePath + "-decoder.onnx");
        System.out.println("[Whisper] Decoder завантажено");
        this.tokenizer = new WhisperTokenizer();
        System.out.println("[Whisper] Готово");

    }

    public TranscriptResult transcribe(float[] audioSamples, TranscriptionConfig config)
            throws OrtException {

        System.out.println("[Whisper] Починаємо transcribe, семплів: " + audioSamples.length);

        float[][][] mel;
        try {
            mel = MelSpectrogram.compute(audioSamples);
        } catch (Exception e) {
            throw new OrtException("Помилка MelSpectrogram: " + e.getMessage());
        }
        OnnxTensor encoderOutput = encoder.encode(env, mel);
        System.out.println("[Whisper] Encoder готово");

        List<Integer> tokens = new ArrayList<>(tokenizer.buildInitialTokens(config.getLanguage()));
        Map<String, OnnxTensor> pastKV = null;

        for (int step = 0; step < MAX_TOKENS; step++) {
            int lastToken = tokens.get(tokens.size() - 1);
            long[][] inputIds = {{lastToken}};

            DecodeStepResult stepResult = decoder.decode(env, inputIds, encoderOutput, pastKV);

            float[][][] logits = (float[][][]) stepResult.logits().getValue();
            int nextToken = tokenizer.greedyNextToken(logits);

            System.out.println("[Whisper] step=" + step + " nextToken=" + nextToken); // ← додай

            if (nextToken == WhisperTokenizer.EOT) {
                System.out.println("[Whisper] EOT на кроці " + step);
                break;
            }

            tokens.add(nextToken);
            pastKV = stepResult.pastKeyValues();
        }

        String text = decodeTokens(tokens);
        System.out.println("[Whisper] Результат: " + text);
        return new TranscriptResult(List.of(), text);
    }

    private String decodeTokens(List<Integer> tokens) {
        return tokenizer.decode(tokens);
    }

    @Override
    public void close() throws OrtException {
        encoder.close();
        decoder.close();
        env.close();
    }
}