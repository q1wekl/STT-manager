package app.controller;

import app.concurent.AppExecutors;
import app.model.TranscriptionConfig;
import app.service.FFmpegConverter;
import app.service.OnnxWhisperService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainController {
    private String filePath;
    @FXML
    private TextArea resultText;
    @FXML
    private ComboBox<String> modelTypeComboBox;

    @FXML
    public void initialize() {
        ObservableList<String> models = FXCollections.observableArrayList(
                "Tiny (Швидка)",
                "Base (Баланс)",
                "Small (Хороша якість)",
                "Medium (Висока якість)"
        );
        modelTypeComboBox.setItems(models);
        modelTypeComboBox.setValue("Small (Хороша якість)");
    }

    @FXML
    private void speechToText() {

        if (filePath == null) {
            resultText.setText("Файл не обрано");
            return;
        }

        resultText.setText("Підготовка аудіо...");

        File input = new File(filePath);

        String selectedModel = modelTypeComboBox.getValue();
        String modelPrefix = "small";

        if (selectedModel.contains("Tiny")) {
            modelPrefix = "tiny";
        } else if (selectedModel.contains("Base")) {
            modelPrefix = "base";
        } else if (selectedModel.contains("Medium")) {
            modelPrefix = "medium";
        }

        String modelBasePath =
                "models/whisper/whisper-" + modelPrefix;

        new Thread(() -> {

            try {

                javafx.application.Platform.runLater(() ->
                        resultText.setText("Конвертація через FFmpeg...")
                );

                // FFmpeg conversion
                FFmpegConverter converter =
                        new FFmpegConverter();

                File wavFile =
                        converter.convertToWav(input);

                javafx.application.Platform.runLater(() ->
                        resultText.setText("Зчитування WAV...")
                );

                // WAV -> float[]
                float[] audio =
                        app.utils.AudioUtils.loadAudio(
                                wavFile.getAbsolutePath()
                        );

                javafx.application.Platform.runLater(() ->
                        resultText.setText("Завантаження ONNX моделей...")
                );

                // Whisper service
                OnnxWhisperService whisperService =
                        new OnnxWhisperService(modelBasePath);

                TranscriptionConfig config =
                        new TranscriptionConfig(
                                modelBasePath,
                                4,
                                "uk",
                                false
                        );

                javafx.application.Platform.runLater(() ->
                        resultText.setText("Розпізнавання мовлення...")
                );

                // Inference
                var result =
                        whisperService.transcribe(
                                audio,
                                config
                        );

                javafx.application.Platform.runLater(() -> {

                    String text = result.getFullText();

                    if (text == null || text.isBlank()) {
                        resultText.setText(
                                "Текст не розпізнано"
                        );
                    } else {
                        resultText.setText(text);
                    }
                });

            } catch (Exception ex) {

                ex.printStackTrace();

                javafx.application.Platform.runLater(() -> {

                    resultText.setText(
                            "Помилка:\n" + ex.getMessage()
                    );
                });
            }

        }).start();
    }

    @FXML
    private void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Оберіть необхідний файл");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files","*.mp3","*.wav","*.m4a","*.flac"),
                new FileChooser.ExtensionFilter("Video Files", "*.mp4","*.mkv","*.avi"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        Stage stage = (Stage) resultText.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            filePath = file.getAbsolutePath();
            resultText.setText("Обрано файл: " + file.getAbsolutePath());
        }
    }
}