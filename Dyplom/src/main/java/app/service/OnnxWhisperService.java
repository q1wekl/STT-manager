package app.service;

import ai.onnxruntime.*;
import app.model.TranscriptResult;
import app.model.TranscriptionConfig;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import app.service.MelSpectrogramExtractor;
public class OnnxWhisperService {
    private final OrtEnvironment env;
    private final OrtSession encoderSession;
    private final OrtSession decoderSession;
    private final Map<Long, String> vocab = new HashMap<>();
    private final Map<Character, Integer> byteEncoder = new HashMap<>();
    private final Map<Integer, Character> byteDecoder = new HashMap<>();

    public OnnxWhisperService(String basePath) {
        try {
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

            String encoderPath = basePath + "-encoder.onnx";
            String decoderPath = basePath + "-decoder.onnx";

            if (!new File(encoderPath).exists() || !new File(decoderPath).exists()) {
                throw new IllegalArgumentException("Критична помилка: Не знайдено файли: \n" + encoderPath + "\n або \n" + decoderPath);
            }

            this.encoderSession = env.createSession(encoderPath, options);
            this.decoderSession = env.createSession(decoderPath, options);

            initByteEncoderDecoder();
            loadVocabulary();

        } catch (Exception e) {
            throw new RuntimeException("Помилка ініціалізації ONNX сесій: " + e.getMessage(), e);
        }
    }

    private void initByteEncoderDecoder() {
        int n = 0;
        for (int b = Character.valueOf('!'); b <= Character.valueOf('~'); b++) {
            byteEncoder.put((char) b, b);
            byteDecoder.put(b, (char) b);
        }
        for (int b = Character.valueOf('¡'); b <= Character.valueOf('¬'); b++) {
            byteEncoder.put((char) b, b);
            byteDecoder.put(b, (char) b);
        }
        for (int b = Character.valueOf('®'); b <= Character.valueOf('ÿ'); b++) {
            byteEncoder.put((char) b, b);
            byteDecoder.put(b, (char) b);
        }
        for (int b = 0; b < 256; b++) {
            if (!byteDecoder.containsKey(b)) {
                int code = 256 + n;
                byteEncoder.put((char) code, b);
                byteDecoder.put(b, (char) code);
                n++;
            }
        }
    }

    private void loadVocabulary() {
        // Спроба 1: Завантаження з resources
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("vocab.json")) {
            if (is != null) {
                parseVocabStream(is);
                System.out.println("[INFO] Словник успішно завантажено з resources.");
                return;
            }
        } catch (Exception e) {
            System.err.println("[WARN] Не вдалося завантажити словник з resources: " + e.getMessage());
        }

        // Спроба 2: Альтернативне завантаження з файлової системи (поруч з моделями)
        String fallbackPath = "models/whisper/whisper-vocab.json";
        File fallbackFile = new File(fallbackPath);
        if (fallbackFile.exists()) {
            try (InputStream is = new java.io.FileInputStream(fallbackFile)) {
                parseVocabStream(is);
                System.out.println("[INFO] Словник успішно завантажено з файлової системи: " + fallbackPath);
                return;
            } catch (Exception e) {
                System.err.println("[ERROR] Помилка читання файлу словника: " + e.getMessage());
            }
        }

