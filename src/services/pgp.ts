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

export async function createPgpTextFile(text: string, chatId: number): Promise<string | null> {
  try {
    return await invoke<string>('create_pgp_text_file', { text, chatId });
  } catch (err) {
    console.error(err);
    return null;
  }
}

export async function createPgpFile(path: string, chatId: number): Promise<string | null> {
  try {
    return await invoke<string>('create_pgp_file', { path, chatId });
  } catch (err) {
    console.error(err);
    return null;
  }
}

export async function decryptFileToString(path: string): Promise<string> {
  return await invoke<string>('decrypt_file_to_string', { path });
}

export async function decryptFile(path: string): Promise<string> {
  return await invoke<string>('decrypt_file', { path });
}

export async function encryptNameAndCaption(fileName: string, caption: string | null, chatId: number): Promise<string> {
  let cipherFileName = await invoke<string>('encrypt_string', { plaintext: fileName, chatId });
  if (caption !== null) {
    let cipherCaption = await invoke<string>('encrypt_string', { plaintext: caption, chatId });
    return JSON.stringify([cipherFileName, cipherCaption]);
  }
  return JSON.stringify([cipherFileName]);
}

export async function decryptNameAndCaption(ciphertext: string) {
  const data = JSON.parse(ciphertext) as string[];
  const fileName = await invoke<string>('decrypt_string', { ciphertext: data[0] });
  if (data.length > 1) {
    const caption = await invoke<string>('decrypt_string', { ciphertext: data[1] });
    return { fileName, caption };
  }
  return { fileName, caption: null };
}
