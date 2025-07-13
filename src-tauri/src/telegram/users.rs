use crate::emit;

use tdlib::enums::Update;

pub async fn handle_users_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::User(value) => {
            emit(app, "update-user", value);
            return true;
        }
        _ => {
            return false;
        }
    }
}
