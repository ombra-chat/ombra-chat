use base64::prelude::*;
use tdlib::enums::{ChatAvailableReactions, ChatList, ChatType, ReactionType::Emoji, UserType};

#[derive(serde::Serialize, Clone)]
#[serde(untagged)]
pub enum ChatReactions {
    All(String),
    Some(Vec<String>),
}

impl Default for ChatReactions {
    fn default() -> Self {
        ChatReactions::All("All".into())
    }
}

#[derive(serde::Serialize, Clone)]
pub struct Chat {
    pub id: i64,
    pub secret_chat_id: Option<i32>,
    pub title: String,
    pub unread_count: i32,
    pub user_id: Option<i64>,
    pub muted: bool,
    pub secret: bool,
    pub folders: Vec<i32>,
    // this field is set only if the chat is inside the main folder
    pub pos: Option<i64>,
    pub can_send: bool,
    pub can_delete_for_all: bool,
    pub can_delete_for_self: bool,
    pub last_read_inbox_message_id: i64,
    pub last_read_outbox_message_id: i64,
    pub reactions: ChatReactions,
}

impl Chat {
    pub fn from(chat: &tdlib::types::Chat) -> Chat {
        let mut user_id: Option<i64> = None;
        if let ChatType::Private(t) = &chat.r#type {
            user_id = Some(t.user_id);
        }

        let mut secret_chat_id: Option<i32> = None;
        if let ChatType::Secret(t) = &chat.r#type {
            secret_chat_id = Some(t.secret_chat_id);
        }

        let reactions: ChatReactions;
        match &chat.available_reactions {
            ChatAvailableReactions::All(_) => {
                reactions = ChatReactions::default();
            }
            ChatAvailableReactions::Some(r) => {
                let mut emojis: Vec<String> = vec![];
                for r in &r.reactions {
                    if let Emoji(e) = r {
                        emojis.push(e.emoji.clone());
                    }
                }
                reactions = ChatReactions::Some(emojis);
            }
        }

        Chat {
            id: chat.id,
            secret_chat_id: secret_chat_id,
            title: chat.title.clone(),
            unread_count: chat.unread_count,
            user_id: user_id,
            muted: chat.notification_settings.mute_for > 0,
            secret: matches!(chat.r#type, ChatType::Secret(_)),
            folders: vec![],
            pos: get_chat_position(&chat.positions),
            can_send: chat.permissions.can_send_basic_messages,
            can_delete_for_all: chat.can_be_deleted_for_all_users,
            can_delete_for_self: chat.can_be_deleted_only_for_self,
            last_read_inbox_message_id: chat.last_read_inbox_message_id,
            last_read_outbox_message_id: chat.last_read_outbox_message_id,
            reactions: reactions,
        }
    }
}

pub fn get_chat_position(positions: &Vec<tdlib::types::ChatPosition>) -> Option<i64> {
    for pos in positions.as_slice() {
        if pos.list == ChatList::Main {
            return Some(pos.order);
        }
    }
    None
}

#[derive(serde::Serialize, Clone)]
pub struct ChatPosition {
    chat_id: i64,
    pos: i64,
}

impl ChatPosition {
    pub fn new(chat_id: i64, pos: i64) -> ChatPosition {
        ChatPosition {
            chat_id: chat_id,
            pos: pos,
        }
    }

