package app.service.whisper;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class WhisperEncoder implements AutoCloseable {

    private final OrtSession session;

    public WhisperEncoder(OrtEnvironment env, String modelPath) throws OrtException {
        OrtSession.SessionOptions opts = OrtProviderSelector.buildOptions();
        this.session = env.createSession(modelPath, opts);

        System.out.println("[Encoder] INPUTS:");
        for (var info : session.getInputInfo().entrySet()) {
            System.out.println("  " + info.getKey() + " → " + info.getValue().getInfo());
        }
    }

    public OnnxTensor encode(OrtEnvironment env, float[][][] melFeatures) throws OrtException {
        System.out.println("[Encoder] Input shape: ["
                + melFeatures.length + "]["
                + melFeatures[0].length + "]["
                + melFeatures[0][0].length + "]");

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, melFeatures);
        var inputs = java.util.Map.of("input_features", inputTensor);
        OrtSession.Result result = session.run(inputs);
        return (OnnxTensor) result.get("last_hidden_state").orElseThrow();
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
