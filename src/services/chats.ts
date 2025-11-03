import { listen } from '@tauri-apps/api/event'
import { Window } from "@tauri-apps/api/window"
import { store } from '../store'
import { Chat, InputMessageContent, InputMessageReplyTo, Message, MessageContent, Messages, MessageSender, UpdateChatAddedToList, UpdateChatFolders, UpdateChatLastMessage, UpdateChatPosition, UpdateChatReadInbox, UpdateChatRemovedFromList, UpdateDeleteMessages, UpdateFile, UpdateMessageInteractionInfo, UpdateMessageSendSucceeded, UpdateNewChat, UpdateNewMessage, UpdateSecretChat, UpdateUnreadChatCount } from '../model';
import { getDefaultChatFolder } from '../settings/settings';
import { invoke } from '@tauri-apps/api/core';
import { getChatKey } from './pgp';
import { getUserDisplayText } from './users';

export async function handleChatsUpdates() {
  return [
    await listen<UpdateChatFolders>('update-chat-folders', async (event) => {
      const { chat_folders } = event.payload;
      store.chatFolders = [
        { id: 0, name: 'Main' },
        ...chat_folders.map(f => ({ id: f.id, name: f.name.text.text }))
      ];
      const defaultChatFolder = await getDefaultChatFolder();
      store.selectedChatFolderId = store.chatFolders.find(f => f.id === defaultChatFolder)?.id || 0;
    }),
    await listen<UpdateNewChat>('update-new-chat', (event) => {
      const { chat } = event.payload;
      store.addChat(chat);
    }),
    await listen<UpdateChatAddedToList>('update-chat-added-to-list', (event) => {
      const update = event.payload;
      if (update.chat_list['@type'] === 'chatListMain') {
        store.addChatToFolder(0, update.chat_id);
      } else if (update.chat_list['@type'] === 'chatListFolder') {
        store.addChatToFolder(update.chat_list.chat_folder_id, update.chat_id);
      }
    }),
    await listen<UpdateNewMessage>('update-new-message', (event) => {
      const { message } = event.payload;
      const selectedChat = store.selectedChat;
      if (!selectedChat) {
        return;
      }
      if (selectedChat.unread_count > 0) {
        // ignore new messages if there are unread messages on the chat
        return;
      }
      store.addMessages([message]);
    }),
    await listen<UpdateFile>('update-file', (event) => {
      const { file } = event.payload;
      store.updateFile(file);
    }),
    await listen<UpdateDeleteMessages>('update-delete-messages', (event) => {
      const { message_ids, chat_id } = event.payload;
      if (store.selectedChat?.id === chat_id) {
        store.deleteMessages(message_ids);
      }
    }),
    await listen<UpdateChatPosition>('update-chat-position', (event) => {
      const update = event.payload;
      if (update.position.list['@type'] === 'chatListFolder') {
        if (update.position.order === 0) {
          store.removeChatFromFolder(update.position.list.chat_folder_id, update.chat_id);
          return;
        }
      }
      store.updateChatPosition(update.chat_id, update.position);
    }),
    await listen<UpdateChatLastMessage>('update-chat-last-message', (event) => {
      const update = event.payload;
      store.updateChat(update.chat_id, { positions: update.positions, last_message: update.last_message });
    }),
    await listen<UpdateChatReadInbox>('update-chat-read-inbox', (event) => {
      const update = event.payload;
      store.updateChat(update.chat_id, {
        last_read_inbox_message_id: update.last_read_inbox_message_id,
        unread_count: update.unread_count
      });
    }),
    await listen<UpdateUnreadChatCount>('update-unread-chat-count', async (event) => {
      const update = event.payload;
      if (update.chat_list['@type'] === 'chatListMain') {
        const unreadChats = update.unread_unmuted_count > 0;
        const mainWindow = new Window('main');
        await mainWindow.setTitle(unreadChats ? '★ OmbraChat' : 'OmbraChat');
      }
    }),
    await listen<UpdateMessageSendSucceeded>('update-message-send-succeeded', async (event) => {
      const update = event.payload;
      store.updateMessage(update.old_message_id, update.message);
    }),
    await listen<UpdateChatRemovedFromList>('update-chat-removed-from-list', async (event) => {
      const update = event.payload;
      if (update.chat_list['@type'] === 'chatListMain') {
        store.removeChatFromFolder(0, update.chat_id);
        store.deleteChat(update.chat_id);
      } else if (update.chat_list['@type'] === 'chatListFolder') {
        store.removeChatFromFolder(update.chat_list.chat_folder_id, update.chat_id);
      }
    }),
    await listen<UpdateMessageInteractionInfo>('update-message-interaction-info', async (event) => {
      const update = event.payload;
      if (store.selectedChat?.id === update.chat_id) {
        store.updateMessageInteractionInfo(update.message_id, update.interaction_info);
      }
    }),
    await listen<UpdateSecretChat>('update-secret-chat', async (event) => {
      const { secret_chat } = event.payload;
      store.updateSecretChat(secret_chat.id, secret_chat);
    })
  ]
}

export async function loadChats() {
  let chatsLoaded = false;
  while (!chatsLoaded) {
    try {
      await invoke('load_chats');
    } catch (err) {
      chatsLoaded = true;
    }
  }
}

