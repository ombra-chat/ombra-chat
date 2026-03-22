use crate::emit;

use tdlib::enums::Update;

pub async fn handle_folders_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::ChatFolders(value) => {
            emit(app, "update-chat-folders", value);
            return true;
        }
        Update::ChatAddedToList(value) => {
            emit(app, "update-chat-added-to-list", value);
            return true;
        }
        Update::ChatRemovedFromList(value) => {
            emit(app, "update-chat-removed-from-list", value);
            return true;
        }
        _ => {
            return false;
        }
    }
}
