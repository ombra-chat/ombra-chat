package net.zonia3000.ombrachat.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javafx.application.Platform;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.controllers.ChatPageController;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersService {

    private static final Logger logger = LoggerFactory.getLogger(UsersService.class);

    private final ConcurrentMap<Long, TdApi.User> users = new ConcurrentHashMap<>();
    private final GuiService guiService;
    private final CurrentUserService currentUserService;

    public UsersService() {
        guiService = ServiceLocator.getService(GuiService.class);
        currentUserService = ServiceLocator.getService(CurrentUserService.class);
    }

    public boolean onResult(TdApi.Object object) {
        if (object instanceof TdApi.UpdateUser update) {
            return handleUpdateUser(update);
        }
        return false;
    }

    public TdApi.User getUser(long userId) {
        return users.get(userId);
    }

    private boolean handleUpdateUser(TdApi.UpdateUser update) {
        if (update.user.id == currentUserService.getMyId()) {
            currentUserService.setMyUser(update.user);
        }
        users.put(update.user.id, update.user);
        Platform.runLater(() -> {
            var chatPageController = guiService.getController(ChatPageController.class);
            if (chatPageController == null) {
                return;
            }
            chatPageController.updateUserOnMessages(update.user);
        });
        return true;
    }

    public String getUserDisplayText(long userId) {
        var user = getUser(userId);
        if (user == null) {
            return null;
        }
        if (user.usernames != null) {
            return user.usernames.editableUsername;
        } else if (user.firstName != null && user.lastName != null) {
            return user.firstName + " " + user.lastName;
        } else if (user.phoneNumber != null) {
            return user.phoneNumber;
        } else {
            return String.valueOf(user.id);
        }
    }
}
