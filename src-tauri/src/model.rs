use tdlib::enums::{ChatAvailableReactions, ChatList, ChatType, ReactionType::Emoji};

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
