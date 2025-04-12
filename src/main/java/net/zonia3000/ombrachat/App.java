package net.zonia3000.ombrachat;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import javafx.application.Application;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.services.CurrentUserService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.MessagesService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.UsersService;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        registerServices(this, stage);
        var guiService = ServiceLocator.getService(GuiService.class);
        var settings = ServiceLocator.getService(SettingsService.class);
        var gpgService = ServiceLocator.getService(GpgService.class);
        if (settings.isInitialConfigDone()) {
            if (settings.getTdlibDatabaseEncryption() != SettingsService.EncryptionType.NONE || gpgService.hasPrivateKey()) {
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
        ServiceLocator.registerService(CurrentUserService.class, new CurrentUserService());
        ServiceLocator.registerService(GuiService.class, new GuiService(app, stage));
        ServiceLocator.registerService(TelegramClientService.class, new TelegramClientService());
        ServiceLocator.registerService(ChatsService.class, new ChatsService());
        ServiceLocator.registerService(UsersService.class, new UsersService());
        ServiceLocator.registerService(MessagesService.class, new MessagesService());
        ServiceLocator.registerService(GpgService.class, new GpgService());
    }
}
