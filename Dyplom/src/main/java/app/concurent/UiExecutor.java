package app.concurent;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UiExecutor {
    private static final ExecutorService fxExecutor = Executors.newCachedThreadPool(runnable->{
        Thread thread= new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });
    public static void run(Task<?> task) {
        fxExecutor.submit(task);
    }
}
