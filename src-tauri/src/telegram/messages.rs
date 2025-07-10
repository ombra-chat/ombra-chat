use crate::emit;

use tdlib::enums::Update;

pub async fn handle_messages_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::NewMessage(value) => {
            emit(app, "update-new-message", value);
            return true;
        }
        Update::File(value) => {
            emit(app, "update-file", value);
            return true;
        }
        Update::DeleteMessages(value) => {
            emit(app, "delete-messages", value);
            return true;
        }
        Update::MessageSendSucceeded(value) => {
            emit(app, "message-send-succeeded", value);
            return true;
        }
        Update::MessageInteractionInfo(value) => {
            emit(app, "message-interaction-info", value);
            return true;
        }
        _ => {
            return false;
        }
    }
}
