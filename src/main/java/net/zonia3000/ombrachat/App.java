package net.zonia3000.ombrachat;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        var mainController = new MainController(stage);
        var clientManager = new ClientManager(mainController);
    }

    public static void main(String[] args) {
        launch();
    }
}
