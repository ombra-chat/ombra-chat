package net.zonia3000.ombrachat;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import net.zonia3000.ombrachat.events.AuthenticationCodeSet;
import net.zonia3000.ombrachat.events.AuthenticationPasswordSet;
import net.zonia3000.ombrachat.events.ErrorReceived;
import net.zonia3000.ombrachat.events.LoadChats;
import net.zonia3000.ombrachat.events.MyIdReceived;
import net.zonia3000.ombrachat.events.PhoneNumberSet;
import net.zonia3000.ombrachat.events.SendClientMessage;
import net.zonia3000.ombrachat.events.ShowAuthenticationCodeDialog;
import net.zonia3000.ombrachat.events.ShowAuthenticationPasswordDialog;
import net.zonia3000.ombrachat.events.ShowPhoneNumberDialog;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientManager {

    private static final Logger logger = LoggerFactory.getLogger(ClientManager.class);

    private final Mediator mediator;
    private final ChatsLoader chatsLoader;
    private final MessagesLoader messagesLoader;

    private TdApi.AuthorizationState lastAuthorizationState = null;

    public ClientManager(Mediator mediator) {
        this.mediator = mediator;

        Client.setLogMessageHandler(0, new LogMessageHandler());

        // disable TDLib log and redirect fatal errors and plain log messages to a file
        try {
            Client.execute(new TdApi.SetLogVerbosityLevel(0));
            Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false)));
        } catch (Client.ExecutionException error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        mediator.subscribe(PhoneNumberSet.class, (e) -> {
            mediator.publish(new SendClientMessage(new TdApi.SetAuthenticationPhoneNumber(e.getPhoneNumber(), null), new AuthorizationRequestHandler()));
        });
        mediator.subscribe(AuthenticationCodeSet.class, (e) -> {
            mediator.publish(new SendClientMessage(new TdApi.CheckAuthenticationCode(e.getCode()), new AuthorizationRequestHandler()));
        });
        mediator.subscribe(AuthenticationPasswordSet.class, (e) -> {
            mediator.publish(new SendClientMessage(new TdApi.CheckAuthenticationPassword(e.getPassword()), new AuthorizationRequestHandler()));
        });

        chatsLoader = new ChatsLoader(mediator);
        messagesLoader = new MessagesLoader(mediator);

        // create client
        var client = Client.create(new UpdateHandler(), null, null);
        mediator.subscribe(SendClientMessage.class, (e) -> {
            client.send(e.getFunction(), e.getResultHandler());
        });
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
                switch (option.name) {
                    case "my_id":
                        mediator.publish(new MyIdReceived(((TdApi.OptionValueInteger) option.value).value));
                        break;
                }
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
                var settings = mediator.getSettings();
                TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
                request.databaseDirectory = "tdlib";
                request.useMessageDatabase = true;
                request.useSecretChats = false;
                request.apiId = settings.getApiId();
                request.apiHash = settings.getApiHash();
                request.systemLanguageCode = "en";
                request.deviceModel = "Desktop";
                request.applicationVersion = "1.0";
                mediator.publish(new SendClientMessage(request, new AuthorizationRequestHandler()));
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR:
                mediator.publish(new ShowPhoneNumberDialog());
                break;
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                mediator.publish(new ShowAuthenticationCodeDialog());
                break;
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                mediator.publish(new ShowAuthenticationPasswordDialog());
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR: {
                mediator.publish(new LoadChats());
                break;
            }
            default:
                logger.warn("Unsupported authorization state {}", lastAuthorizationState.toString());
                break;
        }
    }

    private class AuthorizationRequestHandler implements Client.ResultHandler {

        @Override
        public void onResult(TdApi.Object object) {
            if (object instanceof TdApi.Error error) {
                mediator.publish(new ErrorReceived(error.message));
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
