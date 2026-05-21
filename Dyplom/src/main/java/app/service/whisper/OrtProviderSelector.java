package app.service.whisper;

import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class OrtProviderSelector {

    public static OrtSession.SessionOptions buildOptions() throws OrtException {
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();

        if (isDirectMlAvailable()) {
            opts.addDirectML(1);
            System.out.println("[ORT] Провайдер: DirectML (GPU)");
        } else {
            System.out.println("[ORT] Провайдер: CPU");
        }
        opts.setExecutionMode(
                OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL
        );

        opts.setMemoryPatternOptimization(false);

        opts.setOptimizationLevel(
                OrtSession.SessionOptions.OptLevel.ALL_OPT
        );

        return opts;
    }

    private static boolean isDirectMlAvailable() {
        try {
            OrtSession.SessionOptions test = new OrtSession.SessionOptions();
            test.addDirectML(1);
            test.close();
            return true;
        } catch (OrtException e) {
            System.out.println("[ORT] DirectML недоступний: " + e.getMessage()); // ← додай
            return false;
        }
    }
}
