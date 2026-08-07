import { listen } from '@tauri-apps/api/event'
import { Window } from "@tauri-apps/api/window"
import { store } from '../store'
import { Chat, ChatPosition, InputMessageContent, InputMessageReplyTo, Message, MessageContent, RemoveChatFromFolder, SecretChat, UpdateChatReadInbox, UpdateDeleteMessages, File, UpdateMessageReactions, UpdateMessageSendSucceeded } from '../model';
import { invoke } from '@tauri-apps/api/core';
import { getChatKey } from './pgp';

export async function handleChatsUpdates() {
  return [
    await listen<Chat>('update-new-chat', (event) => {
      store.addChat(event.payload);
    }),
    await listen<Message>('update-new-message', (event) => {
      const message = event.payload;
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
    await listen<File>('update-file', (event) => {
      store.updateFile(event.payload);
    }),
    await listen<UpdateDeleteMessages>('update-delete-messages', (event) => {
      const { message_ids, chat_id } = event.payload;
      if (store.selectedChat?.id === chat_id) {
        store.deleteMessages(message_ids);
      }
    }),
    await listen<ChatPosition>('update-chat-position', (event) => {
      const update = event.payload;
      store.updateChat(update.chat_id, (c: Chat) => {
        if (update.pos !== null) {
          c.pos = update.pos;
        }
        return c;
      });
    }),
    await listen<RemoveChatFromFolder>('remove-chat-from-folder', (event) => {
      const update = event.payload;
      store.removeChatFromFolder(update.folder_id, update.chat_id);
    }),
    await listen<UpdateChatReadInbox>('update-chat-read-inbox', (event) => {
      const update = event.payload;
      store.updateChat(update.chat_id, (c: Chat) => {
        c.last_read_inbox_message_id = update.last_read_inbox_message_id;
        c.unread_count = update.unread_count;
        return c;
      });
    }),
    await listen<number>('update-unread-chat-count', async (event) => {
      const count = event.payload;
      const mainWindow = new Window('main');
      await mainWindow.setTitle(count > 0 ? '★ OmbraChat' : 'OmbraChat');
    }),
    await listen<UpdateMessageSendSucceeded>('update-message-send-succeeded', async (event) => {
      const update = event.payload;
      store.updateMessage(update.old_message_id, () => {
        return update.message;
      });
    }),
    await listen<UpdateMessageReactions>('update-message-reactions', async (event) => {
      const update = event.payload;
      if (store.selectedChat?.id === update.chat_id) {
        store.updateMessage(update.message_id, (m: Message) => {
          m.reactions = update.reactions;
          return m;
        });
      }
    }),
    await listen<SecretChat>('update-secret-chat', async (event) => {
      store.updateSecretChat(event.payload);
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

export async function selectChat(id: number, reload: boolean = false) {
  try {
    if (store.selectedChat !== null) {
      if (store.selectedChat.id === id && !reload) {
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

  let result = await invoke<Message[]>('get_chat_history', {
    chatId: chat.id,
    fromMessageId: 0,
    offset: 0,
    limit: 1
  });

  if (result.length !== 1) {
    return null;
  }

  const lastMessage = result[0];

  // check if the last message has been written by myself (handle edge case)
  if (lastMessage.sender_user_id === store.myId) {
    return lastMessage;
  }

  // the last read message
  result = await invoke<Message[]>('get_chat_history', {
    chatId: chat.id,
    fromMessageId: chat.last_read_inbox_message_id,
    offset: -1,
    limit: 1
  });

  if (store.lastMessageId == 0 && result.length == 0) {
    // this happens when the last read message has been deleted
    return lastMessage;
  }

  return result[0];
}

export async function loadPreviousMessages(fromMessage: Message | undefined = undefined) {
  if (!fromMessage) {
    if (store.currentMessages.length === 0) {
      return;
    }
    fromMessage = store.currentMessages[0];
  }
  const messages = await invoke<Message[]>('get_chat_history', {
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
  const messages = await invoke<Message[]>('get_chat_history', {
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
    replyTo,
    content
  });
}

export async function deleteMessage(chatId: number, messageId: number) {
  try {
    await invoke('delete_message', { chatId, messageId, revoke: true });
  } catch (err) {
    console.error(err);
  }
}

export async function viewMessage(chatId: number, messageId: number) {
  return await invoke('view_message', { chatId, messageId });
}

export async function forwardMessage(message: Message, chatId: number, sendCopy: boolean) {
  await invoke<Message>('forward_message', {
    chatId,
    fromChatId: message.chat_id,
    messageId: message.id,
    sendCopy
  });
}

export function getSenderTitle(message: Message): string {
  if (message.sender_user_id !== null) {
    const user = store.getUser(message.sender_user_id);
    if (user) {
      return user.display_text;
    }
  }
  if (message.sender_chat_id !== null) {
    const chat = store.getChat(message.sender_chat_id);
    if (chat) {
      return chat.title;
    }
  }
  return '';
}

export function getForwardedFromTitle(message: Message): string {
  if (message.forwarded_from) {
    if (message.forwarded_from.chat_title) {
      return message.forwarded_from.chat_title;
    }
    if (message.forwarded_from.chat_id) {
      const chat = store.getChat(message.forwarded_from.chat_id);
      if (chat) {
        return chat.title;
      }
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
    return content.text;
  }
  if ('caption' in content && content.caption) {
    return content.caption;
  }
  return null;
}

export async function sharePublicKeyTo(chatId: number) {
  return await invoke<Message>('share_public_key', { chatId });
}
