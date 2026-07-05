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

export type UpdateChatFolders = {
  chat_folders: Array<{
    id: number;
    name: {
      text: {
        text: string
      }
    };
    icon: { name: string };
    color_id: number;
  }>
}

export type ChatFolder = {
  id: number;
  name: string;
}

export type ChatListMain = {
  '@type': 'chatListMain';
}
export type ChatListFolder = {
  '@type': 'chatListFolder';
  chat_folder_id: number;
}
export type ChatListArchive = {
  '@type': 'chatListArchive';
}

export type ChatList = ChatListMain | ChatListFolder | ChatListArchive;

export type UpdateChatAddedToList = {
  chat_id: number;
  chat_list: ChatList;
}

export type FormattedText = {
  text: string;
  entities: any[];
}

export type MessageText = {
  '@type': 'messageText';
  text: FormattedText
}

export type Document = {
  file_name: string;
  mime_type: string;
  document: File;
}

export type MessageDocument = {
  '@type': 'messageDocument';
  document: Document;
  caption: FormattedText;
}

export type AnimatedEmoji = {
  sticker: Sticker;
}

export type MessageAnimatedEmoji = {
  '@type': 'messageAnimatedEmoji';
  animated_emoji: AnimatedEmoji;
  emoji: string;
}

export type LocalFile = {
  path: string;
  can_be_downloaded: boolean;
  can_be_deleted: boolean;
  is_downloading_active: boolean;
  is_downloading_completed: boolean;
}

export type RemoteFile = {
  id: string;
  unique_id: string;
  is_uploading_active: boolean;
  is_uploading_completed: boolean;
  uploaded_size: number;
}

export type File = {
  id: number;
  size: number;
  local: LocalFile;
  remote: RemoteFile;
}

export type PhotoSize = {
  type: string;
  photo: File;
  width: number;
  height: number;
}

export type Photo = {
  sizes: PhotoSize[]
}

export type MessagePhoto = {
  '@type': 'messagePhoto';
  photo: Photo;
  caption: FormattedText;
}

export type MessageVoiceNote = {
  '@type': 'messageVoiceNote';
  caption: FormattedText;
  is_listened: boolean;
  voice_note: {
    duration: number;
    waveform: string;
    mime_type: string;
    voice: File;
  }
}

export type MessageVideo = {
  '@type': 'messageVideo';
  caption: FormattedText;
}

export type MessageContent = MessageText | MessagePhoto | MessageDocument | MessageVideo | MessageAnimatedEmoji | MessageVoiceNote;

export type MessageSenderUser = {
  '@type': 'messageSenderUser';
  user_id: number;
}

export type MessageSenderChat = {
  '@type': 'messageSenderChat';
  chat_id: number;
}

export type UpdateNewMessage = {
  message: Message
}

export type MessageSender = MessageSenderUser | MessageSenderChat;

export type MessageOriginUser = {
  '@type': 'messageOriginUser';
  sender_user_id: number;
}

export type MessageOrigin = MessageOriginUser;

export type MessageReplyToMessage = {
  '@type': 'messageReplyToMessage';
  chat_id: number;
  message_id: number;
  quote: TextQuote;
  origin: MessageOrigin | null;
  origin_send_date: number;
  content: MessageContent;
}

export type MessageReplyTo = MessageReplyToMessage;

export type Message = {
  id: number;
  sender_id: MessageSender;
  chat_id: number;
  is_pinned: boolean;
  contains_unread_mention: boolean;
  date: number;
  edit_date: number;
  interaction_info: MessageInteractionInfo | null;
  unread_reactions: UnreadReaction[];
  reply_to: MessageReplyTo;
  has_sensitive_content: boolean;
  content: MessageContent;
  sending_state: {
    '@type': 'messageSendingStatePending' | 'messageSendingStateFailed'
  } | null
}

export type Messages = {
  total_count: number;
  messages: Message[];
}

export type TextQuote = {
  text: FormattedText;
  position: number;
  is_manual: boolean;
}

export type InputTextQuote = {
  text: FormattedText;
  position: number;
}

export type InputMessageReplyToMessage = {
  '@type': 'inputMessageReplyToMessage';
  message_id: number;
  quote: InputTextQuote | null;
  // Identifier of the checklist task in the original message that was replied; 0 if none
  checklist_task_id: number;
  poll_option_id: string;
}

export type InputMessageReplyTo = InputMessageReplyToMessage;

export type InputMessageText = {
  '@type': 'inputMessageText',
  text: FormattedText;
  clear_draft: boolean;
}

