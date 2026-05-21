package app.service.whisper;

import ai.onnxruntime.*;

import java.util.HashMap;
import java.util.Map;

public class WhisperDecoder implements AutoCloseable {

    private final OrtSession session;

    public WhisperDecoder(OrtEnvironment env, String modelPath) throws OrtException {
        OrtSession.SessionOptions opts = OrtProviderSelector.buildOptions();
        this.session = env.createSession(modelPath, opts);

        // Друкуємо реальні імена inputs і outputs
        System.out.println("[Decoder] INPUTS:");
        for (var info : session.getInputInfo().entrySet()) {
            System.out.println("  " + info.getKey() + " → " + info.getValue().getInfo());
        }
        System.out.println("[Decoder] OUTPUTS:");
        for (var info : session.getOutputInfo().entrySet()) {
            System.out.println("  " + info.getKey());
        }
    }

    /**
     * Один крок декодування (merged модель з KV-cache)
     *
     * @param env            OrtEnvironment
     * @param inputIds       int[1][1] — поточний токен
     * @param encoderHidden  OnnxTensor — вихід енкодера
     * @param pastKeyValues  попередній KV-cache (null для першого кроку)
     * @return DecodeStepResult — логіти + новий KV-cache
     */
    public DecodeStepResult decode(
            OrtEnvironment env,
            long[][] inputIds,
            OnnxTensor encoderHidden,
            Map<String, OnnxTensor> pastKeyValues
    ) throws OrtException {

        Map<String, OnnxTensorLike> inputs = new HashMap<>();
        inputs.put("input_ids", OnnxTensor.createTensor(env, inputIds));
        inputs.put("encoder_hidden_states", encoderHidden);

        boolean isFirstStep = (pastKeyValues == null || pastKeyValues.isEmpty());

        // Нова merged модель ВИМАГАЄ use_cache_branch
        inputs.put("use_cache_branch",
                OnnxTensor.createTensor(env, new boolean[]{!isFirstStep}));

        if (!isFirstStep) {
            inputs.putAll(pastKeyValues);
        }
        if (isFirstStep) {
            System.out.println("[Decoder] First step inputs: " + inputs.keySet());
        }
        OrtSession.Result result = session.run(inputs);
        OnnxTensor logits = (OnnxTensor) result.get("logits").orElseThrow();

        Map<String, OnnxTensor> newKV = new HashMap<>();
        for (var entry : result) {
            if (entry.getKey().startsWith("present.")) {
                newKV.put(
                        entry.getKey().replace("present.", "past_key_values."),
                        (OnnxTensor) entry.getValue()
                );
            }
        }

        return new DecodeStepResult(logits, newKV);
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
