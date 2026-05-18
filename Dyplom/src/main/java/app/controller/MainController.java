package app.controller;

import app.concurent.AppExecutors;
import app.concurent.UiExecutor;
import app.model.TranscriptResult;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
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
    private void speechToText(){
        if(filePath==null){
            resultText.setText("Файл не обрано");
            return;
        }

        resultText.setText("Додано в чергу...");
        File input =new File(filePath);
        CompletableFuture<TranscriptResult> future= AppExecutors.getInstance().supplySpeechToText(input); {
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