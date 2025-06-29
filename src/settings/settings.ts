import { load } from '@tauri-apps/plugin-store';

const store = await load('store.json', { autoSave: false });

export async function isInitialConfigDone(): Promise<boolean> {
    return (await store.get<boolean>('initial-config-done')) || false;
}
