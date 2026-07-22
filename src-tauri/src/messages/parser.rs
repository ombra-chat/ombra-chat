use std::path::Path;

use crate::{
    crypto::{self, pgp::get_plaintext_path},
    model::{
        File, Message, MessageAnimatedEmoji, MessageContent, MessageDocument, MessageError,
        MessagePgpFile, MessagePgpText, MessagePhoto, MessageReaction, MessageSendingState,
        MessageText, MessageVoiceNote, PhotoSize,
    },
};

pub fn parse_message<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    message: &tdlib::types::Message,
) -> Message {
    let mut sender_user_id: Option<i64> = None;
    let mut sender_chat_id: Option<i64> = None;

    match &message.sender_id {
        tdlib::enums::MessageSender::User(user) => sender_user_id = Some(user.user_id),
        tdlib::enums::MessageSender::Chat(chat) => sender_chat_id = Some(chat.chat_id),
    }

    let mut is_reply = false;
    let mut reply_quote: Option<String> = None;
    if let Some(tdlib::enums::MessageReplyTo::Message(reply)) = &message.reply_to {
        is_reply = true;
        if let Some(quote) = &reply.quote {
            reply_quote = Some(quote.text.text.clone());
        }
    }

    let mut sending_state: Option<MessageSendingState> = None;
    if let Some(state) = &message.sending_state {
        match state {
            tdlib::enums::MessageSendingState::Pending(_) => {
                sending_state = Some(MessageSendingState::Pending)
            }
            tdlib::enums::MessageSendingState::Failed(_) => {
                sending_state = Some(MessageSendingState::Failed)
            }
        }
    }

    Message {
        id: message.id,
        sender_user_id: sender_user_id,
        sender_chat_id: sender_chat_id,
        chat_id: message.chat_id,
        date: message.date,
        is_reply: is_reply,
        reply_quote: reply_quote,
        content: parse_content(app, &message.content),
        reactions: get_reactions_from_interaction_info(&message.interaction_info),
        sending_state: sending_state,
    }
}

pub fn parse_content<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    content: &tdlib::enums::MessageContent,
) -> MessageContent {
    match content {
        tdlib::enums::MessageContent::MessageText(content) => {
            return MessageContent::Text(MessageText {
                text: content.text.text.clone(),
            });
        }
        tdlib::enums::MessageContent::MessagePhoto(content) => {
            return MessageContent::Photo(MessagePhoto {
                sizes: get_usable_photo_sizes(content),
                caption: content.caption.text.clone(),
            });
        }
        tdlib::enums::MessageContent::MessageDocument(content) => {
            let file_name = &content.document.file_name;
            if file_name.starts_with("ombra-chat-") {
                if file_name.ends_with(".txt.pgp") {
                    return parse_pgp_text_message(app, content);
                } else if file_name.ends_with(".pgp") {
                    return parse_pgp_file_message(app, content);
                }
            }

            return MessageContent::Document(MessageDocument {
                file_name: file_name.clone(),
                document: File::from(&content.document.document),
                mime_type: content.document.mime_type.clone(),
                caption: content.caption.text.clone(),
                downloaded: content.document.document.local.is_downloading_completed,
                downloading: content.document.document.local.is_downloading_active,
            });
        }
        tdlib::enums::MessageContent::MessageAnimatedEmoji(content) => {
            return MessageContent::AnimatedEmoji(MessageAnimatedEmoji {
                emoji: content.emoji.clone(),
            });
        }
        tdlib::enums::MessageContent::MessageVoiceNote(content) => {
            return MessageContent::VoiceNote(MessageVoiceNote {
                caption: content.caption.text.clone(),
                is_listened: content.is_listened,
                duration: content.voice_note.duration,
                voice: File::from(&content.voice_note.voice),
            })
        }
        content => {
            return MessageContent::Unsupported(content.clone());
        }
    };
}

