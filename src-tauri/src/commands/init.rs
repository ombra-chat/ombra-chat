use crate::{
    crypto::gpg,
    settings::{self, InitialConfigCheckResult},
};

#[tauri::command]
pub fn check_initial_config() -> InitialConfigCheckResult {
    settings::check_initial_config()
}

#[tauri::command]
pub fn generate_gpg_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    password: &str,
) -> Result<String, String> {
    gpg::generate_gpg_key(&app, password).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn import_gpg_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    key_path: &str,
    password: &str,
) -> Result<String, String> {
    gpg::import_gpg_key(&app, key_path, password).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn save_initial_config<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    api_id: &str,
    api_hash: &str,
    folder: &str,
    gpg_password: &str,
    encrypt_db: bool,
) -> Result<(), String> {
    settings::save_initial_config(&app, api_id, api_hash, folder, gpg_password, encrypt_db)
        .map_err(|e| e.to_string())
}
