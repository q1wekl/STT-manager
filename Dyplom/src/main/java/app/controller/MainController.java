package app.controller;

import app.concurent.AppExecutors;
import app.concurent.UiExecutor;
import app.model.TranscriptResult;

import app.model.TranscriptionConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.CompletableFuture;

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
    private void speechToText(){
        if(filePath==null){
            resultText.setText("Файл не обрано");
            return;
        }
        resultText.setText("Додано в чергу...");
        File input =new File(filePath);

        String selectedModel = modelTypeComboBox.getValue();
        String actualModelPath = "models/whisper/ggml-small-q5_1.bin";
        if (selectedModel.contains("Tiny")) {
            actualModelPath = "models/whisper/ggml-tiny.bin";
        } else if (selectedModel.contains("Base")) {
            actualModelPath = "models/whisper/ggml-base-q5_1.bin";
        } else if (selectedModel.contains("Small")) {
            actualModelPath = "models/whisper/ggml-small-q5_1.bin";
        } else if (selectedModel.contains("Medium")) {
            actualModelPath = "models/whisper/ggml-medium.bin";
        }
        int optimalThreads = 4;
        String language = "uk";
        TranscriptionConfig config= new TranscriptionConfig(
            actualModelPath,
            optimalThreads,
            language,
            false
        );

        CompletableFuture<TranscriptResult> future= AppExecutors.getInstance().supplySpeechToText(input,config); {
            Task<TranscriptResult> fxTask = new Task<>(){
                @Override
                protected TranscriptResult call() throws Exception {
                    return future.get();
                }
            };

            fxTask.setOnRunning(e -> resultText.setText("Обробка файлу сервісом Whisper..."));
            fxTask.setOnSucceeded(e -> resultText.setText(fxTask.getValue().getFullText()));
            fxTask.setOnFailed(e -> resultText.setText("Помилка: " + fxTask.getException().getMessage()));
            UiExecutor.run(fxTask);

        };




    }


    @FXML
    private void handleOpenFile(){
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
            resultText.setText("Обрано файл: "+ file.getAbsolutePath());

        }
    }
}