use crate::emit;

use tdlib::enums::Update;

pub async fn handle_effects_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::AvailableMessageEffects(value) => {
            emit(app, "update-available-message-effects", value);
            return true;
        }
        _ => {
            return false;
        }
    }
}
