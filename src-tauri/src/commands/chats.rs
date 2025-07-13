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

#[tauri::command]
pub async fn send_message<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    message_thread_id: i64,
    reply_to: Option<tdlib::enums::InputMessageReplyTo>,
    options: Option<tdlib::types::MessageSendOptions>,
    reply_markup: Option<tdlib::enums::ReplyMarkup>,
    input_message_content: tdlib::enums::InputMessageContent,
) -> Result<tdlib::enums::Message, String> {
    tdlib::functions::send_message(
        chat_id,
        message_thread_id,
        reply_to,
        options,
        reply_markup,
        input_message_content,
        state::get_client_id(&app),
    )
    .await
    .map_err(|e| e.message)
}

#[tauri::command]
pub async fn delete_message<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    message_id: i64,
    revoke: bool,
) -> Result<(), String> {
    tdlib::functions::delete_messages(
        chat_id,
        vec![message_id],
        revoke,
        state::get_client_id(&app),
    )
    .await
    .map_err(|e| e.message)
}
