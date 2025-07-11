use crate::{state, store, thumbnails};
use base64::Engine;
use image::GenericImageView;
use std::fs;
use std::io::Read;
use std::{fs::File, path::Path};
use tdlib::enums::InputThumbnail;

#[tauri::command]
pub async fn download_file<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    file_id: i32,
) -> Result<tdlib::enums::File, String> {
    tdlib::functions::download_file(file_id, 1, 0, 0, false, state::get_client_id(&app))
        .await
        .map_err(|e| e.message)
}

#[tauri::command]
pub fn get_photo(path: &str) -> Result<String, String> {
    let mut file = File::open(path).map_err(|e| e.to_string())?;
    let mut buffer = Vec::new();
    file.read_to_end(&mut buffer).map_err(|e| e.to_string())?;
    let base64_data = base64::engine::general_purpose::STANDARD.encode(&buffer);
    Ok(format!("data:image/jpeg;base64,{}", base64_data))
}

#[tauri::command]
pub fn get_image_size(path: &str) -> Option<(u32, u32)> {
    let img_path = Path::new(path);
    match image::open(img_path) {
        Ok(img) => {
            return Some(img.dimensions());
        }
        Err(_) => {
            return None;
        }
    }
}

#[tauri::command]
pub fn create_thumbnail<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<InputThumbnail, String> {
    thumbnails::create_thumbnail(&app, path).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn remove_thumbnail<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<(), String> {
    thumbnails::remove_thumbnail(&app, path).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn save_file(from_path: &str, to_path: &str) -> Result<(), String> {
    let from = Path::new(from_path);
    let to = Path::new(to_path);
    fs::copy(from, to).map_err(|e| e.to_string())?;
    Ok(())
}
