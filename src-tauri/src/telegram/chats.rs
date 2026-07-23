use crate::{
    emit,
    model::{
        get_chat_position, Chat, ChatPosition, RemoveChatFromFolder, SecretChat,
        UpdateChatReadInbox,
    },
};

use tdlib::enums::{ChatList, Update};

pub async fn handle_chats_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::NewChat(update) => {
            emit(app, "update-new-chat", Chat::from(&update.chat));
            return true;
        }
        Update::ChatReadInbox(update) => {
            emit(
                app,
                "update-chat-read-inbox",
                UpdateChatReadInbox {
                    chat_id: update.chat_id,
                    last_read_inbox_message_id: update.last_read_inbox_message_id,
                    unread_count: update.unread_count,
                },
            );
            return true;
        }
        Update::UnreadChatCount(update) => {
            if update.chat_list == ChatList::Main {
                emit(app, "update-unread-chat-count", update.unread_unmuted_count);
            }
            return true;
        }
        Update::ChatPosition(update) => {
            if update.position.list == ChatList::Main {
                emit(app, "update-chat-position", ChatPosition::from(update));
            } else if let ChatList::Folder(folder) = &update.position.list {
                if update.position.order == 0 {
                    emit(
                        app,
                        "remove-chat-from-folder",
                        RemoveChatFromFolder {
                            folder_id: folder.chat_folder_id,
                            chat_id: update.chat_id,
                        },
                    );
                }
            }
            return true;
        }
        Update::ChatLastMessage(update) => {
            if let Some(pos) = get_chat_position(&update.positions) {
                emit(
                    app,
                    "update-chat-position",
                    ChatPosition::new(update.chat_id, pos),
                );
            }
            return true;
        }
        Update::SecretChat(update) => {
            emit(app, "update-secret-chat", SecretChat::from(&update));
            return true;
        }
        _ => {
            return false;
        }
    }
}
