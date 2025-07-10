use crate::state;

#[tauri::command]
pub async fn load_chats<R: tauri::Runtime>(app: tauri::AppHandle<R>) -> Result<(), String> {
    tdlib::functions::load_chats(None, 20, state::get_client_id(&app))
        .await
        .map_err(|e| e.message)
}

#[tauri::command]
pub async fn open_chat<R: tauri::Runtime>(app: tauri::AppHandle<R>, id: i64) -> Result<(), String> {
    tdlib::functions::open_chat(id, state::get_client_id(&app))
        .await
        .map_err(|e| e.message)
}

#[tauri::command]
pub async fn close_chat<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    id: i64,
) -> Result<(), String> {
    tdlib::functions::close_chat(id, state::get_client_id(&app))
        .await
        .map_err(|e| e.message)
}

#[tauri::command]
pub async fn get_chat_history<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    from_message_id: i64,
    offset: i32,
    limit: i32,
) -> Result<tdlib::enums::Messages, String> {
    tdlib::functions::get_chat_history(
        chat_id,
        from_message_id,
        offset,
        limit,
        false,
        state::get_client_id(&app),
    )
    .await
    .map_err(|e| e.message)
}
