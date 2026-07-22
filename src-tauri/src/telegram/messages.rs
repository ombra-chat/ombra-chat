use crate::{
    emit, messages::parser::{get_reactions_from_interaction_info, parse_message}, model::{UpdateMessageReactions, UpdateMessageSendSucceeded},
};

use tdlib::enums::Update;

pub async fn handle_messages_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::NewMessage(update) => {
            emit(app, "update-new-message", parse_message(app, &update.message));
            return true;
        }
        Update::File(value) => {
            emit(app, "update-file", value);
            return true;
        }
        Update::DeleteMessages(value) => {
            emit(app, "update-delete-messages", value);
            return true;
        }
        Update::MessageSendSucceeded(update) => {
            emit(
                app,
                "update-message-send-succeeded",
                UpdateMessageSendSucceeded {
                    message: parse_message(app, &update.message),
                    old_message_id: update.old_message_id,
                },
            );
            return true;
        }
        Update::MessageInteractionInfo(update) => {
            emit(
                app,
                "update-message-reactions",
                UpdateMessageReactions {
                    message_id: update.message_id,
                    chat_id: update.chat_id,
                    reactions: get_reactions_from_interaction_info(&update.interaction_info),
                },
            );
            return true;
        }
        _ => {
            return false;
        }
    }
}
