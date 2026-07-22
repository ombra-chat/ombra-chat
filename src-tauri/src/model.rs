use tdlib::enums::{
    ChatAvailableReactions, ChatList, ChatType, ReactionType::Emoji, UserType,
};

#[derive(serde::Serialize, Clone)]
pub enum ChatReactions {
    All,
    Some(Vec<String>),
}

#[derive(serde::Serialize, Clone)]
pub struct Chat {
    id: i64,
    secret_chat_id: Option<i32>,
    title: String,
    unread_count: i32,
    user_id: Option<i64>,
    muted: bool,
    secret: bool,
    folders: Vec<i32>,
    // this field is set only if the chat is inside the main folder
    pos: Option<i64>,
    can_send: bool,
    can_delete_for_all: bool,
    can_delete_for_self: bool,
    last_read_inbox_message_id: i64,
    last_read_outbox_message_id: i64,
    reactions: ChatReactions,
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
                reactions = ChatReactions::All;
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
}

impl SecretChat {
    pub fn from(update: &tdlib::types::UpdateSecretChat) -> SecretChat {
        let chat = &update.secret_chat;
        let state = match chat.state {
            tdlib::enums::SecretChatState::Pending => SecretChatState::Pending,
            tdlib::enums::SecretChatState::Ready => SecretChatState::Ready,
            tdlib::enums::SecretChatState::Closed => SecretChatState::Closed,
        };
        SecretChat {
            id: chat.id,
            user_id: chat.user_id,
            state: state,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct RemoveChatFromFolder {
    folder_id: i32,
    chat_id: i64,
}

impl RemoveChatFromFolder {
    pub fn new(folder_id: i32, chat_id: i64) -> RemoveChatFromFolder {
        RemoveChatFromFolder {
            folder_id: folder_id,
            chat_id: chat_id,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateChatReadInbox {
    chat_id: i64,
    last_read_inbox_message_id: i64,
    unread_count: i32,
}

impl UpdateChatReadInbox {
    pub fn from(update: &tdlib::types::UpdateChatReadInbox) -> UpdateChatReadInbox {
        UpdateChatReadInbox {
            chat_id: update.chat_id,
            last_read_inbox_message_id: update.last_read_inbox_message_id,
            unread_count: update.unread_count,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct ChatFolder {
    id: i32,
    name: String,
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
    chat_id: i64,
    folder_id: i32,
}

impl UpdateChatAddedToFolder {
    pub fn new(chat_id: i64, folder_id: i32) -> UpdateChatAddedToFolder {
        UpdateChatAddedToFolder {
            chat_id: chat_id,
            folder_id: folder_id,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateChatRemovedFromFolder {
    chat_id: i64,
    folder_id: i32,
}

impl UpdateChatRemovedFromFolder {
    pub fn new(chat_id: i64, folder_id: i32) -> UpdateChatRemovedFromFolder {
        UpdateChatRemovedFromFolder {
            chat_id: chat_id,
            folder_id: folder_id,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct User {
    id: i64,
    display_text: String,
}

impl User {
    pub fn from(update: &tdlib::types::UpdateUser) -> User {
        return User {
            id: update.user.id,
            display_text: get_user_display_text(&update.user),
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
pub struct Message {
    pub id: i64,
    pub sender_user_id: Option<i64>,
    pub sender_chat_id: Option<i64>,
    pub chat_id: i64,
    pub date: i32,
    pub is_reply: bool,
    pub reply_quote: Option<String>,
    pub content: MessageContent,
    pub reactions: Vec<MessageReaction>,
    pub sending_state: Option<MessageSendingState>,
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateMessageSendSucceeded {
    pub message: Message,
    pub old_message_id: i64
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
    pub downloading: bool
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
    pub text: Option<String>
}

#[derive(serde::Serialize, Clone)]
pub struct MessagePgpFile {
    pub document_id: i32,
    pub ciphertext_path: Option<String>,
    pub plaintext_path: Option<String>,
    pub file_name: String,
    pub caption: Option<String>
}

#[derive(serde::Serialize, Clone)]
pub struct MessageError {
    pub text: String
}

#[derive(serde::Serialize, Clone)]
pub struct UpdateMessageReactions {
    pub message_id: i64,
    pub chat_id: i64,
    pub reactions: Vec<MessageReaction>,
}

#[derive(serde::Serialize, Clone)]
struct LocalFile {
    path: String,
    can_be_downloaded: bool,
    can_be_deleted: bool,
    is_downloading_active: bool,
    is_downloading_completed: bool,
}

impl LocalFile {
    fn from(file: &tdlib::types::LocalFile) -> LocalFile {
        LocalFile {
            path: file.path.clone(),
            can_be_downloaded: file.can_be_downloaded,
            can_be_deleted: file.can_be_deleted,
            is_downloading_active: file.is_downloading_active,
            is_downloading_completed: file.is_downloading_completed,
        }
    }
}

#[derive(serde::Serialize, Clone)]
struct RemoteFile {
    id: String,
    unique_id: String,
    is_uploading_active: bool,
    is_uploading_completed: bool,
    uploaded_size: i64,
}

impl RemoteFile {
    fn from(file: &tdlib::types::RemoteFile) -> RemoteFile {
        RemoteFile {
            id: file.id.clone(),
            unique_id: file.unique_id.clone(),
            is_uploading_active: file.is_uploading_active,
            is_uploading_completed: file.is_uploading_completed,
            uploaded_size: file.uploaded_size,
        }
    }
}

#[derive(serde::Serialize, Clone)]
pub struct File {
    id: i32,
    size: i64,
    local: LocalFile,
    remote: RemoteFile,
}

impl File {
    pub fn from(file: &tdlib::types::File) -> File {
        File {
            id: file.id,
            size: file.size,
            local: LocalFile::from(&file.local),
            remote: RemoteFile::from(&file.remote),
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
#[serde(tag = "@type")]
pub enum InputMessageContent {
    #[serde(rename(serialize = "inputMessageText", deserialize = "inputMessageText"))]
    Text(InputMessageText),
    #[serde(rename(serialize = "inputMessageDocument", deserialize = "inputMessageDocument"))]
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
    pub width: i32,
    pub height: i32,
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
