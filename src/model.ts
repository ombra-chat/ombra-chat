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

export type ChatAvailableReactionsAll = {
  '@type': 'chatAvailableReactionsAll';
  max_reaction_count: number;
}

export type ChatAvailableReactionsSome = {
  '@type': 'chatAvailableReactionsSome';
  reactions: ReactionType[];
  max_reaction_count: number;
}

export type ChatAvailableReactions = ChatAvailableReactionsAll | ChatAvailableReactionsSome;

export type ChatPermission = {
  can_send_basic_messages: boolean;
  can_send_audios: boolean;
  can_send_documents: boolean;
  can_send_photos: boolean;
  can_send_videos: boolean;
  can_send_video_notes: boolean;
  can_send_voice_notes: boolean;
  can_send_polls: boolean;
  can_send_other_messages: boolean;
  can_add_link_previews: boolean;
  can_change_info: boolean;
  can_invite_users: boolean;
  can_pin_messages: boolean;
  can_create_topics: boolean;
}

export type UpdateNewChat = {
  chat: Chat
}

export type ChatNotificationSettings = {
  mute_for: number;
}

export type ChatTypePrivate = {
  '@type': 'chatTypePrivate';
  user_id: number;
}

export type ChatTypeSecret = {
  '@type': 'chatTypeSecret';
  secret_chat_id: number;
  user_id: number;
}

export type ChatTypeBasicGroup = {
  '@type': 'chatTypeBasicGroup';
  basic_group_id: number;
}

export type ChatTypeSupergroup = {
  '@type': 'chatTypeSupergroup';
  supergroup_id: number;
  is_channel: boolean;
}

export type ChatType = ChatTypePrivate | ChatTypeSecret | ChatTypeBasicGroup | ChatTypeSupergroup;

export type Chat = {
  id: number;
  type: ChatType;
  title: string;
  photo: any;
  permissions: ChatPermission;
  last_message: Message;
  positions: ChatPosition[];
  unread_count: number;
  notification_settings: ChatNotificationSettings;
  last_read_inbox_message_id: number;
  last_read_outbox_message_id: number;
  unread_mention_count: number;
  unread_reaction_count: number;
  available_reactions: ChatAvailableReactions;
  can_be_deleted_for_all_users: boolean;
  can_be_deleted_only_for_self: boolean;
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

export type MessageVideo = {
  '@type': 'messageVideo';
  caption: FormattedText;
}

export type MessageContent = MessageText | MessagePhoto | MessageDocument | MessageVideo | MessageAnimatedEmoji;

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
  photo: InputFile;
  thumbnail: InputThumbnail;
  width: number;
  height: number;
  caption: FormattedText | null;
  added_sticker_file_ids: number[];
  show_caption_above_media: boolean;
  self_destruct_type: MessageSelfDestructType | null;
  has_spoiler: boolean;
}

export type InputMessageDocument = {
  '@type': 'inputMessageDocument';
  document: InputFile;
  thumbnail: InputThumbnail | null;
  disable_content_type_detection: boolean;
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

export type ChatPosition = {
  list: ChatList;
  order: number;
  is_pinned: boolean;
}

export type UpdateChatPosition = {
  chat_id: number;
  position: ChatPosition;
}

export type UpdateChatLastMessage = {
  chat_id: number;
  positions: ChatPosition[];
  last_message: Message;
}

export type UpdateChatReadInbox = {
  chat_id: number;
  last_read_inbox_message_id: number;
  unread_count: number;
}

export type UpdateUnreadChatCount = {
  chat_list: ChatList;
  unread_unmuted_count: number;
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

export type UpdateAvailableMessageEffects = {
  reaction_effect_ids: number[];
  sticker_effect_ids: number[];
}

export type Sticker = {
  id: number;
  set_id: number;
  width: number;
  height: number;
  emoji: string;
  sticker: File;
}

export type MessageEffectTypeEmojiReaction = {
  '@type': 'messageEffectTypeEmojiReaction';
}

export type MessageEffectTypePremiumSticker = {
  '@type': 'messageEffectTypePremiumSticker';
}

export type MessageEffectType = MessageEffectTypeEmojiReaction | MessageEffectTypePremiumSticker;

export type MessageEffect = {
  id: number;
  static_icon: Sticker | null;
  emoji: string;
  is_premium: boolean;
  type: MessageEffectType;
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
