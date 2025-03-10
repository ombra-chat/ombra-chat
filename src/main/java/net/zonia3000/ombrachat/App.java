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
        Mediator mediator = new Mediator(this);
        new MainController(mediator, stage);
        new ClientManager(mediator);
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        launch();
    }
}
