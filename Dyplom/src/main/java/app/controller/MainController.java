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
import java.util.LinkedHashMap;
import java.util.Map;

public class MainController {
    private static final Map<String, String> MODEL_MAP = new LinkedHashMap<>();
    static {
        MODEL_MAP.put("Tiny (Швидка)", "tiny");
        MODEL_MAP.put("Base (Баланс)", "base");
        MODEL_MAP.put("Small (Хороша якість)", "small");
        MODEL_MAP.put("Medium (Висока якість)", "medium");
        MODEL_MAP.put("Large (Максимальна якість)", "large");

    }
    private static final Map<String, String> ACCELERATION_MAP = new LinkedHashMap<>();
    static {
        ACCELERATION_MAP.put("Auto", "auto");
        ACCELERATION_MAP.put("CPU", "cpu");
        ACCELERATION_MAP.put("GPU (Auto detect)", "auto_gpu");
        ACCELERATION_MAP.put("CUDA", "cuda");
        ACCELERATION_MAP.put("Vulkan", "vulkan");
    }
    private static final Map<String, String> LANGUAGE_MAP = new LinkedHashMap<>();
    static {
        LANGUAGE_MAP.put("Auto", "auto");
        LANGUAGE_MAP.put("Ukrainian", "uk");
        LANGUAGE_MAP.put("English", "en");
        LANGUAGE_MAP.put("Polish", "pl");
        LANGUAGE_MAP.put("German", "de");
        LANGUAGE_MAP.put("French", "fr");
        LANGUAGE_MAP.put("Spanish", "es");
        LANGUAGE_MAP.put("Italian", "it");
        LANGUAGE_MAP.put("Japanese", "ja");
    }

    private String filePath;
    @FXML
    private ComboBox<String> accelerationComboBox;
    @FXML
    private TextArea resultText;
    @FXML
    private ComboBox<String> modelTypeComboBox;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML private Button speechToTextButton;

    @FXML
    public void initialize() {
        ObservableList<String> models = FXCollections.observableArrayList(MODEL_MAP.keySet().stream().sorted().toList());
        modelTypeComboBox.setItems(models);
        modelTypeComboBox.setValue("Base (Баланс)");

        ObservableList<String> languages = FXCollections.observableArrayList(LANGUAGE_MAP.keySet());
        languageComboBox.setItems(languages);
        languageComboBox.setValue("Auto");

        ObservableList<String> accelerations = FXCollections.observableArrayList(ACCELERATION_MAP.keySet());
        accelerationComboBox.setItems(accelerations);
        accelerationComboBox.setValue("Auto");
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
        String language = LANGUAGE_MAP.getOrDefault(languageComboBox.getValue(), "auto");
        String acceleration = ACCELERATION_MAP.getOrDefault(accelerationComboBox.getValue(), "auto");

        int threads;
        switch (modelPrefix) {
            case "tiny":
            case "base":
                threads = 2;
                break;

            case "small":
            case "medium":
                threads = 3;
                break;

            case "large":
                threads = 4;
                break;

            default:
                threads = 3;
        }

        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            resultText.setText("Модель не знайдено:\n" + modelPath);
            resultText.setText("Модель не знайдено:\n" + modelPath);
            return;
        }

        setUiProcessing(true);
        resultText.clear();
        TranscriptionConfig config = new TranscriptionConfig(acceleration,language,threads,modelPath,false);
        AppExecutors.getInstance().supplySpeechToText(input, config, this::updateStatus).thenAccept(result -> {
                    javafx.application.Platform.runLater(() -> {
                        String text = result.getFullText();
                        resultText.setText((text == null || text.isBlank()) ? "Текст не розпізнано" : text);
                        setUiProcessing(false);
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
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
            languageComboBox.setDisable(isProcessing);
        });
    }


}