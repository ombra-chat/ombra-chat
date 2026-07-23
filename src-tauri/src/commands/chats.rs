use crate::{
    messages::{
        builder::{MessageCleaner, MessagePreparer},
        parser::parse_message,
    },
    model::{Chat, InputMessageContent, InputMessageReplyTo, Message},
    state,
};

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
) -> Result<Vec<Message>, String> {
    tdlib::functions::get_chat_history(
        chat_id,
        from_message_id,
        offset,
        limit,
        false,
        state::get_client_id(&app),
    )
    .await
    .map(|messages| match messages {
        tdlib::enums::Messages::Messages(messages) => {
            return messages
                .messages
                .iter()
                .flatten()
                .map(|m| parse_message(&app, m))
                .collect();
        }
    })
    .map_err(|e| e.message)
}

#[tauri::command]
pub async fn send_message<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    reply_to: Option<InputMessageReplyTo>,
    content: InputMessageContent,
) -> Result<Message, String> {
    let input_message_content = content.prepare_message(&app, chat_id).await?;

    let result = tdlib::functions::send_message(
        chat_id,
        None,
        reply_to.map(|r| r.to_tdlib()),
        None,
        None,
        input_message_content,
        state::get_client_id(&app),
    )
    .await;

    content.cleanup(&app).await;

    result
        .map(|tdlib::enums::Message::Message(m)| parse_message(&app, &m))
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

#[tauri::command]
pub async fn view_message<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    message_id: i64,
) -> Result<(), String> {
    tdlib::functions::view_messages(
        chat_id,
        vec![message_id],
        None,
        false,
        state::get_client_id(&app),
    )
    .await
    .map_err(|e| e.message)
}

#[tauri::command]
pub async fn forward_message<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    from_chat_id: i64,
    message_id: i64,
    send_copy: bool,
) -> Result<Vec<Message>, String> {
    tdlib::functions::forward_messages(
        chat_id,
        None,
        from_chat_id,
        vec![message_id],
        None,
        send_copy,
        false,
        state::get_client_id(&app),
    )
    .await
    .map(|tdlib::enums::Messages::Messages(messages)| {
        return messages
            .messages
            .iter()
            .flatten()
            .map(|m| parse_message(&app, m))
            .collect();
    })
    .map_err(|e| e.message)
}

#[tauri::command]
pub async fn get_replied_message<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    message_id: i64,
) -> Result<Message, String> {
    tdlib::functions::get_replied_message(chat_id, message_id, state::get_client_id(&app))
        .await
        .map(|tdlib::enums::Message::Message(m)| parse_message(&app, &m))
        .map_err(|e| e.message)
}

#[tauri::command]
pub async fn create_new_secret_chat<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    user_id: i64,
) -> Result<Chat, String> {
    tdlib::functions::create_new_secret_chat(user_id, state::get_client_id(&app))
        .await
        .map(|tdlib::enums::Chat::Chat(chat)| Chat::from(&chat))
        .map_err(|e| e.message)
}

#[tauri::command]
pub async fn delete_chat<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
) -> Result<(), String> {
    tdlib::functions::delete_chat(chat_id, state::get_client_id(&app))
        .await
        .map_err(|e| e.message)
}

#[tauri::command]
pub async fn add_message_reaction<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    message_id: i64,
    emoji: String,
) -> Result<(), String> {
    let reaction = tdlib::enums::ReactionType::Emoji(tdlib::types::ReactionTypeEmoji { emoji: emoji });
    tdlib::functions::add_message_reaction(
        chat_id,
        message_id,
        reaction,
        false,
        false,
        state::get_client_id(&app),
    )
    .await
    .map_err(|e| e.message)
}

#[tauri::command]
pub async fn remove_message_reaction<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    message_id: i64,
    emoji: String,
) -> Result<(), String> {
    let reaction = tdlib::enums::ReactionType::Emoji(tdlib::types::ReactionTypeEmoji { emoji: emoji });
    tdlib::functions::remove_message_reaction(
        chat_id,
        message_id,
        reaction,
        state::get_client_id(&app),
    )
    .await
    .map_err(|e| e.message)
}

#[tauri::command]
pub async fn share_public_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
) -> Result<Message, String> {
    let public_key_path =
        crate::crypto::pgp::get_my_public_key_tmp_file(&app).map_err(|e| e.to_string())?;

    let document = tdlib::types::InputMessageDocument {
        document: tdlib::types::InputDocument {
            document: tdlib::enums::InputFile::Local(tdlib::types::InputFileLocal {
                path: public_key_path.to_string(),
            }),
            thumbnail: None,
            disable_content_type_detection: true,
        },
        caption: None,
    };

    let input_message_content = tdlib::enums::InputMessageContent::InputMessageDocument(document);

    tdlib::functions::send_message(
        chat_id,
        None,
        None,
        None,
        None,
        input_message_content,
        state::get_client_id(&app),
    )
    .await
    .map(|tdlib::enums::Message::Message(m)| parse_message(&app, &m))
    .map_err(|e| e.message)
}
