package net.zonia3000.ombrachat;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class ClientManager {

    private final MainController mainController;
    private final Settings settings;
    private final Client client;
    private final ChatsLoader chatsLoader;
    private final MessagesLoader messagesLoader;

    private TdApi.AuthorizationState lastAuthorizationState = null;
    private long myId;

    public ClientManager(MainController mainController) {
        this.mainController = mainController;
        settings = new Settings();

        Client.setLogMessageHandler(0, new LogMessageHandler());

        // disable TDLib log and redirect fatal errors and plain log messages to a file
        try {
            Client.execute(new TdApi.SetLogVerbosityLevel(0));
            Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false)));
        } catch (Client.ExecutionException error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        // create client
        client = Client.create(new UpdateHandler(), null, null);
        chatsLoader = new ChatsLoader(client);
        messagesLoader = new MessagesLoader(client);
    }

    private class UpdateHandler implements Client.ResultHandler {

        @Override
        public void onResult(TdApi.Object object) {
            if (chatsLoader.onResult(object)) {
                return;
            }
            if (messagesLoader.onResult(object)) {
                return;
            }
            if (object instanceof TdApi.UpdateAuthorizationState update) {
                onAuthorizationStateUpdated(update.authorizationState);
            } else if (object instanceof TdApi.UpdateOption option) {
                if (option.name.equals("my_id")) {
                    myId = ((TdApi.OptionValueInteger) option.value).value;
                }
            }
        }
    }

    private void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
        if (authorizationState != null) {
            lastAuthorizationState = authorizationState;
        }
        switch (lastAuthorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
                request.databaseDirectory = "tdlib";
                request.useMessageDatabase = true;
                request.useSecretChats = false;
                request.apiId = settings.getApiId();
                request.apiHash = settings.getApiHash();
                request.systemLanguageCode = "en";
                request.deviceModel = "Desktop";
                request.applicationVersion = "1.0";

                client.send(request, new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR:
                mainController.showPhoneNumberDialog(phoneNumber -> {
                    client.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), new AuthorizationRequestHandler());
                });
                break;
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                mainController.showAuthenticationCodeDialog(code -> {
                    client.send(new TdApi.CheckAuthenticationCode(code), new AuthorizationRequestHandler());
                });
                break;
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                mainController.showAuthenticationPasswordDialog(password -> {
                    client.send(new TdApi.CheckAuthenticationPassword(password), new AuthorizationRequestHandler());
                });
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR: {
                chatsLoader.loadChats();
                mainController.showMainWindow(chatsLoader, messagesLoader, myId);
                break;
            }
            default:
                System.out.println(lastAuthorizationState.toString());
                break;
        }
    }

    private class AuthorizationRequestHandler implements Client.ResultHandler {

        @Override
        public void onResult(TdApi.Object object) {
            if (object instanceof TdApi.Error error) {
                mainController.displayError(error.message);
                onAuthorizationStateUpdated(null); // repeat last action
            } else if (!(object instanceof TdApi.Ok)) {
                System.err.println("Receive wrong response from TDLib:\n" + object);
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
            System.err.println(message);
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
