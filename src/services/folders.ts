import { invoke } from "@tauri-apps/api/core";
import { Message, UpdateChatAddedToList, UpdateChatFolders, UpdateChatRemovedFromList } from "../model";
import { listen } from "@tauri-apps/api/event";
import { store } from "../store";
import { getDefaultChatFolder } from "../settings/settings";

export async function handleFoldersUpdates() {
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
    await listen<UpdateChatAddedToList>('update-chat-added-to-list', (event) => {
      const update = event.payload;
      if (update.chat_list['@type'] === 'chatListMain') {
        store.addChatToFolder(0, update.chat_id);
      } else if (update.chat_list['@type'] === 'chatListFolder') {
        store.addChatToFolder(update.chat_list.chat_folder_id, update.chat_id);
      }
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
  ]
}

export async function createFolder(title: string, includedChatIds: number[]) {
  await invoke<Message>('create_chat_folder', { title, includedChatIds })
}

export async function deleteFolder(folderId: number) {
  await invoke<Message>('delete_chat_folder', { folderId })
}

export async function renameFolder(folderId: number, newTitle: string) {
  await invoke<Message>('rename_chat_folder', { folderId, newTitle })
}

export async function addChatToFolder(chatId: number, folderId: number) {
  await invoke<Message>('add_chat_to_folder', { chatId, folderId })
}

export async function removeChatFromFolder(chatId: number, folderId: number) {
  await invoke<Message>('remove_chat_from_folder', { chatId, folderId })
}
