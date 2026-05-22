package app.controller;

import app.concurent.AppExecutors;
import app.model.TranscriptionConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainController {
    private static final java.util.Map<String, String> MODEL_MAP = java.util.Map.of(
            "Tiny (Швидка)",        "tiny",
            "Base (Баланс)",        "base",
            "Small (Хороша якість)", "small",
            "Medium (Висока якість)","medium"
    );
    private String filePath;
    private String currentModelPath;
    @FXML
    private TextArea resultText;
    @FXML
    private ComboBox<String> modelTypeComboBox;
    @FXML private Button speechToTextButton;

    @FXML
    public void initialize() {
        ObservableList<String> models = FXCollections.observableArrayList(
                MODEL_MAP.keySet().stream().sorted().toList()
        );
        modelTypeComboBox.setItems(models);
        modelTypeComboBox.setValue("Base (Баланс)");
    }

    @FXML
    private void speechToText() {
        if (filePath == null) {
            resultText.setText("Файл не обрано");
            return;
        }

        File input = new File(filePath);
        String modelPrefix = MODEL_MAP.getOrDefault(modelTypeComboBox.getValue(), "small");
        String modelPath = "models/whisper/ggml-" + modelPrefix + ".bin";
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            resultText.setText("Модель не знайдено:\n" + modelPath);
            resultText.setText("Модель не знайдено:\n" + modelPath);
            return;
        }

        setUiProcessing(true);
        resultText.clear();
        TranscriptionConfig config = new TranscriptionConfig(
                modelPath,
                4,
                "en",
                false
        );
        System.out.println("[Controller] Викликаємо supplySpeechToText");
        AppExecutors.getInstance()
                .supplySpeechToText(input, config, this::updateStatus)  // ← передаємо callback
                .thenAccept(result -> {
                    javafx.application.Platform.runLater(() -> {
                        String text = result.getFullText();
                        resultText.setText(
                                (text == null || text.isBlank()) ? "Текст не розпізнано" : text
                        );
                        setUiProcessing(false);
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        // getCause() бо CompletableFuture обгортає в CompletionException
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        resultText.setText("Помилка:\n" + cause.getMessage());
                        setUiProcessing(false);
                    });
                    return null;
                });
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

    private void updateStatus(String message) {
        javafx.application.Platform.runLater(() -> resultText.setText(message));
    }

    private void setUiProcessing(boolean isProcessing) {
        javafx.application.Platform.runLater(() -> {
            speechToTextButton.setDisable(isProcessing);
            modelTypeComboBox.setDisable(isProcessing);
        });
    }


}