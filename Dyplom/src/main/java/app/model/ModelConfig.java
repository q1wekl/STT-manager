package app.model;

public class ModelConfig {

    public final String id;
    public final String path;
    public final ChunkingConfig chunking;
    public final int consumerCount;

    private ModelConfig(String id, String path, ChunkingConfig chunking, int consumerCount) {
        this.id = id;
        this.path = path;
        this.chunking = chunking;
        this.consumerCount = consumerCount;
    }

    public static ModelConfig of(String modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("modelId is null");
        }

        return switch (modelId.toLowerCase()) {

            case "tiny" -> new ModelConfig("tiny", "models/whisper/ggml-tiny.bin",
                    new ChunkingConfig(45, 60), 2);

            case "base" -> new ModelConfig("base", "models/whisper/ggml-base.bin",
                    new ChunkingConfig(45, 60), 2);

            case "small" -> new ModelConfig("small", "models/whisper/ggml-small.bin",
                    new ChunkingConfig(35, 50), 2);

            case "medium" -> new ModelConfig("medium", "models/whisper/ggml-medium.bin",
                    new ChunkingConfig(20, 35), 1);

            case "large" -> new ModelConfig("large", "models/whisper/ggml-large.bin",
                    new ChunkingConfig(15, 30), 1);

           default -> throw new IllegalArgumentException("Unknown model: " + modelId);
        };
    }
}