package app;

import app.concurent.AppExecutors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application{


    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/xml/main-view.fxml")
        );

        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
       // stage.setMaximized(true);

        stage.setOnCloseRequest(event -> {
            System.out.println("Application closing...");
            AppExecutors.getInstance().shutdown();
            Platform.exit();
            System.exit(0);
        });

        stage.setScene(scene);
        stage.show();
    }
    @Override
    public void stop() {
        System.out.println("JavaFX stop()");
        AppExecutors.getInstance().shutdown();
    }

    public static void main(String[] args) throws IOException {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        launch();
    }
}
