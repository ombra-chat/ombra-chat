package net.zonia3000.ombrachat;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import javafx.application.Application;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.UserService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.MessagesService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        registerServices(this, stage);
        var guiService = ServiceLocator.getService(GuiService.class);
        var settings = ServiceLocator.getService(SettingsService.class);
        if (settings.isInitialConfigDone()) {
            if (settings.isTdlibDatabaseEncrypted()) {
                guiService.showEncryptionPasswordDialog();
            } else {
                ServiceLocator.getService(TelegramClientService.class).startClient();
            }
        } else {
            guiService.showInitialConfigDialog();
        }
    }

    public static void main(String[] args) {
        configureLogger();
        Security.addProvider(new BouncyCastleProvider());
        launch();
    }

    private static void configureLogger() {
        var logLevel = System.getenv("LOG_LEVEL");
        if (logLevel != null) {
            System.setProperty("org.slf4j.simpleLogger.log.net.zonia3000.ombrachat", logLevel);
        }
    }

    private static void registerServices(App app, Stage stage) {
        ServiceLocator.registerService(SettingsService.class, new SettingsService());
        ServiceLocator.registerService(UserService.class, new UserService());
        ServiceLocator.registerService(GuiService.class, new GuiService(app, stage));
        ServiceLocator.registerService(TelegramClientService.class, new TelegramClientService());
        ServiceLocator.registerService(ChatsService.class, new ChatsService());
        ServiceLocator.registerService(MessagesService.class, new MessagesService());
    }
}
