use crate::state;

#[tauri::command]
pub async fn load_chats<R: tauri::Runtime>(app: tauri::AppHandle<R>) -> Result<(), String> {
    tdlib::functions::load_chats(None, 20, state::get_client_id(&app))
        .await
        .map_err(|e| e.message)
}
