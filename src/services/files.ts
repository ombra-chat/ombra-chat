import { invoke } from "@tauri-apps/api/core";
import { File } from '../model';

export function getFileName(path: string): string {
  return path.split('/').pop()?.split('\\').pop()!;
}

export async function downloadFile(fileId: number): Promise<File | null> {
  try {
    return await invoke('download_file', { fileId });
  } catch (err) {
    console.error(err);
    return null;
  }
}

export async function saveFile(from: string, to: string) {
  try {
    await invoke('save_file', { fromPath: from, toPath: to });
  } catch (err) {
    console.error(err);
  }
}
