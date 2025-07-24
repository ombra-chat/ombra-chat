import { reactive } from 'vue'
import type { Chat, ChatFolder, Message, File, User, ChatPosition, MessageWithStatus } from './model';
import { viewMessage } from './services/chats';

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
  removeChatFromFolder(folder_id: number, chat_id: number) {
    const chatFoldersMap = this.chatFoldersMap as { [id: number]: number[] };
    if (!(folder_id in chatFoldersMap)) {
      return;
    }
    const chats = chatFoldersMap[folder_id];
    chatFoldersMap[folder_id] = chats.filter(id => id !== chat_id);
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
  loadingNewMessages: false,
  messagesToLoad: [] as number[],
  scrollTargetMessageId: 0,
  selectedMessage: null as Message | null,
  lastMessageId: 0,
  currentMessages: [] as MessageWithStatus[],
  messageLoaded(messageId: number) {
    const messages = this.messagesToLoad as number[];
    this.messagesToLoad = messages.filter(m => m !== messageId);
  },
  addMessages(newMessages: Message[]) {
    const chat = this.selectedChat as null | Chat;
    if (chat === null) {
      return;
    }
    const messages = this.currentMessages as MessageWithStatus[];
    for (const message of newMessages) {
      if (message.chat_id !== chat.id) {
        return;
      }
      if (!messages.find(m => m.id === message.id)) {
        this.messagesToLoad.push(message.id);
        messages.push({
          ...message,
          read: message.id <= chat.last_read_inbox_message_id ||
            (message.sender_id['@type'] === 'messageSenderUser' && message.sender_id.user_id === this.myId)
        });
      }
    }
    messages.sort((m1: Message, m2: Message) => m1.id < m2.id ? -1 : 1);
    this.lastMessageId = messages[messages.length - 1].id;
  },
  clearMessages() {
    this.currentMessages = [];
    this.messagesToLoad = [];
  },
  deleteMessages(messageIds: number[]) {
    const messages = this.currentMessages as MessageWithStatus[];
    const messagesToLoad = this.messagesToLoad as number[];
    this.currentMessages = messages.filter(m => !messageIds.includes(m.id));
    this.messagesToLoad = messagesToLoad.filter(id => !messageIds.includes(id));
  },
  updateFile(file: File) {
    const messages = this.currentMessages as MessageWithStatus[];
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
  },
  updateChatPosition(chatId: number, newPosition: ChatPosition) {
    if (newPosition.list['@type'] !== 'chatListMain') {
      return;
    }

    const chatsMap = this.chatsMap as { [id: number]: Chat };
    const chat = chatsMap[chatId];
    if (!chat) {
      return;
    }

    let newPositions: ChatPosition[] = [];
    let found = false;
    for (const pos of chat.positions) {
      if (pos.list['@type'] === newPosition.list['@type']) {
        newPositions.push(newPosition);
        found = true;
      } else {
        newPositions.push(pos);
      }
    }

    if (!found) {
      newPositions = [...chat.positions, newPosition];
    }

    chatsMap[chatId] = {
      ...chat,
      positions: newPositions
    }
  },
  updateChat(chatId: number, chatUpdate: Partial<Chat>) {
    const chatsMap = this.chatsMap as { [id: number]: Chat };
    const chat = chatsMap[chatId];
    if (!chat) {
      return;
    }
    chatsMap[chatId] = { ...chat, ...chatUpdate };
  },
  async markMessageAsRead(messageId: number) {
    const messages = this.currentMessages as MessageWithStatus[];
    for (const message of messages) {
      if (message.id === messageId) {
        if (!message.read) {
          await viewMessage(message.chat_id, message.id);
          message.read = true;
          return;
        }
      }
    }
  },
  updateMessage(oldMessageId: number, message: Message) {
    const messages = this.currentMessages as MessageWithStatus[];
    this.currentMessages = messages.map(m => m.id === oldMessageId ? message : m);
    this.messageLoaded(oldMessageId);
  }
});