        // Якщо нічого не знайшли — жорсткий демо-режим
        System.err.println("[CRITICAL] vocab.json не знайдено ні в resources, ні в models/whisper/. Активується демо-режим.");
        vocab.put(220L, " ");
        vocab.put(273L, "і");
        vocab.put(1123L, "Привіт");
    }

    private void parseVocabStream(InputStream is) throws Exception {

        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String content = sb.toString();

        // Whisper vocab format:
        // "token": 1234

        Pattern pattern =
                Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");

        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {

            String token = matcher.group(1);
            long id = Long.parseLong(matcher.group(2));

            if (token.startsWith("\\u")) {
                token = unescapeUnicode(token);
            }

            vocab.put(id, token);
        }

        System.out.println(
                "[INFO] Завантажено токенів: " + vocab.size()
        );
    }

    private String unescapeUnicode(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            char ch = str.charAt(i);
            if (ch == '\\' && i + 1 < str.length() && str.charAt(i + 1) == 'u') {
                String hex = str.substring(i + 2, i + 6);
                sb.append((char) Integer.parseInt(hex, 16));
                i += 6;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    public TranscriptResult transcribe(float[] audio, TranscriptionConfig config) {
        if (audio == null || audio.length == 0) {
            return new TranscriptResult(new ArrayList<>(), "Аудіофайл порожній");
        }

        OrtSession.Result encoderResult = null;
        OnnxTensor inputFeaturesTensor = null;

        try {
            System.out.println("[INFO] Генерація Mel Spectrogram...");

            float[] melFeatures =
                    app.service.MelSpectrogramExtractor.extract(audio);

            System.out.println(
                    "[INFO] Mel features size: "
                            + melFeatures.length
            );

            long[] encoderShape = new long[]{1, 80, 3000};

            FloatBuffer melBuffer =
                    FloatBuffer.wrap(melFeatures);

            inputFeaturesTensor =
                    OnnxTensor.createTensor(
                            env,
                            melBuffer,
                            encoderShape
                    );

            Map<String, OnnxTensor> encoderInputs = new HashMap<>();
            encoderInputs.put("input_features", inputFeaturesTensor);

            encoderResult = encoderSession.run(encoderInputs);
            OnnxTensor lastHiddenState = (OnnxTensor) encoderResult.get(0);

            List<Long> currentTokens = new ArrayList<>();
            currentTokens.add(50258L);
            currentTokens.add(50363L);
            currentTokens.add(50359L);

            int maxNewTokens = 448;
            long endOfTextToken = 50257L;

            for (int step = 0; step < maxNewTokens; step++) {
                long[] decoderShape = new long[]{1, currentTokens.size()};
                long[] tokensArray = currentTokens.stream().mapToLong(Long::longValue).toArray();
                OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokensArray), decoderShape);

                Map<String, OnnxTensor> decoderInputs = new HashMap<>();
                decoderInputs.put("input_ids", inputIdsTensor);
                decoderInputs.put("encoder_hidden_states", lastHiddenState);

                try (OrtSession.Result decoderResult = decoderSession.run(decoderInputs)) {
                    float[][][] logits = (float[][][]) decoderResult.get(0).getValue();
                    float[] lastTokenLogits = logits[0][logits[0].length - 1];
                    long nextToken = argmax(lastTokenLogits);

                    if (nextToken == endOfTextToken) {
                        break;
                    }

                    currentTokens.add(nextToken);
                    System.out.println(
                            "TOKEN: "
                                    + nextToken
                                    + " -> "
                                    + vocab.get(nextToken)
                    );
                } finally {
                    inputIdsTensor.close();
                }
            }

            String recognizedText = decodeTokensToText(currentTokens);
            return new TranscriptResult(new ArrayList<>(), recognizedText);

        } catch (Exception e) {
            System.err.println("[ONNX Pipeline Error] Помилка конвеєра Whisper:");
            e.printStackTrace();
            throw new RuntimeException("Помилка під час виконання інференсу Whisper ONNX: " + e.getMessage(), e);
        } finally {
            if (inputFeaturesTensor != null) inputFeaturesTensor.close();
            if (encoderResult != null) encoderResult.close();
        }
    }

    private long argmax(float[] logits) {
        int maxIdx = 0;
        float maxVal = logits[0];
        for (int i = 1; i < logits.length; i++) {
            if (logits[i] > maxVal) {
                maxVal = logits[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private String decodeTokensToText(List<Long> tokens) {

        ByteArrayOutputStream byteStream =
                new ByteArrayOutputStream();

        for (Long token : tokens) {

            // skip special tokens
            if (token >= 50257L) {
                continue;
            }

            String tokenStr = vocab.get(token);

            if (tokenStr == null) {
                continue;
            }

            for (int i = 0; i < tokenStr.length(); i++) {

                char c = tokenStr.charAt(i);

                if (byteEncoder.containsKey(c)) {

                    byteStream.write(
                            byteEncoder.get(c)
                    );

                } else {

                    byte[] fallback =
                            String.valueOf(c)
                                    .getBytes(StandardCharsets.UTF_8);

                    byteStream.write(
                            fallback,
                            0,
                            fallback.length
                    );
                }
            }
        }

        String result =
                new String(
                        byteStream.toByteArray(),
                        StandardCharsets.UTF_8
                );

        return result.isBlank()
                ? "Текст не розпізнано"
                : result.trim();
    }
}