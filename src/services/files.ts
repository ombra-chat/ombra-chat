import { invoke } from "@tauri-apps/api/core";
import { File, InputThumbnail } from '../model';

export function getFileName(path: string) {
  return path.split('/').pop()?.split('\\').pop();
}

export async function downloadFile(fileId: number): Promise<File | null> {
  try {
    return await invoke('download_file', { fileId });
  } catch (err) {
    console.error(err);
    return null;
  }
}

export async function getPhoto(path: string): Promise<string> {
  return await invoke<string>('get_photo', { path });
}

export async function createThumbnail(path: string): Promise<InputThumbnail> {
  return await invoke<InputThumbnail>('create_thumbnail', { path });
}

export async function getImageSize(path: string): Promise<{ width: number, height: number } | null> {
  const response = await invoke<[number, number] | null>('get_image_size', { path });
  if (Array.isArray(response)) {
    console.log(response)
    return { width: response[0], height: response[1] }
  } else {
    return null;
  }
}

export async function removeThumbnail(path: string): Promise<void> {
  try {
    await invoke('remove_thumbnail', { path });
  } catch (err) {
    console.error(err);
  }
}

export async function saveFile(from: string, to: string) {
  try {
    await invoke('save_file', { fromPath: from, toPath: to });
  } catch (err) {
    console.error(err);
  }
}
