package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.CurrentUserService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutController {

    private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);

    @FXML
    private CheckBox deleteAllDataCheckBox;

    @FXML
    private void confirmLogout() {
        logger.debug("Logging out. Deleting data and settings: {}", deleteAllDataCheckBox.isSelected());

        var clientService = ServiceLocator.getService(TelegramClientService.class);
        var userService = ServiceLocator.getService(CurrentUserService.class);

        userService.setDeleteAllData(deleteAllDataCheckBox.isSelected());

        clientService.sendClientMessage(new TdApi.LogOut(), (res) -> {
        });
    }
}
