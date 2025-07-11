import { invoke } from "@tauri-apps/api/core";

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
