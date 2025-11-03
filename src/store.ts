import { reactive } from 'vue'
import type { Chat, ChatFolder, Message, File, User, ChatPosition, MessageWithStatus, MessageInteractionInfo, SecretChat } from './model';
import { viewMessage } from './services/chats';

type Store = {
  sidebarExpanded: boolean;
  myId: number;
  settingsModalActive: boolean;
  chatSettingsModalActive: boolean;
  messageModalActive: boolean;
  chatFolders: ChatFolder[];
  chatsMap: Record<number, Chat>;
  secretChatsMap: Record<number, SecretChat>;
  // key are folders id, values are id of chats in each folder
  chatFoldersMap: Record<number, number[]>;
  usersMap: Record<number, User>;
  selectedChatFolderId: number;
  selectedChat: Chat | null;
  selectedChatKey: string;
  loadingNewMessages: boolean;
  messagesToLoad: number[];
  messagesBubblesToLoad: number[];
  scrollTargetMessageId: number;
  selectedMessage: Message | null;
  lastMessageId: number;
  currentMessages: MessageWithStatus[];
  replyToMessage: Message | null;
  replyToQuote: string | null;
  allReactions: Record<string, string>;
  aboutModalActive: boolean;
  getChat: (chatId: number) => Chat | undefined;
  getUser: (userId: number) => User | undefined;
  toggleSidebar: () => void;
  toggleSettingsModal: () => void;
  toggleChatSettingsModal: () => void;
  toggleMessageModal: () => void;
  addChat: (chat: Chat) => void;
  deleteChat: (chatId: number) => void;
  addChatToFolder: (folder_id: number, chat_id: number) => void;
  removeChatFromFolder: (folder_id: number, chat_id: number) => void;
  selectChat: (id: number | null) => void;
  messageLoaded: (messageId: number) => void;
  messageBubbleLoaded: (messageId: number) => void;
  addMessages: (newMessages: Message[]) => void;
  clearMessages: () => void;
  deleteMessages: (messageIds: number[]) => void;
  updateFile: (file: File) => void;
  updateUser: (user: User) => void;
  updateChatPosition: (chatId: number, newPosition: ChatPosition) => void;
  updateChat: (chatId: number, chatUpdate: Partial<Chat>) => void;
  updateSecretChat: (chatId: number, chat: SecretChat) => void;
  markMessageAsRead: (messageId: number) => Promise<void>;
  updateMessage: (oldMessageId: number, message: Message) => void;
  updateMessageInteractionInfo: (message_id: number, info: MessageInteractionInfo) => void;
  toggleAboutModal: () => void;
}

