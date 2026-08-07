export type Chat = {
  id: number;
  secret_chat_id: number | null;
  title: string;
  unread_count: number;
  muted: boolean;
  user_id: number;
  secret: boolean;
  folders: number[];
  pos: number;
  can_send: boolean;
  can_delete_for_all: boolean;
  can_delete_for_self: boolean;
  last_read_inbox_message_id: number;
  last_read_outbox_message_id: number;
  reactions: string[] | 'All';
}

export type ChatPosition = {
  chat_id: number;
  pos: number;
}

export type RemoveChatFromFolder = {
  chat_id: number;
  folder_id: number;
}

export type SecretChat = {
  id: number;
  user_id: number;
  state: 'Pending' | 'Ready' | 'Closed'
}

export type ChatFolder = {
  id: number;
  name: string;
}

export type UpdateChatAddedToFolder = {
  chat_id: number;
  folder_id: number;
}

export type UpdateChatRemovedFromFolder = {
  chat_id: number;
  folder_id: number;
}

export type File = {
  id: number;
  size: number;
  path: string | null;
}

export type PhotoSize = {
  photo: File;
  width: number;
  height: number;
}

export type MessageText = {
  '@type': 'messageText';
  text: string;
}

export type MessageDocument = {
  '@type': 'messageDocument';
  file_name: string;
  mime_type: string;
  document: File;
  caption: string;
}

export type MessageAnimatedEmoji = {
  '@type': 'messageAnimatedEmoji';
  emoji: string;
}

export type MessagePhoto = {
  '@type': 'messagePhoto';
  sizes: PhotoSize[];
  caption: string;
}

export type MessageVoiceNote = {
  '@type': 'messageVoiceNote';
  caption: string;
  is_listened: boolean;
  duration: number;
  voice: File;
}

export type MessagePgpText = {
  '@type': 'messagePgpText';
  document_id: number;
  text: string | null;
}

export type MessagePgpFile = {
  '@type': 'messagePgpFile';
  document: File;
  ciphertext_path: string | null;
  plaintext_path: string | null;
  file_name: string;
  caption: string | null;
}

export type MessagePgpKey = {
  '@type': 'messagePgpKey';
  document_id: number;
  path: string | null;
  fingerprint: string | null;
}

export type MessageContent = MessageText | MessagePhoto | MessageDocument | MessageAnimatedEmoji | MessageVoiceNote | MessagePgpText | MessagePgpFile | MessagePgpKey;

export type Message = {
  id: number;
  sender_user_id: number | null;
  sender_chat_id: number | null;
  chat_id: number;
  date: number;
  is_reply: boolean;
  reply_quote: string | null;
  content: MessageContent;
  reactions: MessageReaction[];
  sending_state: 'Pending' | 'Failed' | null;
  forwarded_from: {
    chat_id: number | null;
    chat_title: string | null;
  } | null;
}

export type InputMessageReplyTo = {
  message_id: number;
  quote: string | null;
};

export type InputMessageText = {
  '@type': 'inputMessageText',
  text: string;
}

export type InputMessagePhoto = {
  '@type': 'inputMessagePhoto';
  path: string;
  caption: string | null;
}

export type InputMessageDocument = {
  '@type': 'inputMessageDocument';
  path: string;
  caption: string | null;
}

export type InputMessagePgpText = {
  '@type': 'inputMessagePgpText';
  text: string;
}

export type InputMessagePgpFile = {
  '@type': 'inputMessagePgpFile';
  path: string;
  caption: string | null;
}

export type InputMessageContent = InputMessageText | InputMessagePhoto | InputMessageDocument | InputMessagePgpText | InputMessagePgpFile;

export type PublicKeyFingerprints = {
  primary: string;
  encryption_keys: string[];
}

export type UpdateDeleteMessages = {
  message_ids: number[];
  chat_id: number;
}

export type User = {
  id: number;
  display_text: string;
}

export type UpdateChatReadInbox = {
  chat_id: number;
  last_read_inbox_message_id: number;
  unread_count: number;
}

export type MessageWithStatus = Message & {
  read: boolean;
}

export type UpdateMessageSendSucceeded = {
  message: Message;
  old_message_id: number;
}

export type Sticker = {
  id: number;
  set_id: number;
  width: number;
  height: number;
  emoji: string;
  sticker: File;
}

export type MessageReplyInfo = {
  replyCount: number;
  last_read_inbox_message_id: number;
  last_read_outbox_message_id: number;
  last_message_id: number;
}

export type MessageReaction = {
  user_id: number | null;
  emoji: string;
}

export type UpdateMessageReactions = {
  chat_id: number;
  message_id: number;
  reactions: MessageReaction[];
}
