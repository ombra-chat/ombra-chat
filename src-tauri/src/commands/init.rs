use std::sync::Mutex;

use crate::{
    crypto::gpg,
    settings::{self, InitialConfigCheckResult},
    store, AppState,
};
use pgp::types::KeyDetails;
use tauri::Manager;

#[tauri::command]
pub fn check_initial_config() -> InitialConfigCheckResult {
    settings::check_initial_config()
}

#[tauri::command]
pub fn generate_gpg_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    passphrase: &str,
) -> Result<String, String> {
    let key = gpg::generate_key(passphrase).map_err(|e| e.to_string())?;
    let armored_key = gpg::get_armored_private_key(&key).map_err(|e| e.to_string())?;
    store::set_secret_key(&app, &armored_key);
    Ok(key.fingerprint().to_string())
}

#[tauri::command]
pub fn import_gpg_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    key_path: &str,
    passphrase: &str,
) -> Result<String, String> {
    gpg::import_secret_key_to_store(&app, key_path, passphrase).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn save_initial_config<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    api_id: &str,
    api_hash: &str,
    folder: &str,
    gpg_passphrase: &str,
    encrypt_db: bool,
) -> Result<(), String> {
    settings::save_initial_config(&app, api_id, api_hash, folder, gpg_passphrase, encrypt_db)
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub fn check_gpg_passphrase<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    passphrase: &str,
) -> Result<(), String> {
    let key = gpg::get_my_key(&app).map_err(|e| e.to_string())?;
    gpg::check_secret_key(&key, passphrase).map_err(|e| e.to_string())?;
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    state.gpg_passphrase = passphrase.into();
    Ok(())
}
