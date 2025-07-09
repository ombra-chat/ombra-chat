import { reactive } from 'vue'
import type { Chat, ChatFolder } from './model';

export const store = reactive<{
  sidebarExpanded: boolean;
  toggleSidebar: () => void;
  chatFolders: ChatFolder[];
  chatsMap: { [id: number]: Chat };
  // key are folders id, values are id of chats in each folder
  chatFoldersMap: { [id: number]: number[] };
  selectedChatFolderId: number;
  addChat: (chat: Chat) => void;
  addChatToFolder: (folder_id: number, chat_id: number) => void;
  test: any;
}>({
  sidebarExpanded: false,
  toggleSidebar() {
    this.sidebarExpanded = !this.sidebarExpanded;
  },
  chatFolders: [],
  chatsMap: {},
  chatFoldersMap: {},
  selectedChatFolderId: 0,
  addChat(chat: Chat) {
    this.chatsMap[chat.id] = chat;
  },
  addChatToFolder(folder_id: number, chat_id: number) {
    let chats = this.chatFoldersMap[folder_id];
    if (chats === undefined) {
      chats = [chat_id];
    } else {
      chats.push(chat_id);
    }
    this.chatFoldersMap[folder_id] = chats;
  },
  test: null
});
