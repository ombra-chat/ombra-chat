use crate::{
    emit,
    model::{ChatFolder, UpdateChatAddedToFolder, UpdateChatRemovedFromFolder},
};

use tdlib::enums::{ChatList, Update};

pub async fn handle_folders_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::ChatFolders(update) => {
            let mut folders = vec![ChatFolder::new(0, String::from("Main"))];
            for folder in &update.chat_folders {
                folders.push(ChatFolder::from(&folder));
            }
            emit(app, "update-chat-folders", folders);
            return true;
        }
        Update::ChatAddedToList(update) => {
            if update.chat_list == ChatList::Main {
                emit(
                    app,
                    "update-chat-added-to-folder",
                    UpdateChatAddedToFolder {
                        chat_id: update.chat_id,
                        folder_id: 0,
                    },
                );
            } else if let tdlib::enums::ChatList::Folder(folder) = &update.chat_list {
                emit(
                    app,
                    "update-chat-added-to-folder",
                    UpdateChatAddedToFolder {
                        chat_id: update.chat_id,
                        folder_id: folder.chat_folder_id,
                    },
                );
            }
            return true;
        }
        Update::ChatRemovedFromList(update) => {
            if update.chat_list == ChatList::Main {
                emit(
                    app,
                    "update-chat-removed-from-folder",
                    UpdateChatRemovedFromFolder {
                        chat_id: update.chat_id,
                        folder_id: 0,
                    },
                );
            } else if let tdlib::enums::ChatList::Folder(folder) = &update.chat_list {
                emit(
                    app,
                    "update-chat-removed-from-folder",
                    UpdateChatRemovedFromFolder {
                        chat_id: update.chat_id,
                        folder_id: folder.chat_folder_id,
                    },
                );
            }
            return true;
        }
        _ => {
            return false;
        }
    }
}
