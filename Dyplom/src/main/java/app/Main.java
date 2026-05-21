package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.classfile.Label;

public class Main extends Application{


    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/xml/main-view.fxml")
        );

        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();


    }

    public static void main(String[] args) throws IOException {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        launch();
    }
}
