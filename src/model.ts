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
  chat: {
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
    available_reactions: { '@type': AvailableReactionsType, max_reaction_count: number };
  }
}

export type Chat = {
  id: number;
  title: string;
  permissions: ChatPermission;
}

export type UpdateChatAddedToList = {
  chat_id: number;
  chat_list: { '@type': 'chatListMain', }
  | { '@type': 'chatListFolder', chat_folder_id: number }
  | { '@type': 'chatListArchive' }
}
