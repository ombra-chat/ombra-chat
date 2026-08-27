use crate::{
    messages::{
        builder::{MessageCleaner, MessagePreparer},
        parser::parse_message,
    },
    model::{Chat, InputMessageContent, InputMessageReplyTo, Message},
    state,
};
use rayon::iter::{IntoParallelRefIterator, ParallelIterator};

#[tauri::command]
pub async fn load_chats<R: tauri::Runtime>(app: tauri::AppHandle<R>) -> Result<(), String> {
    let client_id = state::get_client_id(&app);
    loop {
        if let Err(err) = tdlib::functions::load_chats(None, 20, client_id).await {
            if err.code == 404 {
                return Ok(());
            } else {
                return Err(err.message);
            }
        }
    }
}

#[tauri::command]
pub async fn open_chat<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    last_read_inbox_message_id: i64,
) -> Result<Vec<Message>, String> {
    let client_id = state::get_client_id(&app);

    tdlib::functions::open_chat(chat_id, client_id)
        .await
        .map_err(|e| e.message)?;

    let mut messages: Vec<Message> = vec![];

    if let Some(last_message) =
        get_last_message(&app, chat_id, last_read_inbox_message_id, client_id)
            .await
            .map_err(|e| e.message)?
    {
        let last_message_id = last_message.id;

        let previous_messages =
            tdlib::functions::get_chat_history(chat_id, last_message_id, 0, 20, false, client_id)
                .await
                .map(|messages| match messages {
                    tdlib::enums::Messages::Messages(result) => {
                        return result
                            .messages
                            .par_iter()
                            .flatten()
                            .map(|m| parse_message(&app, m))
                            .collect::<Vec<Message>>();
                    }
                })
                .map_err(|e| e.message)?;

        for message in previous_messages {
            messages.push(message);
        }
        messages.push(last_message);
    }

    Ok(messages)
}

/**
 * Retrieve only the last message; this is done because in some cases tdlib sends only
 * one message in any case at the first load, so it is better to always expect to receive
 * only one message when the chat is opened, in order to handle message loading in a more
 * deterministic way; next messages are requested in chunks of 20 or 10 messages
 */
async fn get_last_message<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    chat_id: i64,
    last_read_inbox_message_id: i64,
    client_id: i32,
) -> Result<Option<Message>, tdlib::types::Error> {
    let tdlib::enums::Messages::Messages(result) =
        tdlib::functions::get_chat_history(chat_id, 0, 0, 1, false, client_id).await?;

    if result.messages.len() != 1 {
        return Ok(None);
    }

    if let Some(message) = &result.messages[0] {
        let last_message = parse_message(&app, message);

        // check if the last message has been written by myself (handle edge case)
        if last_message.sender_user_id == state::get_my_id(app) {
            return Ok(Some(last_message));
        }

        // get the last read message
        let tdlib::enums::Messages::Messages(result) = tdlib::functions::get_chat_history(
            chat_id,
            last_read_inbox_message_id,
            -1,
            1,
            false,
            client_id,
        )
        .await?;

        if result.messages.len() == 0 {
            // this happens when the last read message has been deleted
            return Ok(Some(last_message));
        }

        if result.messages.len() == 1 {
            if let Some(message) = &result.messages[0] {
                let last_message = parse_message(&app, message);
                return Ok(Some(last_message));
            }
        }
    }

    Ok(None)
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
                .par_iter()
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
    let reaction =
        tdlib::enums::ReactionType::Emoji(tdlib::types::ReactionTypeEmoji { emoji: emoji });
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
    let reaction =
        tdlib::enums::ReactionType::Emoji(tdlib::types::ReactionTypeEmoji { emoji: emoji });
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
