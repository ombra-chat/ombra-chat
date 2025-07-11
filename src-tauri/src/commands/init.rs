use crate::{crypto, settings, state, store, telegram::client::Client};
use pgp::types::KeyDetails;
use std::thread;

#[tauri::command]
pub fn get_default_folder() -> String {
    settings::get_default_folder()
}

#[tauri::command]
pub fn generate_pgp_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    passphrase: &str,
) -> Result<String, String> {
    let key = crypto::pgp::generate_key(passphrase).map_err(|e| e.to_string())?;
    let armored_key = crypto::pgp::get_armored_private_key(&key).map_err(|e| e.to_string())?;
    store::set_secret_key(&app, &armored_key);
    Ok(key.fingerprint().to_string())
}

#[tauri::command]
pub fn import_pgp_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    key_path: &str,
    passphrase: &str,
) -> Result<String, String> {
    crypto::pgp::import_secret_key_to_store(&app, key_path, passphrase).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn save_initial_config<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    api_id: &str,
    api_hash: &str,
    folder: &str,
    pgp_passphrase: &str,
    encrypt_db: bool,
) -> Result<(), String> {
    settings::save_initial_config(&app, api_id, api_hash, folder, pgp_passphrase, encrypt_db)
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub fn check_pgp_passphrase<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    passphrase: &str,
) -> Result<(), String> {
    let key = crypto::pgp::get_my_key(&app).map_err(|e| e.to_string())?;
    crypto::pgp::check_secret_key(&key, passphrase).map_err(|e| e.to_string())?;
    state::set_pgp_passphrase(&app, passphrase);
    Ok(())
}

#[tauri::command]
pub fn start_telegram_client<R: tauri::Runtime>(app: tauri::AppHandle<R>) {
    thread::spawn(move || {
        let client = Client::new();
        client.start(&app);
    });
}