export async function selectChat(id: number) {
  try {
    if (store.selectedChat !== null) {
      if (store.selectedChat.id === id) {
        return;
      }
      await invoke('close_chat', { id: store.selectedChat.id })
    }
    await invoke('open_chat', { id });
    store.clearMessages();
    store.lastMessageId = 0;
    store.selectChat(id);
    store.selectedChatKey = await getChatKey(id);
    store.loadingNewMessages = true;
    const lastMessage = await getLastMessage();
    if (lastMessage) {
      store.scrollTargetMessageId = lastMessage.id;
      store.addMessages([lastMessage]);
      await loadPreviousMessages(lastMessage);
    }
  } catch (err) {
    console.error(err);
  }
}

export async function closeCurrentChat() {
  try {
    if (store.selectedChat === null) {
      return;
    }
    await invoke('close_chat', { id: store.selectedChat.id })
    store.selectChat(null);
  } catch (err) {
    console.error(err);
  }
}

/**
 * Retrieve only the last message; this is done because in some cases tdlib sends only
 * one message in any case at the first load, so it is better to always expect to receive
 * only one message when the chat is opened, in order to handle message loading in a more
 * deterministic way; next messages are requested in chunks of 20 or 10 messages
 */
async function getLastMessage(): Promise<Message | null> {
  const chat = store.selectedChat;
  if (chat === null) {
    return null;
  }

  let result = await invoke<Messages>('get_chat_history', {
    chatId: chat.id,
    fromMessageId: 0,
    offset: 0,
    limit: 1
  });

  if (result.messages.length !== 1) {
    return null;
  }

  const lastMessage = result.messages[0];

  // check if the last message has been written by myself (handle edge case)
  if (lastMessage.sender_id['@type'] === 'messageSenderUser' && lastMessage.sender_id.user_id === store.myId) {
    return lastMessage;
  }

  // the last read message
  result = await invoke<Messages>('get_chat_history', {
    chatId: chat.id,
    fromMessageId: chat.last_read_inbox_message_id,
    offset: -1,
    limit: 1
  });

  if (store.lastMessageId == 0 && result.messages.length == 0) {
    // this happens when the last read message has been deleted
    return lastMessage;
  }

  return result.messages[0];
}

export async function loadPreviousMessages(fromMessage: Message | undefined = undefined) {
  if (!fromMessage) {
    if (store.currentMessages.length === 0) {
      return;
    }
    fromMessage = store.currentMessages[0];
  }
  const { messages } = await invoke<Messages>('get_chat_history', {
    chatId: fromMessage.chat_id,
    fromMessageId: fromMessage.id,
    offset: 0,
    limit: 20
  });
  store.addMessages(messages);
}

export async function loadNewMessages() {
  const chat = store.selectedChat;
  if (!chat || chat.unread_count === 0) {
    return;
  }
  const fromMessageId = store.lastMessageId;
  const { messages } = await invoke<Messages>('get_chat_history', {
    chatId: chat.id,
    fromMessageId,
    offset: -5,
    limit: 5
  });
  store.addMessages(messages);
}

export async function sendMessage(chatId: number, replyTo: InputMessageReplyTo | null, content: InputMessageContent): Promise<Message> {
  return await invoke<Message>('send_message', {
    chatId,
    messageThreadId: 0,
    replyTo,
    options: null,
    replyMarkup: null,
    inputMessageContent: content
  });
}

export async function deleteMessage(chatId: number, messageId: number) {
  try {
    await invoke('delete_message', { chatId, messageId, revoke: true });
  } catch (err) {
    console.error(err);
  }
}

export function getChatPosition(chat: Chat) {
  for (const pos of chat.positions) {
    if (pos.list['@type'] === 'chatListMain') {
      return pos.order;
    }
  }
  return 0;
}

export async function viewMessage(chatId: number, messageId: number) {
  return await invoke('view_message', { chatId, messageId });
}

export async function forwardMessage(message: Message, chatId: number, sendCopy: boolean) {
  await invoke<Messages>('forward_message', {
    chatId,
    messageThreadId: 0,
    fromChatId: message.chat_id,
    messageId: message.id,
    sendCopy
  });
}
export function getSenderTitle(sender: MessageSender): string {
  if (sender['@type'] === 'messageSenderUser') {
    return getUserDisplayText(sender.user_id);
  } else if (sender['@type'] === 'messageSenderChat') {
    const chat = store.getChat(sender.chat_id);
    if (chat) {
      return chat.title;
    }
  }
  return '';
}

export async function getRepliedMessage(chatId: number, messageId: number): Promise<Message | null> {
  try {
    return await invoke<Message>('get_replied_message', {
      chatId,
      messageId
    });
  } catch (_) {
    return null;
  }
}

export async function createNewSecretChat(userId: number) {
  const chat = await invoke<Chat>('create_new_secret_chat', { userId });
  store.addChat(chat);
  return chat;
}

export async function deleteChat(chatId: number) {
  await invoke('delete_chat', { chatId });
  store.deleteChat(chatId);
}

export function getMessageTextContent(content: MessageContent): string | null {
  if (content['@type'] === 'messageText') {
    return content.text.text;
  }
  if (content['@type'] === 'messagePhoto') {
    if (content.caption != null) {
      return content.caption.text;
    }
    return null;
  }
  if (content['@type'] === 'messageDocument') {
    if (content.caption != null) {
      return content.caption.text;
    }
    return null;
  }
  if (content['@type'] === 'messageVideo') {
    if (content.caption != null) {
      return content.caption.text;
    }
    return null;
  }
  return null;
}

export async function sharePublicKeyTo(chatId: number) {
  return await invoke<Message>('share_public_key', { chatId });
}
