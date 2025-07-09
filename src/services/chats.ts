import { listen } from '@tauri-apps/api/event'
import { store } from '../store'
import { UpdateChatAddedToList, UpdateChatFolders, UpdateNewChat } from '../model';
import { getDefaultChatFolder } from '../settings/settings';

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
      store.addChat({
        id: chat.id,
        title: chat.title,
        permissions: chat.permissions
      });
    }),
    await listen<UpdateChatAddedToList>('update-chat-added-to-list', (event) => {
      const update = event.payload;
      if (update.chat_list['@type'] === 'chatListMain') {
        store.addChatToFolder(0, update.chat_id);
      } else if (update.chat_list['@type'] === 'chatListFolder') {
        store.addChatToFolder(update.chat_list.chat_folder_id, update.chat_id);
      }
    }),
  ]
}