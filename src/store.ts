import { reactive } from 'vue'
import type { Chat, ChatFolder, Message, File, User } from './model';

export const store = reactive({
  sidebarExpanded: false,
  toggleSidebar() {
    this.sidebarExpanded = !this.sidebarExpanded;
  },
  myId: 0,
  settingsModalActive: false,
  chatSettingsModalActive: false,
  messageModalActive: false,
  toggleSettingsModal() {
    this.settingsModalActive = !this.settingsModalActive;
  },
  toggleChatSettingsModal() {
    this.chatSettingsModalActive = !this.chatSettingsModalActive;
  },
  toggleMessageModal() {
    this.messageModalActive = !this.messageModalActive;
  },
  chatFolders: [] as ChatFolder[],
  chatsMap: {} as { [id: number]: Chat },
  // key are folders id, values are id of chats in each folder
  chatFoldersMap: {} as { [id: number]: number[] },
  usersMap: {} as { [id: number]: User },
  selectedChatFolderId: 0,
  getChat(chatId: number): Chat | undefined {
    return this.chatsMap[chatId];
  },
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
  selectedChat: null as Chat | null,
  selectChat(id: number | null) {
    if (id === null) {
      this.selectedChat = null;
    } else {
      const chat = (this.chatsMap as { [id: number]: Chat })[id];
      this.selectedChat = chat || null;
    }
  },
  selectedChatKey: '',
  selectedMessage: null as Message | null,
  lastMessageId: 0,
  currentMessages: [] as Message[],
  addMessages(newMessages: Message[]) {
    const chat = this.selectedChat as null | Chat;
    if (chat === null) {
      return;
    }
    const messages = this.currentMessages as Message[];
    for (const message of newMessages) {
      if (message.chat_id !== chat.id) {
        return;
      }
      if (!messages.find(m => m.id === message.id)) {
        messages.push(message);
      }
    }
    messages.sort((m1: Message, m2: Message) => m1.id < m2.id ? -1 : 1);
    this.lastMessageId = messages[messages.length - 1];
  },
  clearMessages() {
    const messages = this.currentMessages as Message[];
    messages.splice(0, messages.length);
  },
  deleteMessages(messageIds: number[]) {
    const messages = this.currentMessages as Message[];
    this.currentMessages = messages.filter(m => !messageIds.includes(m.id));
  },
  updateFile(file: File) {
    const messages = this.currentMessages as Message[];
    this.currentMessages = messages.map(m => {
      if (m.content['@type'] === 'messageDocument') {
        if (m.content.document.document.id === file.id) {
          m.content.document.document = file;
        }
      } else if (m.content['@type'] === 'messagePhoto') {
        for (const size of m.content.photo.sizes) {
          if (size.photo.id === file.id) {
            size.photo = file;
            break;
          }
        }
      }
      return m;
    });
  },
  getUser(userId: number): User | undefined {
    const usersMap = this.usersMap as { [id: number]: User };
    return usersMap[userId];
  },
  updateUser(user: User) {
    this.usersMap[user.id] = user;
  }
});
