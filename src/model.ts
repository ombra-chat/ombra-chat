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

export type ChatType = 'chatTypePrivate';

export type AvailableReactionsType = 'chatAvailableReactionsAll';

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

export type Chat = {
  id: number;
  type: { '@type': ChatType; user_id: number };
  title: string;
  photo: any;
  permissions: ChatPermission;
  unread_count: number;
  last_read_inbox_message_id: number;
  last_read_outbox_message_id: number;
  unread_mention_count: number;
  unread_reaction_count: number;
  available_reactions: { '@type': AvailableReactionsType; max_reaction_count: number };
}

export type UpdateChatAddedToList = {
  chat_id: number;
  chat_list: { '@type': 'chatListMain'; }
  | { '@type': 'chatListFolder'; chat_folder_id: number }
  | { '@type': 'chatListArchive' };
}

export type FormattedText = {
  text: string;
  entities: any[];
}

export type MessageText = {
  '@type': 'messageText';
  text: FormattedText
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
  width: number;
  height: number;
  photo: File;
}

export type Photo = {
  sizes: PhotoSize[]
}

export type MessagePhoto = {
  '@type': 'messagePhoto';
  photo: Photo;
  caption: FormattedText;
}

export type MessageContent = MessageText | MessagePhoto;

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

export type Message = {
  id: number;
  sender_id: MessageSender;
  chat_id: number;
  is_pinned: boolean;
  contains_unread_mention: boolean;
  date: number;
  edit_date: number;
  interaction_info: any;
  unread_reactions: any[];
  reply_to: any;
  has_sensitive_content: boolean;
  content: MessageContent;
}

export type Messages = {
  total_count: number;
  messages: Message[];
}