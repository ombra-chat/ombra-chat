use crate::{state, store};
use base64::Engine;
use std::io::Read;
use std::{fs::File, path::Path};

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
pub fn get_photo<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<String, String> {
    let folder = store::get_application_folder(&app);
    if !is_path_in_parent(path, &folder) {
        return Err("Path outside application folder".into());
    }
    let mut file = File::open(path).map_err(|e| e.to_string())?;
    let mut buffer = Vec::new();
    file.read_to_end(&mut buffer).map_err(|e| e.to_string())?;
    let base64_data = base64::engine::general_purpose::STANDARD.encode(&buffer);
    Ok(format!("data:image/jpeg;base64,{}", base64_data))
}

fn is_path_in_parent(path: &str, parent: &str) -> bool {
    let path = Path::new(path);
    let parent = Path::new(parent);

    // Normalize the paths to avoid issues with different representations
    let path = path.canonicalize().ok();
    let parent = parent.canonicalize().ok();

    match (path, parent) {
        (Some(path), Some(parent)) => {
            // Check if the path starts with the parent path
            path.starts_with(&parent)
        }
        _ => false, // Return false if either path cannot be normalized
    }
}
