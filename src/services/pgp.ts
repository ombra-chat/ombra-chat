import { invoke } from "@tauri-apps/api/core";
import { MessagePgpKey, PublicKeyCompleteInfo, PublicKeyFingerprints } from "../model";

export async function getMyKeyFingerprint(): Promise<PublicKeyFingerprints> {
  return await invoke<PublicKeyFingerprints>('get_my_key_fingerprint');
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

export async function getChatKeyCompleteInfo(chatId: number): Promise<PublicKeyCompleteInfo> {
  return await invoke<PublicKeyCompleteInfo>('get_chat_key_complete_info', { chatId }) || '';
}

export async function removeChatKey(chatId: number): Promise<void> {
  await invoke('remove_chat_key', { chatId });
}

export async function decryptFile(path: string): Promise<string> {
  return await invoke<string>('decrypt_file', { path });
}

export async function decryptPgpTextMessage(documentId: number): Promise<string> {
  return await invoke<string>('decrypt_pgp_text_message', { documentId });
}

export async function decryptPgpFileMessage(documentId: number): Promise<string> {
  return await invoke<string>('decrypt_pgp_file_message', { documentId });
}

export async function downloadPgpKeyFile(documentId: number): Promise<MessagePgpKey> {
  return await invoke<MessagePgpKey>('download_pgp_key_file', { documentId });
}

export async function changeKeyPassphrase(newPassphrase: string): Promise<void> {
  await invoke<void>('change_passphrase', { newPassphrase });
}
