import { Theme } from '@tauri-apps/api/window';
import { LazyStore } from '@tauri-apps/plugin-store';

const store = new LazyStore('store.json', { autoSave: false });

export async function isInitialConfigDone(): Promise<boolean> {
    return (await store.get<boolean>('initial-config-done')) || false;
}

export async function getDefaultChatFolder(): Promise<number> {
    return (await store.get<number>('default-chat-folder')) || 0;
}

export async function setDefaultChatFolder(folderId: number): Promise<void> {
    await store.set('default-chat-folder', folderId);
    await store.save();
}

export async function setTheme(theme: Theme): Promise<void> {
    await store.set('theme', theme);
    await store.save();
}

export async function getTheme(): Promise<Theme | undefined> {
    return await store.get<Theme>('theme');
}
