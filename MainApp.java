import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        new GUI(stage).launch();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
