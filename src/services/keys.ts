import { invoke } from "@tauri-apps/api/core";
import { PublicKeyFingerprints } from "../model";

export async function getMyKeyFingerprint(): Promise<string> {
  try {
    return await invoke<string>('get_my_key_fingerprint');
  } catch (err) {
    console.error(err);
    return '';
  }
}

export async function exportSecretKey(path: string): Promise<void> {
  try {
    await invoke<string>('export_secret_key', { path });
  } catch (err) {
    console.error(err);
  }
}

export async function exportPublicKey(path: string): Promise<void> {
  try {
    await invoke<string>('export_public_key', { path });
  } catch (err) {
    console.error(err);
  }
}

export async function loadPublicKey(path: string): Promise<PublicKeyFingerprints> {
  return await invoke<PublicKeyFingerprints>('load_public_key', { path });
}

export async function saveChatKey(keyFile: string, encryptionKeyFingerprint: string, chatId: number): Promise<void> {
  await invoke('save_chat_key', { keyFile, encryptionKeyFingerprint, chatId });
}

export async function getChatKey(chatId: number): Promise<string> {
  return await invoke<string | null>('get_chat_key', { chatId }) || '';
}

export async function removeChatKey(chatId: number): Promise<void> {
  await invoke('remove_chat_key', { chatId });
}
