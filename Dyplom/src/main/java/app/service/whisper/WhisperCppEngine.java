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
    private static final WhisperJNI whisper;
    private WhisperContext context;
    // modes: "cpu", "vulkan", "cuda"
    private static final String backend_mode = "cuda";
    static {
        try {
            Path baseNativeDir = Paths.get("native").toAbsolutePath();
            Path selectedDir = baseNativeDir.resolve(backend_mode.toLowerCase());
            loadAbsolute(selectedDir.resolve("vcruntime140.dll"));
            loadAbsolute(selectedDir.resolve("msvcp140.dll"));

            switch (backend_mode.toLowerCase()) {
                case "cuda":
                    System.out.println("[WhisperCpp] Linking NVIDIA CUDA Environment...");
                    loadAbsolute(selectedDir.resolve("cudart64_13.dll"));
                    loadAbsolute(selectedDir.resolve("ggml.dll"));
                    loadAbsolute(selectedDir.resolve("whisper.dll")); // ← додай це
                    break;
                case "vulkan":
                    System.out.println("[WhisperCpp] Linking Vulkan Compute Shaders...");
                    loadAbsolute(selectedDir.resolve("vulkan-1.dll"));
                    loadAbsolute(selectedDir.resolve("SPIRV-Tools-shared.dll"));
                    loadAbsolute(selectedDir.resolve("glslangd.dll"));
                    loadAbsolute(selectedDir.resolve("ggml.dll"));
                    loadAbsolute(selectedDir.resolve("whisper.dll")); // ← і тут
                    break;
                case "cpu":
                default:
                    System.out.println("[WhisperCpp] Linking Clean CPU Architecture...");
                    break;
            }
            loadAbsolute(selectedDir.resolve("whisper-jni.dll"));

            whisper = new WhisperJNI();
            System.out.println("[WhisperCpp] JNI Bridge successfully linked with whisper-jni.dll!");
        } catch (Exception e) {
            throw new RuntimeException("Error loading native libraries: " + e.getMessage(), e);
        }
    }

    public WhisperCppEngine(String modelPath) {
        try {
            Path absoluteModelPath = Path.of(modelPath).toAbsolutePath();
            System.out.println("[WhisperCpp] Booting core context with model: " + absoluteModelPath);

            java.io.File modelFile = absoluteModelPath.toFile();
            if (!modelFile.exists()) {
                System.err.println("[WhisperCpp] CRITICAL ERROR: Model file NOT FOUND at: " + absoluteModelPath);
                throw new java.io.FileNotFoundException("Model file missing!");
            } else {
                System.out.println("[WhisperCpp] Model file found. Size: " + (modelFile.length() / 1024 / 1024) + " MB");
            }

            WhisperContextParams contextParams = new WhisperContextParams();
            contextParams.useGPU = !backend_mode.equalsIgnoreCase("cpu");

            this.context = whisper.init(absoluteModelPath, contextParams); // Path + params ✓
            System.out.println("CUDA enabled: " + contextParams.useGPU);
            if (this.context == null) {
                throw new RuntimeException("C++ engine returned NULL context pointer. Model might be corrupted.");
            }

            System.out.println("[WhisperCpp] Success! Whisper engine context is alive and ready for chunks.");
        } catch (Exception e) {
            throw new RuntimeException("Failure initializing Whisper context: " + e.getMessage(), e);
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
        List<TranscriptSegment> segmentsList= new ArrayList<>();
        WhisperFullParams params = new WhisperFullParams();

        params.nThreads = 1;
        params.language = config.getLanguage();


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
        return new TranscriptResult(segmentsList, fullText.toString().trim());
    }

    @Override
    public void close() {
        if (context != null) {
            whisper.free(context);
            context = null;
        }
    }
}