export const store = reactive<Store>({
  sidebarExpanded: false,
  toggleSidebar() {
    const store = this as Store;
    store.sidebarExpanded = !store.sidebarExpanded;
  },
  myId: 0,
  settingsModalActive: false,
  chatSettingsModalActive: false,
  messageModalActive: false,
  toggleSettingsModal() {
    const store = this as Store;
    store.settingsModalActive = !store.settingsModalActive;
  },
  toggleChatSettingsModal() {
    const store = this as Store;
    store.chatSettingsModalActive = !store.chatSettingsModalActive;
  },
  toggleMessageModal() {
    const store = this as Store;
    store.messageModalActive = !store.messageModalActive;
  },
  chatFolders: [],
  chatsMap: {},
  secretChatsMap: {},
  chatFoldersMap: {},
  usersMap: {},
  selectedChatFolderId: 0,
  getChat(chatId: number): Chat | undefined {
    return (this as Store).chatsMap[chatId];
  },
  addChat(chat: Chat) {
    (this as Store).chatsMap[chat.id] = chat;
  },
  deleteChat(chatId: number) {
    delete (this as Store).chatsMap[chatId];
  },
  addChatToFolder(folder_id: number, chat_id: number) {
    const store = this as Store;
    let chats = store.chatFoldersMap[folder_id];
    if (chats === undefined) {
      chats = [chat_id];
    } else {
      chats.push(chat_id);
    }
    store.chatFoldersMap[folder_id] = chats;
  },
  removeChatFromFolder(folder_id: number, chat_id: number) {
    const chatFoldersMap = (this as Store).chatFoldersMap;
    if (!(folder_id in chatFoldersMap)) {
      return;
    }
    const chats = chatFoldersMap[folder_id];
    chatFoldersMap[folder_id] = chats.filter(id => id !== chat_id);
  },
  selectedChat: null,
  selectChat(id: number | null) {
    const store = this as Store;
    if (id === null) {
      store.selectedChat = null;
    } else {
      const chat = store.chatsMap[id];
      store.selectedChat = chat || null;
    }
  },
  selectedChatKey: '',
  loadingNewMessages: false,
  messagesToLoad: [],
  messagesBubblesToLoad: [],
  scrollTargetMessageId: 0,
  selectedMessage: null,
  lastMessageId: 0,
  currentMessages: [],
  replyToMessage: null,
  replyToQuote: null,
  messageLoaded(messageId: number) {
    const store = this as Store;
    store.messagesToLoad = store.messagesToLoad.filter(m => m !== messageId);
  },
  messageBubbleLoaded(messageId: number) {
    const store = this as Store;
    store.messagesBubblesToLoad = store.messagesBubblesToLoad.filter(m => m !== messageId);
  },
  addMessages(newMessages: Message[]) {
    const store = this as Store;
    const chat = store.selectedChat;
    if (chat === null) {
      return;
    }
    const messages = store.currentMessages;
    for (const message of newMessages) {
      if (message.chat_id !== chat.id) {
        return;
      }
      if (!messages.find(m => m.id === message.id)) {
        store.messagesToLoad.push(message.id);
        store.messagesBubblesToLoad.push(message.id);
        messages.push({
          ...message,
          read: message.id <= chat.last_read_inbox_message_id ||
            (message.sender_id['@type'] === 'messageSenderUser' && message.sender_id.user_id === store.myId)
        });
      }
    }
    messages.sort((m1: MessageWithStatus, m2: MessageWithStatus) => m1.id < m2.id ? -1 : 1);
    store.lastMessageId = messages[messages.length - 1].id;
  },
  clearMessages() {
    const store = this as Store;
    store.currentMessages = [];
    store.messagesToLoad = [];
    store.messagesBubblesToLoad = [];
  },
  deleteMessages(messageIds: number[]) {
    const store = this as Store;
    store.currentMessages = store.currentMessages.filter(m => !messageIds.includes(m.id));
    store.messagesToLoad = store.messagesToLoad.filter(id => !messageIds.includes(id));
    store.messagesBubblesToLoad = store.messagesBubblesToLoad.filter(id => !messageIds.includes(id));
  },
  updateFile(file: File) {
    const store = this as Store;
    store.currentMessages = store.currentMessages.map(m => {
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
    const usersMap = (this as Store).usersMap;
    return usersMap[userId];
  },
  updateUser(user: User) {
    (this as Store).usersMap[user.id] = user;
  },
  updateChatPosition(chatId: number, newPosition: ChatPosition) {
    if (newPosition.list['@type'] !== 'chatListMain') {
      return;
    }

    const store = this as Store;
    const chat = store.chatsMap[chatId];
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

    store.chatsMap[chatId] = {
      ...chat,
      positions: newPositions
    }
  },
  updateChat(chatId: number, chatUpdate: Partial<Chat>) {
    const store = this as Store;
    const chat = store.chatsMap[chatId];
    if (!chat) {
      return;
    }
    const updatedChat = { ...chat, ...chatUpdate };
    store.chatsMap[chatId] = updatedChat;
    if (updatedChat.id === store.selectedChat?.id) {
      store.selectedChat = updatedChat;
    }
  },
  updateSecretChat(chatId: number, chat: SecretChat) {
    const store = this as Store;
    store.secretChatsMap[chatId] = chat;
  },
  async markMessageAsRead(messageId: number) {
    const messages = (this as Store).currentMessages;
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
    const store = this as Store;
    store.currentMessages = store.currentMessages.map(m => m.id === oldMessageId ? { ...message, read: m.read } : m);
    store.messageLoaded(oldMessageId);
    store.messageBubbleLoaded(oldMessageId);
  },
  allReactions: {},
  updateMessageInteractionInfo(message_id: number, info: MessageInteractionInfo) {
    store.currentMessages = store.currentMessages.map(m => m.id === message_id ? { ...m, interaction_info: info } : m);
  },
  aboutModalActive: false,
  toggleAboutModal: function () {
    const store = this as Store;
    store.aboutModalActive = !store.aboutModalActive;
  }
});
