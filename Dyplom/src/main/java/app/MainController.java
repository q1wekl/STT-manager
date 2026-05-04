package app;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import java.io.File;

public class MainController {
@FXML
private Button fileSelectionButton;
    @FXML
    public void handleClick() {
        System.out.println("Click!");
    }
    @FXML
    private void handleOpenFile(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Оберіть необхідний файл");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Audio Files","*.mp3","*.wav"),
            new FileChooser.ExtensionFilter("Video Files", "*.mp4"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fileChooser.showOpenDialog(
                fileSelectionButton.getScene().getWindow()
        );
        if(file !=null){
            System.out.println("Обраний файл: "+ file.getAbsolutePath());
        }
    }
}