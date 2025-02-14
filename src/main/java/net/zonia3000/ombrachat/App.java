package net.zonia3000.ombrachat;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        var settings = new Settings();
        var mainController = new MainController(stage, settings);
        var clientManager = new ClientManager(mainController, settings);
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        launch();
    }
}
