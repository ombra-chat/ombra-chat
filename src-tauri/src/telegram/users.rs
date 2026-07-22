use crate::{emit, model::User};

use tdlib::enums::Update;

pub async fn handle_users_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::User(update) => {
            emit(app, "update-user", User::from(update));
            return true;
        }
        _ => {
            return false;
        }
    }
}
