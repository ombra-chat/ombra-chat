use crate::model::File;
use crate::state;
use std::{fs, path::Path};

#[tauri::command]
pub async fn download_file<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    file_id: i32,
) -> Result<File, String> {
    tdlib::functions::download_file(file_id, 1, 0, 0, false, state::get_client_id(&app))
        .await
        .map(|tdlib::enums::File::File(f)| File::from(&f))
        .map_err(|e| e.message)
}

#[tauri::command]
pub fn save_file(from_path: &str, to_path: &str) -> Result<(), String> {
    let from = Path::new(from_path);
    let to = Path::new(to_path);
    fs::copy(from, to).map_err(|e| e.to_string())?;
    Ok(())
}
