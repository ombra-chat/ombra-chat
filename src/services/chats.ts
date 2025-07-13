import { listen } from '@tauri-apps/api/event'
import { store } from '../store'
import { InputMessageContent, InputMessageReplyTo, Message, Messages, UpdateChatAddedToList, UpdateChatFolders, UpdateDeleteMessages, UpdateFile, UpdateNewChat, UpdateNewMessage } from '../model';
import { getDefaultChatFolder } from '../settings/settings';
import { invoke } from '@tauri-apps/api/core';
import { getChatKey } from './pgp';

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
    await listen<UpdateDeleteMessages>('delete-messages', (event) => {
      const { message_ids, chat_id } = event.payload;
      if (store.selectedChat?.id === chat_id) {
        store.deleteMessages(message_ids);
      }
    }),
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
    const lastMessage = await getLastMessage();
    if (lastMessage) {
      store.addMessages([lastMessage]);
      await loadPreviousMessages(lastMessage);
    }
    store.selectedChatKey = await getChatKey(id);
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
    console.warn('Loaded an unexpected number of messages: {}', result.messages.length);
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