fn parse_pgp_text_message<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    message: &tdlib::types::MessageDocument,
) -> MessageContent {
    let doc = &message.document.document;

    let text: Option<String>;

    if doc.local.is_downloading_completed {
        let p = doc.local.path.clone();
        match crypto::pgp::decrypt_file_to_string(app, &p) {
            Ok(plaintext) => {
                text = Some(plaintext);
            }
            Err(e) => {
                log::warn!("Error decrypting message: {}", e);
                return MessageContent::Error(MessageError {
                    text: String::from("Unable to decrypt message"),
                });
            }
        }
    } else {
        text = None;
    }

    MessageContent::PgpText(MessagePgpText {
        document_id: doc.id,
        text: text,
    })
}

fn parse_pgp_file_message<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    message: &tdlib::types::MessageDocument,
) -> MessageContent {
    let ciphertexts = match serde_json::from_str::<Vec<String>>(&message.caption.text) {
        Ok(v) => v,
        Err(e) => {
            log::warn!("Error parsing JSON from caption: {}", e.to_string());
            return MessageContent::Error(MessageError {
                text: String::from("Unable to parse message"),
            });
        }
    };

    if ciphertexts.len() == 0 || ciphertexts.len() > 2 {
        log::warn!("Unexpected array length: {}", ciphertexts.len());
        return MessageContent::Error(MessageError {
            text: String::from("Unable to parse message"),
        });
    }

    let cipher_file_name = match ciphertexts.get(0) {
        Some(v) => v,
        None => {
            return MessageContent::Error(MessageError {
                text: String::from("Unable to extract file name from message"),
            });
        }
    };
    let file_name = match crypto::pgp::decrypt_string_to_string(&app, cipher_file_name) {
        Ok(v) => v,
        Err(e) => {
            log::warn!("Error decrypting file name: {}", e);
            return MessageContent::Error(MessageError {
                text: String::from("Unable to decrypt file name"),
            });
        }
    };

    let caption = ciphertexts.get(1).and_then(|cipher_caption| {
        crypto::pgp::decrypt_string_to_string(&app, cipher_caption)
            .map(Some)
            .unwrap_or_else(|e| {
                log::warn!("Error decrypting caption: {}", e);
                None
            })
    });

    let doc = &message.document.document;

    let ciphertext_path: Option<String>;
    let plaintext_path: Option<String>;
    if doc.local.is_downloading_completed {
        let path = doc.local.path.clone();
        ciphertext_path = Some(path.clone());
        let target_plaintext_path = get_plaintext_path(&path);
        if Path::new(target_plaintext_path).exists() {
            plaintext_path = Some(target_plaintext_path.into());
        } else {
            plaintext_path = None;
        }
    } else {
        ciphertext_path = None;
        plaintext_path = None;
    }

    MessageContent::PgpFile(MessagePgpFile {
        document_id: doc.id,
        ciphertext_path: ciphertext_path,
        plaintext_path: plaintext_path,
        file_name: file_name,
        caption: caption,
    })
}

fn get_usable_photo_sizes(content: &tdlib::types::MessagePhoto) -> Vec<PhotoSize> {
    return content
        .photo
        .sizes
        .iter()
        .filter(|p| {
            // See https://core.telegram.org/api/files#image-thumbnail-types
            p.r#type != "t"
                && p.r#type != "i"
                && p.r#type != "j"
                && ((p.photo.local.is_downloading_completed && p.photo.local.path != "")
                    || p.photo.local.can_be_downloaded)
        })
        .map(PhotoSize::from)
        .collect();
}

pub fn get_reactions_from_interaction_info(
    info: &Option<tdlib::types::MessageInteractionInfo>,
) -> Vec<MessageReaction> {
    let mut reactions: Vec<MessageReaction> = vec![];
    if let Some(interaction) = info {
        if let Some(message_reactions) = &interaction.reactions {
            for r in &message_reactions.reactions {
                if let tdlib::enums::ReactionType::Emoji(emoji) = &r.r#type {
                    let mut user_id: Option<i64> = None;
                    if let Some(tdlib::enums::MessageSender::User(user)) = &r.used_sender_id {
                        user_id = Some(user.user_id);
                    }
                    reactions.push(MessageReaction {
                        user_id: user_id,
                        emoji: emoji.emoji.clone(),
                    });
                }
            }
        }
    }
    return reactions;
}