export type InputFileLocal = {
  '@type': 'inputFileLocal',
  path: string;
}

export type InputFile = InputFileLocal;

export type InputThumbnail = {
  thumbnail: InputFile;
  width: number;
  height: number;
}

export type MessageSelfDestructTypeTimer = {
  '@type': 'messageSelfDestructTypeTimer';
  self_destruct_time: number;
}

export type MessageSelfDestructTypeImmediately = {
  '@type': 'messageSelfDestructTypeImmediately';
}

export type MessageSelfDestructType = MessageSelfDestructTypeTimer | MessageSelfDestructTypeImmediately;

export type InputMessagePhoto = {
  '@type': 'inputMessagePhoto';
  photo: {
    photo: InputFile;
    thumbnail: InputThumbnail | null;
    video: null;
    added_sticker_file_ids: number[];
    width: number;
    height: number;
  },
  caption: FormattedText | null;
  show_caption_above_media: boolean;
  self_destruct_type: MessageSelfDestructType | null;
  has_spoiler: boolean;
}

export type InputMessageDocument = {
  '@type': 'inputMessageDocument';
  document: {
    document: InputFile;
    thumbnail: InputThumbnail | null;
    disable_content_type_detection: boolean;
  }
  caption: FormattedText | null;
}

export type InputMessageContent = InputMessageText | InputMessagePhoto | InputMessageDocument;

export type UpdateFile = {
  file: File;
}

export type PublicKeyFingerprints = {
  primary: string;
  encryption_keys: string[];
}

export type UpdateDeleteMessages = {
  message_ids: number[];
  chat_id: number;
}

export type Usernames = {
  active_usernames: string[];
  disabled_usernames: string[];
  editable_username: string;
}

export type UserStatusEmpty = {
  '@type': 'userStatusEmpty';
}

export type UserStatusOnline = {
  '@type': 'userStatusOnline';
}

export type UserStatusOffline = {
  '@type': 'userStatusOffline';
}

export type UserStatusRecently = {
  '@type': 'userStatusRecently';
}

export type UserStatusLastWeek = {
  '@type': 'userStatusLastWeek';
}

export type UserStatusLastMonth = {
  '@type': 'userStatusLastMonth';
}

export type UserStatus = UserStatusEmpty | UserStatusOnline | UserStatusOffline | UserStatusRecently | UserStatusLastWeek | UserStatusLastMonth;

export type ProfilePhoto = {
  id: number;
  small: File;
  big: File;
  is_personal: boolean;
}

export type UserTypeRegular = {
  '@type': 'userTypeRegular';
}

export type UserTypeDeleted = {
  '@type': 'userTypeDeleted';
}

export type UserTypeBot = {
  '@type': 'userTypeBot';
}

export type UserTypeUnknown = {
  '@type': 'userTypeUnknown';
}

export type UserType = UserTypeRegular | UserTypeDeleted | UserTypeBot | UserTypeUnknown;

export type User = {
  id: number;
  first_name: string;
  last_name: string;
  usernames: Usernames | null;
  phone_number: string;
  status: UserStatus;
  profile_photo: ProfilePhoto;
  is_contact: boolean;
  is_mutual_contact: boolean;
  is_close_friend: boolean;
  user_type: UserType;
}

export type UpdateUser = {
  user: User;
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

export type UpdateChatRemovedFromList = {
  chat_id: number;
  chat_list: ChatList;
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
  recent_replier_ids: MessageSender[];
  last_read_inbox_message_id: number;
  last_read_outbox_message_id: number;
  last_message_id: number;
}

export type ReactionTypeEmoji = {
  '@type': 'reactionTypeEmoji';
  emoji: string;
}

export type ReactionType = ReactionTypeEmoji;

export type MessageReaction = {
  type: ReactionType;
  totalCount: number;
  is_chosen: boolean;
  used_sender_id: MessageSender | null;
  recent_sender_ids: MessageSender[];
}

export type MessageReactions = {
  reactions: MessageReaction[];
}

export type MessageInteractionInfo = {
  view_count: number;
  forward_count: number;
  reply_info: MessageReplyInfo | null;
  reactions: MessageReactions | null;
}

export type UpdateMessageInteractionInfo = {
  chat_id: number;
  message_id: number;
  interaction_info: MessageInteractionInfo;
}

export type UnreadReaction = {
  type: ReactionType;
  sender_id: MessageSender;
  is_big: boolean;
}
