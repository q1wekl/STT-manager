package app.service.whisper;

import app.model.TranscriptResult;
import app.model.TranscriptSegment;
import app.model.TranscriptionConfig;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperContextParams;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WhisperCppEngine implements SpeechToTextEngine {
    private final WhisperJNI whisper;
    private WhisperContext context;
    private final String backendMode;

    public WhisperCppEngine(String modelPath, String backendMode) {
        this.backendMode = resolveBackendMode(backendMode);;

        try {
            Path baseNativeDir = Paths.get("native").toAbsolutePath();
            Path selectedDir = baseNativeDir.resolve(this.backendMode);

            loadAbsolute(selectedDir.resolve("vcruntime140.dll"));
            loadAbsolute(selectedDir.resolve("msvcp140.dll"));

            switch (this.backendMode) {
                case "cuda":
                    loadAbsolute(selectedDir.resolve("cudart64_13.dll"));
                    loadAbsolute(selectedDir.resolve("ggml.dll"));
                    loadAbsolute(selectedDir.resolve("whisper.dll"));
                    break;

                case "vulkan":
                    loadAbsolute(selectedDir.resolve("vulkan-1.dll"));
                    loadAbsolute(selectedDir.resolve("SPIRV-Tools-shared.dll"));
                    loadAbsolute(selectedDir.resolve("glslangd.dll"));
                    loadAbsolute(selectedDir.resolve("ggml.dll"));
                    loadAbsolute(selectedDir.resolve("whisper.dll"));
                    break;

                case "cpu":
                case "auto":
                default:
                    break;
            }

            loadAbsolute(selectedDir.resolve("whisper-jni.dll"));

            this.whisper = new WhisperJNI();
            Path absoluteModelPath = Path.of(modelPath).toAbsolutePath();

            WhisperContextParams contextParams = new WhisperContextParams();
            contextParams.useGPU = this.backendMode.equals("cuda") || this.backendMode.equals("vulkan");
            this.context = whisper.init(absoluteModelPath, contextParams);

            if (this.context == null) {
                throw new RuntimeException("Context init failed");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static String resolveBackendMode(String mode) {

        if (mode == null) {
            return "cpu";
        }

        switch (mode.toLowerCase()) {

            case "auto":
                return "cpu";
            case "auto_gpu":
                if (java.nio.file.Files.exists(java.nio.file.Paths.get("native", "cuda"))) {
                    return "cuda";
                }
                if (java.nio.file.Files.exists(java.nio.file.Paths.get("native", "vulkan"))) {
                    return "vulkan";
                }
                return "cpu";
            default: return mode.toLowerCase();
        }
    }

    private static void loadAbsolute(Path path) {
        if (path.toFile().exists()) {
            try {
                Runtime.getRuntime().load(path.toString());
                System.out.println("  [OK] Loaded: " + path.getFileName() + " | size: " + path.toFile().length() / 1024 + " KB");
            } catch (Throwable t) {
                System.out.println("  [FAIL] " + path.getFileName() + ": " + t.getMessage());
            }
        } else {
            System.err.println("  [MISSING] " + path.toAbsolutePath());
        }
    }
    @Override
    public TranscriptResult transcribe(float[] audio, TranscriptionConfig config){
        if (context == null) {
            throw new IllegalStateException("Whisper context is closed");
        }
        List<TranscriptSegment> segmentsList = new ArrayList<>();
        WhisperFullParams params = new WhisperFullParams();

        params.nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        if ("auto".equalsIgnoreCase(config.getLanguage())) {
            params.language = null;
        } else {
            params.language = config.getLanguage();
        }

        int result = whisper.full(context, params, audio, audio.length);
        if (result != 0) {
            throw new RuntimeException("Whisper failed" + result);
        }

        StringBuilder fullText = new StringBuilder();
        int segmentsCount = whisper.fullNSegments(context);
        for (int i = 0; i < segmentsCount; i++) {
            String text = whisper.fullGetSegmentText(context, i);
            long t0 = whisper.fullGetSegmentTimestamp0(context, i);
            long t1 = whisper.fullGetSegmentTimestamp1(context, i);
            segmentsList.add(new TranscriptSegment(text, t0, t1));
            fullText.append(text).append("\n");
        }
        return new TranscriptResult(segmentsList, fullText.toString().trim(), -1);
    }

    @Override
    public void close() {
        if (context != null) {
            whisper.free(context);
            context = null;
        }
    }
}
