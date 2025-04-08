package net.zonia3000.ombrachat.services;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import net.zonia3000.ombrachat.CryptoUtils;
import net.zonia3000.ombrachat.ServiceLocator;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelegramClientService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramClientService.class);

    private UserService userService;
    private ChatsService chatsService;
    private MessagesService messagesService;
    private GuiService guiService;

    private TdApi.AuthorizationState lastAuthorizationState = null;
    private Client client;
    private UpdateHandler updateHandler;

    public void startClient() {
        userService = ServiceLocator.getService(UserService.class);
        chatsService = ServiceLocator.getService(ChatsService.class);
        messagesService = ServiceLocator.getService(MessagesService.class);
        guiService = ServiceLocator.getService(GuiService.class);

        Client.setLogMessageHandler(0, new LogMessageHandler());

        try {
            // log only tdlib fatal errors
            Client.execute(new TdApi.SetLogVerbosityLevel(0));
            var logLevel = System.getenv("LOG_LEVEL");
            if ("DEBUG".equals(logLevel)) {
                // redirect tdlib debug log messages to a file
                var logFile = System.getenv("TDLIB_LOG_FILE");
                Client.execute(new TdApi.SetLogStream(
                        new TdApi.LogStreamFile(logFile == null ? "tdlib.log" : logFile, 1 << 27, false))
                );
                Client.execute(new TdApi.SetLogVerbosityLevel(4));
            }
        } catch (Client.ExecutionException error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        logger.debug("Starting Telegram client");
        updateHandler = new UpdateHandler();
        client = Client.create(updateHandler, null, null);
    }

    public void sendClientMessage(TdApi.Function function, Client.ResultHandler resultHandler) {
        client.send(function, resultHandler);
    }

    public void sendClientMessage(TdApi.Function function) {
        sendClientMessage(function, updateHandler);
    }

    public void setPhoneNumber(String phoneNumber) {
        logger.debug("Setting phone number");
        sendClientMessage(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), new AuthorizationRequestHandler());
    }

    public void setAuthenticationCode(String code) {
        logger.debug("Setting authentication code");
        sendClientMessage(new TdApi.CheckAuthenticationCode(code), new AuthorizationRequestHandler());
    }

    public void setAuthenticationPassword(String password) {
        logger.debug("Setting authentication password");
        sendClientMessage(new TdApi.CheckAuthenticationPassword(password), new AuthorizationRequestHandler());
    }

    private class UpdateHandler implements Client.ResultHandler {

        @Override
        public void onResult(TdApi.Object object) {
            try {
                if (chatsService.onResult(object)) {
                    return;
                }
                if (messagesService.onResult(object)) {
                    return;
                }
                if (object instanceof TdApi.UpdateAuthorizationState update) {
                    onAuthorizationStateUpdated(update.authorizationState);
                } else if (object instanceof TdApi.UpdateOption option) {
                    switch (option.name) {
                        case "my_id":
                            userService.setMyId(((TdApi.OptionValueInteger) option.value).value);
                            break;
                    }
                }
            } catch (Exception ex) {
                logger.error("Exception inside onResult", ex);
            }
        }
    }

    private void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
        if (authorizationState != null) {
            logger.debug("Authorization state {}", authorizationState.getClass().getSimpleName());
            lastAuthorizationState = authorizationState;
        }
        switch (lastAuthorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                var settings = ServiceLocator.getService(SettingsService.class);
                TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
                request.databaseDirectory = getTdLibFolderPath();
                request.useMessageDatabase = true;
                request.useSecretChats = false;
                request.apiId = settings.getApiId();
                request.apiHash = settings.getApiHash();
                request.systemLanguageCode = "en";
                request.deviceModel = "Desktop";
                request.applicationVersion = "1.0";
                if (settings.getTdlibDatabaseEncryption() != SettingsService.EncryptionType.NONE) {
                    var password = userService.getEncryptionPassword();
                    var salt = settings.getTdlibEncryptionSalt();
                    request.databaseEncryptionKey = CryptoUtils.generateDerivedKey(password, salt);
                }
                client.send(request, new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR:
                guiService.showPhoneNumberDialog();
                break;
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                guiService.showAuthenticationCodeDialog();
                break;
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                guiService.showAuthenticationPasswordDialog();
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
            case TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR: {
                // ignored states
                break;
            }
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR: {
                client.send(new TdApi.Close(), (res) -> {
                    userService.performCleanupAfterLogout();
                    // Exiting the application. For the moment, restarting the app is the
                    // simplest way to handle the logout.
                    logger.info("Quitting after logout...");
                    System.exit(0);
                });
                break;
            }
            default:
                logger.warn("Unsupported authorization state {}", lastAuthorizationState.toString());
                break;
        }
    }

    private String getTdLibFolderPath() {
        var settings = ServiceLocator.getService(SettingsService.class);
        Path dir = Paths.get(settings.getApplicationFolderPath(), "tdlib");
        return dir.toAbsolutePath().toString();
    }

    private class AuthorizationRequestHandler implements Client.ResultHandler {

        @Override
        public void onResult(TdApi.Object object) {
            if (object instanceof TdApi.Error error) {
                guiService.handleError(error.message);
                onAuthorizationStateUpdated(null); // repeat last action
            } else if (!(object instanceof TdApi.Ok)) {
                logger.error("Received wrong response from TDLib {}", object);
            }
        }
    }

    private static class LogMessageHandler implements Client.LogMessageHandler {

        @Override
        public void onLogMessage(int verbosityLevel, String message) {
            if (verbosityLevel == 0) {
                onFatalError(message);
                return;
            }
            logger.error("Received error {}", message);
        }
    }

    private static void onFatalError(String errorMessage) {
        final class ThrowError implements Runnable {

            private final String errorMessage;
            private final AtomicLong errorThrowTime;

            private ThrowError(String errorMessage, AtomicLong errorThrowTime) {
                this.errorMessage = errorMessage;
                this.errorThrowTime = errorThrowTime;
            }

            @Override
            public void run() {
                if (isDatabaseBrokenError(errorMessage) || isDiskFullError(errorMessage) || isDiskError(errorMessage)) {
                    processExternalError();
                    return;
                }

                errorThrowTime.set(System.currentTimeMillis());
                throw new ClientError("TDLib fatal error: " + errorMessage);
            }

            private void processExternalError() {
                errorThrowTime.set(System.currentTimeMillis());
                throw new ExternalClientError("Fatal error: " + errorMessage);
            }

            final class ClientError extends Error {

                private ClientError(String message) {
                    super(message);
                }
            }

            final class ExternalClientError extends Error {

                public ExternalClientError(String message) {
                    super(message);
                }
            }

            private boolean isDatabaseBrokenError(String message) {
                return message.contains("Wrong key or database is corrupted")
                        || message.contains("SQL logic error or missing database")
                        || message.contains("database disk image is malformed")
                        || message.contains("file is encrypted or is not a database")
                        || message.contains("unsupported file format")
                        || message.contains("Database was corrupted and deleted during execution and can't be recreated");
            }

            private boolean isDiskFullError(String message) {
                return message.contains("PosixError : No space left on device")
                        || message.contains("database or disk is full");
            }

            private boolean isDiskError(String message) {
                return message.contains("I/O error") || message.contains("Structure needs cleaning");
            }
        }

        final AtomicLong errorThrowTime = new AtomicLong(Long.MAX_VALUE);
        new Thread(new ThrowError(errorMessage, errorThrowTime), "TDLib fatal error thread").start();

        // wait at least 10 seconds after the error is thrown
        while (errorThrowTime.get() >= System.currentTimeMillis() - 10000) {
            try {
                Thread.sleep(1000 /* milliseconds */);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
