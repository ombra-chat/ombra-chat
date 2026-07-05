import { invoke } from "@tauri-apps/api/core";
import { ChatFolder, Message, UpdateChatAddedToFolder, UpdateChatRemovedFromFolder } from "../model";
import { listen } from "@tauri-apps/api/event";
import { store } from "../store";
import { getDefaultChatFolder } from "../settings/settings";

export async function handleFoldersUpdates() {
  return [
    await listen<ChatFolder[]>('update-chat-folders', async (event) => {
      store.chatFolders = event.payload;
      const defaultChatFolder = await getDefaultChatFolder();
      store.selectedChatFolderId = store.chatFolders.find(f => f.id === defaultChatFolder)?.id || 0;
    }),
    await listen<UpdateChatAddedToFolder>('update-chat-added-to-folder', (event) => {
      const update = event.payload;
      store.addChatToFolder(update.folder_id, update.chat_id);
    }),
    await listen<UpdateChatRemovedFromFolder>('update-chat-removed-from-folder', async (event) => {
      const update = event.payload;
      store.removeChatFromFolder(update.folder_id, update.chat_id);
      if (update.folder_id === 0) {
        store.deleteChat(update.chat_id);
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
