package app.service.whisper;

import ai.onnxruntime.OnnxTensor;
import java.util.Map;

public record DecodeStepResult(
        OnnxTensor logits,
        Map<String, OnnxTensor> pastKeyValues
) {}