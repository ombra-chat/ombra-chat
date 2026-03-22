use crate::emit;

use tdlib::enums::Update;

pub async fn handle_chats_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::NewChat(value) => {
            emit(app, "update-new-chat", value);
            return true;
        }
        Update::ChatReadInbox(value) => {
            emit(app, "update-chat-read-inbox", value);
            return true;
        }
        Update::UnreadChatCount(value) => {
            emit(app, "update-unread-chat-count", value);
            return true;
        }
        Update::ChatPosition(value) => {
            emit(app, "update-chat-position", value);
            return true;
        }
        Update::ChatLastMessage(value) => {
            emit(app, "update-chat-last-message", value);
            return true;
        }
        Update::SecretChat(value) => {
            emit(app, "update-secret-chat", value);
            return true;
        }
        _ => {
            return false;
        }
    }
}