    pub fn from(update: &tdlib::types::UpdateChatPosition) -> ChatPosition {
        ChatPosition {
            chat_id: update.chat_id,
            pos: update.position.order,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub enum SecretChatState {
    Pending,
    Ready,
    Closed,
}

#[derive(serde::Serialize, Clone)]
pub struct SecretChat {
    id: i32,
    user_id: i64,
    state: SecretChatState,
    key_hash_hex: Vec<String>,
    key_hash_img: Vec<Vec<u8>>,
}

impl SecretChat {
    pub fn from(update: &tdlib::types::UpdateSecretChat) -> SecretChat {
        let chat = &update.secret_chat;
        let state = match chat.state {
            tdlib::enums::SecretChatState::Pending => SecretChatState::Pending,
            tdlib::enums::SecretChatState::Ready => SecretChatState::Ready,
            tdlib::enums::SecretChatState::Closed => SecretChatState::Closed,
        };
        let decoded_key_hash: Vec<u8> = BASE64_STANDARD.decode(&chat.key_hash).unwrap_or(vec![]);
        SecretChat {
            id: chat.id,
            user_id: chat.user_id,
            state: state,
            key_hash_hex: get_key_hash_hex(&decoded_key_hash),
            key_hash_img: get_key_hash_img(&decoded_key_hash),
        }
    }
}

fn get_key_hash_hex(decoded: &Vec<u8>) -> Vec<String> {
    let mut result: Vec<String> = vec![];
    let hex_string = hex::encode(&decoded).to_uppercase();
    let mut i = 0;
    let mut current_row: Vec<String> = vec![];
    while i < 64 && i < hex_string.len() {
        if i > 0 && i % 16 == 0 {
            result.push(current_row.join(" "));
            current_row.clear();
        }
        if let Some(data) = hex_string.get(i..(i + 2)) {
            current_row.push(String::from(data));
        } else {
            break;
        }
        i += 2;
    }
    if current_row.len() > 0 {
        result.push(current_row.join(" "));
    }
    result
}

fn get_key_hash_img(decoded: &Vec<u8>) -> Vec<Vec<u8>> {
    let mut result: Vec<Vec<u8>> = vec![];
    let mut current_row: Vec<u8> = vec![];
    for b in decoded {
        let mut i = 0;
        while i < 8 {
            if current_row.len() == 12 {
                result.push(current_row.clone());
                current_row.clear();
            }
            let b0 = b >> i & 1;
            let b1 = b >> (i + 1) & 1;
            if b1 == 0 && b0 == 0 {
                current_row.push(0);
            }
            if b1 == 0 && b0 == 1 {
                current_row.push(1);
            }
            if b1 == 1 && b0 == 0 {
                current_row.push(2);
            }
            if b1 == 1 && b0 == 1 {
                current_row.push(3);
            }
            i += 2;
        }
    }
    result.push(current_row.clone());
    result
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateChatReadInbox {
    pub chat_id: i64,
    pub last_read_inbox_message_id: i64,
    pub unread_count: i32,
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateDeleteMessages {
    pub chat_id: i64,
    pub message_ids: Vec<i64>,
}

#[derive(serde::Serialize, Clone)]
pub struct ChatFolder {
    pub id: i32,
    pub name: String,
}

impl ChatFolder {
    pub fn new(id: i32, name: String) -> ChatFolder {
        ChatFolder { id: id, name: name }
    }

    pub fn from(chat_folder: &tdlib::types::ChatFolderInfo) -> ChatFolder {
        ChatFolder {
            id: chat_folder.id,
            name: chat_folder.name.text.text.clone(),
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateChatAddedToFolder {
    pub chat_id: i64,
    pub folder_id: i32,
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateChatRemovedFromFolder {
    pub chat_id: i64,
    pub folder_id: i32,
}

#[derive(serde::Serialize, Clone)]
pub struct User {
    pub id: i64,
    pub display_text: String,
    pub phone_number: String,
    pub usernames: Vec<String>,
}

impl User {
    pub fn from(update: &tdlib::types::UpdateUser) -> User {
        let mut usernames = vec![];
        if let Some(values) = &update.user.usernames {
            for username in &values.active_usernames {
                usernames.push(username.clone());
            }
        }
        return User {
            id: update.user.id,
            display_text: get_user_display_text(&update.user),
            phone_number: update.user.phone_number.clone(),
            usernames: usernames,
        };
    }
}

fn get_user_display_text(user: &tdlib::types::User) -> String {
    if !user.first_name.is_empty() {
        if !user.last_name.is_empty() {
            let first_name = user.first_name.clone();
            let last_name = user.last_name.clone();
            return format!("{first_name} {last_name}");
        }
        return user.first_name.clone();
    }

    if let Some(usernames) = &user.usernames {
        if !usernames.editable_username.is_empty() {
            return usernames.editable_username.clone();
        }
        if usernames.active_usernames.len() > 0 {
            return usernames.active_usernames[0].clone();
        }
    }

    if user.r#type == UserType::Deleted {
        return String::from("Deleted account");
    }

    return String::new();
}

#[derive(serde::Serialize, Clone)]
pub enum MessageSendingState {
    Pending,
    Failed,
}

#[derive(serde::Serialize, Clone)]
pub struct MessageReaction {
    pub user_id: Option<i64>,
    pub emoji: String,
}

#[derive(serde::Serialize, Clone)]
pub struct ForwardedFrom {
    pub chat_id: Option<i64>,
    pub chat_title: Option<String>,
}

impl ForwardedFrom {
    pub fn from(message_forward_info: &tdlib::types::MessageForwardInfo) -> ForwardedFrom {
        match &message_forward_info.origin {
            tdlib::enums::MessageOrigin::User(origin) => {
                return ForwardedFrom {
                    chat_id: Some(origin.sender_user_id),
                    chat_title: None,
                }
            }
            tdlib::enums::MessageOrigin::HiddenUser(origin) => {
                return ForwardedFrom {
                    chat_id: None,
                    chat_title: Some(origin.sender_name.clone()),
                }
            }
            tdlib::enums::MessageOrigin::Chat(origin) => {
                return ForwardedFrom {
                    chat_id: Some(origin.sender_chat_id),
                    chat_title: Some(origin.author_signature.clone()),
                }
            }
            tdlib::enums::MessageOrigin::Channel(origin) => {
                return ForwardedFrom {
                    chat_id: Some(origin.chat_id),
                    chat_title: None,
                }
            }
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct Message {
    pub id: i64,
    pub sender_user_id: Option<i64>,
    pub sender_chat_id: Option<i64>,
    pub chat_id: i64,
    pub date: i32,
    pub is_reply: bool,
    pub reply_quote: Option<String>,
    pub forwarded_from: Option<ForwardedFrom>,
    pub content: MessageContent,
    pub reactions: Vec<MessageReaction>,
    pub sending_state: Option<MessageSendingState>,
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateMessageSendSucceeded {
    pub message: Message,
    pub old_message_id: i64,
}

#[derive(serde::Serialize, Clone)]
#[serde(tag = "@type")]
pub enum MessageContent {
    #[serde(rename(serialize = "messageText", deserialize = "messageText"))]
    Text(MessageText),
    #[serde(rename(serialize = "messagePhoto", deserialize = "messagePhoto"))]
    Photo(MessagePhoto),
    #[serde(rename(serialize = "messageDocument", deserialize = "messageDocument"))]
    Document(MessageDocument),
    #[serde(rename(serialize = "messageVoiceNote", deserialize = "messageVoiceNote"))]
    VoiceNote(MessageVoiceNote),
    #[serde(rename(
        serialize = "messageAnimatedEmoji",
        deserialize = "messageAnimatedEmoji"
    ))]
    AnimatedEmoji(MessageAnimatedEmoji),
    #[serde(rename(serialize = "messagePgpText", deserialize = "messagePgpText"))]
    PgpText(MessagePgpText),
    #[serde(rename(serialize = "messagePgpFile", deserialize = "messagePgpFile"))]
    PgpFile(MessagePgpFile),
    #[serde(rename(serialize = "messagePgpKey", deserialize = "messagePgpKey"))]
    PgpKey(MessagePgpKey),
    #[serde(rename(serialize = "messageError", deserialize = "messageError"))]
    Error(MessageError),
    #[serde(rename(serialize = "messageUnsupported", deserialize = "messageUnsupported"))]
    Unsupported(tdlib::enums::MessageContent),
}

#[derive(serde::Serialize, Clone)]
pub struct MessageText {
    pub text: String,
}

#[derive(serde::Serialize, Clone)]
pub struct MessagePhoto {
    pub sizes: Vec<PhotoSize>,
    pub caption: String,
}

#[derive(serde::Serialize, Clone)]
pub struct MessageDocument {
    pub file_name: String,
    pub mime_type: String,
    pub document: File,
    pub caption: String,
    pub downloaded: bool,
    pub downloading: bool,
}

#[derive(serde::Serialize, Clone)]
pub struct MessageAnimatedEmoji {
    pub emoji: String,
}

#[derive(serde::Serialize, Clone)]
pub struct MessageVoiceNote {
    pub caption: String,
    pub is_listened: bool,
    pub duration: i32,
    pub voice: File,
}

#[derive(serde::Serialize, Clone)]
pub struct MessagePgpText {
    pub document_id: i32,
    pub text: Option<String>,
}

#[derive(serde::Serialize, Clone)]
pub struct MessagePgpFile {
    pub document: File,
    pub ciphertext_path: Option<String>,
    pub plaintext_path: Option<String>,
    pub file_name: String,
    pub caption: Option<String>,
}

#[derive(serde::Serialize, Clone)]
pub struct MessagePgpKey {
    pub document_id: i32,
    pub path: Option<String>,
    pub key_info: Option<PublicKeyCompleteInfo>,
}

#[derive(serde::Serialize, Clone)]
pub struct MessageError {
    pub text: String,
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateMessageReactions {
    pub message_id: i64,
    pub chat_id: i64,
    pub reactions: Vec<MessageReaction>,
}

#[derive(serde::Serialize, Clone)]
pub struct File {
    pub id: i32,
    pub size: i64,
    pub path: Option<String>,
    pub downloading: bool,
}

impl File {
    pub fn from(file: &tdlib::types::File) -> File {
        let path: Option<String>;
        if file.local.is_downloading_completed {
            path = Some(file.local.path.clone());
        } else {
            path = None;
        }
        File {
            id: file.id,
            size: file.size,
            path: path,
            downloading: file.local.is_downloading_active,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct PhotoSize {
    pub photo: File,
    pub width: i32,
    pub height: i32,
}

impl PhotoSize {
    pub fn from(photo_size: &tdlib::types::PhotoSize) -> PhotoSize {
        PhotoSize {
            photo: File::from(&photo_size.photo),
            width: photo_size.width,
            height: photo_size.height,
        }
    }
}

#[derive(serde::Deserialize, Clone)]
pub struct InputMessageReplyTo {
    pub message_id: i64,
    pub quote: Option<String>,
}

#[derive(serde::Deserialize, Clone)]
#[serde(tag = "@type")]
pub enum InputMessageContent {
    #[serde(rename(serialize = "inputMessageText", deserialize = "inputMessageText"))]
    Text(InputMessageText),
    #[serde(rename(
        serialize = "inputMessageDocument",
        deserialize = "inputMessageDocument"
    ))]
    Document(InputMessageDocument),
    #[serde(rename(serialize = "inputMessagePhoto", deserialize = "inputMessagePhoto"))]
    Photo(InputMessagePhoto),
    #[serde(rename(serialize = "inputMessagePgpText", deserialize = "inputMessagePgpText"))]
    PgpText(InputMessagePgpText),
    #[serde(rename(serialize = "inputMessagePgpFile", deserialize = "inputMessagePgpFile"))]
    PgpFile(InputMessagePgpFile),
}

#[derive(serde::Deserialize, Clone)]
pub struct InputMessageText {
    pub text: String,
}

#[derive(serde::Deserialize, Clone)]
pub struct InputMessageDocument {
    pub path: String,
    pub caption: Option<String>,
}

#[derive(serde::Deserialize, Clone)]
pub struct InputMessagePhoto {
    pub path: String,
    pub caption: Option<String>,
}

#[derive(serde::Deserialize, Clone)]
pub struct InputMessagePgpText {
    pub text: String,
}

#[derive(serde::Deserialize, Clone)]
pub struct InputMessagePgpFile {
    pub path: String,
    pub caption: Option<String>,
}

#[derive(serde::Serialize, Clone)]
pub struct PublicKeyCompleteInfo {
    pub id_key_fingerprint: String,
    pub encryption_key_fingerprint: String,
    pub armored_data: String,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn build_key_hash_hex() {
        let base64_key = "gAgW4CAbiALapa8WM40B9c1xeUs6bdqu8auGx3VTyxnBT3oY";
        let decoded_key_hash: Vec<u8> = BASE64_STANDARD.decode(&base64_key).unwrap_or(vec![]);
        let result = get_key_hash_hex(&decoded_key_hash);
        assert_eq!(result.len(), 4);
        assert_eq!(result[0], "80 08 16 E0 20 1B 88 02");
        assert_eq!(result[1], "DA A5 AF 16 33 8D 01 F5");
        assert_eq!(result[2], "CD 71 79 4B 3A 6D DA AE");
        assert_eq!(result[3], "F1 AB 86 C7 75 53 CB 19");
    }

    #[test]
    fn build_key_hash_img() {
        let base64_key = "gAgW4CAbiALapa8WM40B9c1xeUs6bdqu8auGx3VTyxnBT3oY";
        let decoded_key_hash: Vec<u8> = BASE64_STANDARD.decode(&base64_key).unwrap_or(vec![]);
        let result = get_key_hash_img(&decoded_key_hash);
        assert_eq!(result.len(), 12);
        assert_eq!(result[0].len(), 12);
        assert_eq!(result[11].len(), 12);
        assert_eq!(result[0][0], 0);
        assert_eq!(result[0][1], 0);
        assert_eq!(result[0][2], 0);
        assert_eq!(result[0][3], 2);
        assert_eq!(result[11][0], 3);
        assert_eq!(result[11][1], 3);
        assert_eq!(result[11][2], 0);
        assert_eq!(result[11][3], 1);
    }
}
