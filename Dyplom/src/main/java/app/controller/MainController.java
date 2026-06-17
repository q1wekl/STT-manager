package app.controller;

import app.concurent.AppExecutors;
import app.model.TranscriptResult;
import app.model.TranscriptSegment;
import app.model.TranscriptionConfig;
import app.service.FFmpegConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    @FXML private TextArea resultText;

    @FXML private ComboBox<String> accelerationComboBox;
    @FXML private ComboBox<String> modelTypeComboBox;
    @FXML private ComboBox<String> languageComboBox;

    @FXML private Button speechToTextButton;
    @FXML private Button saveAsButton;
    @FXML private Button cleanTextButton;

    @FXML private CheckBox removeTagsCB;
    @FXML private CheckBox removeSymbolsCB;
    @FXML private CheckBox removeSpacesCB;
    @FXML private CheckBox addTimelapsCB;

    @FXML private Label statusLabel;
    @FXML private ProgressBar chunkProgressBar;
    @FXML private Label chunkCountLabel;


    private TranscriptResult lastResult;

    @FXML
    public void initialize() {
        resultText.setEditable(false);
        ObservableList<String> models = FXCollections.observableArrayList(MODEL_MAP.keySet().stream().sorted().toList());
        modelTypeComboBox.setItems(models);


        ObservableList<String> languages = FXCollections.observableArrayList(LANGUAGE_MAP.keySet());
        languageComboBox.setItems(languages);

        ObservableList<String> accelerations = FXCollections.observableArrayList(ACCELERATION_MAP.keySet());
        accelerationComboBox.setItems(accelerations);
    }

    @FXML
    private void speechToText() {

        if (filePath == null) {
            statusLabel.setText("Файл не обрано");
            return;
        }

        resultText.clear();
        resultText.setEditable(false);

        File input = new File(filePath);

        String modelId = MODEL_MAP.getOrDefault(
                modelTypeComboBox.getValue(),
                "small"
        );

        String language = LANGUAGE_MAP.getOrDefault(
                languageComboBox.getValue(),
                "auto"
        );

        String acceleration = ACCELERATION_MAP.getOrDefault(
                accelerationComboBox.getValue(),
                "auto"
        );

        setUiProcessing(true);

        TranscriptionConfig config = new TranscriptionConfig(acceleration, language, modelId, false);
        AppExecutors.getInstance()
                .supplySpeechToText(input, config, this::updateStatus)
                .thenAccept(result -> javafx.application.Platform.runLater(() -> {
                    lastResult = result;
                    resultText.setText(applyPostProcessing(result.getFullText()));
                    setUiProcessing(false);
                }))
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

    @FXML
    private void handleSaveAs() {
        String text = resultText.getText();
        if (text == null || text.isBlank()) {
            statusLabel.setText("Немає тексту для збереження");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти як");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text file", "*.txt"),
                new FileChooser.ExtensionFilter("SRT subtitles", "*.srt")
        );

        Stage stage = (Stage) resultText.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            String content;
            if (file.getName().endsWith(".srt") && lastResult != null) {
                content = buildSrt(lastResult);
            } else {
                content = text;
            }
            java.nio.file.Files.writeString(file.toPath(), content);
        } catch (Exception e) {
            resultText.setText("Помилка збереження:\n" + e.getMessage());
        }
    }
    @FXML
    private void handleClear() {
        resultText.clear();
        lastResult = null;
        statusLabel.setText("Готово");
        chunkProgressBar.setProgress(0);
        chunkCountLabel.setText("0 / 0");
    }
    @FXML
    private void handleConvertToAudio() {
        if (filePath == null) {
            statusLabel.setText("Файл не обрано");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти аудіо як");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MP3 Audio", "*.mp3")
        );
        fileChooser.setInitialFileName(
                new File(filePath).getName().replaceAll("\\.[^.]+$", "") + ".mp3"
        );

        Stage stage = (Stage) resultText.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile == null) return;

        setUiProcessing(true);
        statusLabel.setText("Конвертація...");
        CompletableFuture.runAsync(() -> {
            try {
                FFmpegConverter ffmpeg = new FFmpegConverter();
                File mp3 = ffmpeg.convertToMp3(new File(filePath));
                java.nio.file.Files.copy(mp3.toPath(), saveFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                mp3.delete();
                javafx.application.Platform.runLater(() -> {
                    resultText.setText("Збережено: " + saveFile.getAbsolutePath());
                    setUiProcessing(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    resultText.setText("Помилка конвертації:\n" + e.getMessage());
                    setUiProcessing(false);
                });
            }
        }, AppExecutors.getInstance().getIoExecutor());
    }

    private void updateStatus(String message, int completed, int total) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText(message);
            chunkProgressBar.setProgress(total > 0 ? (double) completed / total : 0);
            chunkCountLabel.setText(completed + " / " + total);
        });
    }

    private void setUiProcessing(boolean isProcessing) {
        javafx.application.Platform.runLater(() -> {
            speechToTextButton.setDisable(isProcessing);
            modelTypeComboBox.setDisable(isProcessing);
            languageComboBox.setDisable(isProcessing);
            accelerationComboBox.setDisable(isProcessing);
            saveAsButton.setDisable(isProcessing);
            cleanTextButton.setDisable(isProcessing);


            resultText.setEditable(!isProcessing);;
        });
    }
    private String applyPostProcessing(String text) {
        if (text == null) return "";
        if (removeTagsCB.isSelected())
            text = text.replaceAll("\\[.*?\\]", "").trim();
        if (removeSymbolsCB.isSelected())
            text = text.replaceAll("[♪♫]", "").trim();
        if (removeSpacesCB.isSelected())
            text = text.lines()
                    .filter(l -> !l.isBlank())
                    .collect(java.util.stream.Collectors.joining("\n"));

        if (addTimelapsCB.isSelected() && lastResult != null) {
            StringBuilder sb = new StringBuilder();
            for (TranscriptSegment s : lastResult.getSegments()) {
                String line = s.getText().trim();
                if (removeTagsCB.isSelected())
                    line = line.replaceAll("\\[.*?\\]", "").trim();
                if (removeSymbolsCB.isSelected())
                    line = line.replaceAll("[♪♫]", "").trim();
                if (line.isBlank()) continue;
                sb.append("[")
                        .append(formatSrtTime(s.getStartTime()))
                        .append("]-[")
                        .append(formatSrtTime(s.getEndTime()))
                        .append("] ")
                        .append(line)
                        .append("\n");
            }
            return sb.toString().trim();
        }

        return text;
    }

    private String buildSrt(TranscriptResult result) {
        StringBuilder sb = new StringBuilder();
        List<TranscriptSegment> segments = result.getSegments();
        for (int i = 0; i < segments.size(); i++) {
            TranscriptSegment s = segments.get(i);
            sb.append(i + 1).append("\n");
            sb.append(formatSrtTime(s.getStartTime()))
                    .append(" --> ")
                    .append(formatSrtTime(s.getEndTime()))
                    .append("\n");
            sb.append(s.getText().trim()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String formatSrtTime(long ms) {
        long h  = ms / 3_600_000;
        long m  = (ms % 3_600_000) / 60_000;
        long s  = (ms % 60_000) / 1_000;
        long ml = ms % 1_000;
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ml);
    }

}