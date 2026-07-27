import { invoke } from "@tauri-apps/api/core";
import { File } from '../model';

export function getFileName(path: string): string {
  return path.split('/').pop()?.split('\\').pop()!;
}

export async function downloadFile(file: File | number): Promise<File | null> {
  try {
    let sync = true;
    if (typeof file === 'object') {
      if (file.size > 1_000_000) {
        sync = false;
      }
    }
    return await invoke('download_file', { 
      fileId: typeof file === 'number' ? file : file.id,
      sync
    });
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
