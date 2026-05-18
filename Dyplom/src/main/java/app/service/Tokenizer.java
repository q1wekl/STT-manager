package app.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Tokenizer {

    private static final Map<Long, String> vocab = new HashMap<>();
    private static boolean isLoaded = false;
    public static synchronized void loadVocabulary() {
        if (isLoaded) return;

        try {
            InputStream is = Tokenizer.class.getResourceAsStream("/vocab.json");
            if (is == null) {
                System.err.println("[Tokenizer] Попередження: vocab.json не знайдено! Текст буде в ID токенів.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String json = reader.lines().collect(Collectors.joining());
                json = json.substring(json.indexOf("{") + 1, json.lastIndexOf("}"));
                String[] tokens = json.split(",");

                for (String token : tokens) {
                    String[] parts = token.split(":");
                    if (parts.length == 2) {
                        String word = parts[0].trim().replace("\"", "");
                        long id = Long.parseLong(parts[1].trim());
                        vocab.put(id, word);
                    }
                }
                isLoaded = true;
                System.out.println("[Tokenizer] Словник успішно завантажено. Кількість слів: " + vocab.size());
            }
        } catch (Exception e) {
            System.err.println("Помилка завантаження токенізатора: " + e.getMessage());
        }
    }

    public static String decodeTokens(long[] tokens, String language) {
        if (!isLoaded) {
            loadVocabulary();
        }

        StringBuilder text = new StringBuilder();
        for (long token : tokens) {
            if (token >= 50257) {
                continue;
            }

            if (vocab.containsKey(token)) {
                String word = vocab.get(token);
                word = word.replace("Ġ", " ");
                text.append(word);
            }
        }

        return text.toString().trim().replaceAll("\\s+", " ");
    }
}