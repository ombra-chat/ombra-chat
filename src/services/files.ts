import { invoke } from "@tauri-apps/api/core";
import { File } from '../model';

export async function downloadFile(fileId: number): Promise<File | null> {
  try {
    return await invoke('download_file', { fileId });
  } catch (err) {
    console.error(err);
    return null;
  }
}

export async function getPhoto(path: string) : Promise<string> {
  return await invoke<string>('get_photo', { path });
}
