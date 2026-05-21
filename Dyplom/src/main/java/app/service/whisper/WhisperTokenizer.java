package app.service.whisper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhisperTokenizer {

    public static final int SOT           = 50258;
    public static final int EOT           = 50257;
    public static final int TRANSCRIBE    = 50359;
    public static final int NO_TIMESTAMPS = 50363;

    private static final Map<String, Integer> LANG_TOKENS = Map.of(
            "uk", 50341,
            "en", 50259,
            "de", 50261,
            "fr", 50265
    );

    // id → рядок (інвертований vocab)
    private final Map<Integer, String> idToToken;

    public WhisperTokenizer() {
        this.idToToken = loadVocab();
    }
    
    private Map<Integer, String> loadVocab() {
        try {
            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("vocab.json");

            if (is == null) throw new RuntimeException("vocab.json не знайдено в ресурсах");

            // Читаємо як Map<String, Integer> і інвертуємо
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Integer> tokenToId = mapper.readValue(
                    is, new TypeReference<>() {}
            );

            Map<Integer, String> result = new HashMap<>();
            tokenToId.forEach((token, id) -> result.put(id, token));
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Помилка завантаження vocab.json", e);
        }
    }

    public int getLanguageToken(String lang) {
        return LANG_TOKENS.getOrDefault(lang, LANG_TOKENS.get("en"));
    }

    public List<Integer> buildInitialTokens(String language) {
        return List.of(SOT, getLanguageToken(language), TRANSCRIBE, NO_TIMESTAMPS);
    }

    public int greedyNextToken(float[][][] logits) {

        float[] last = logits[0][0];
        int best = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < last.length; i++) {
            if (last[i] > bestScore) {
                bestScore = last[i];
                best = i;
            }
        }
        return best;
    }

    /**
     * Список токен-id → текст
     * Пропускає спеціальні токени (id >= 50256)
     */
    public String decode(List<Integer> tokenIds) {
        StringBuilder sb = new StringBuilder();
        for (int id : tokenIds) {
            if (id >= 50256) continue; // спеціальні токени пропускаємо
            String token = idToToken.getOrDefault(id, "");
            // Whisper використовує "Ġ" як пробіл на початку слова (BPE)
            sb.append(token.replace("Ġ", " ").replace("▁", " "));
        }
        return sb.toString().trim();
    }
}