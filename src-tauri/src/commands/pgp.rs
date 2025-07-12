use crate::{
    crypto,
    store::{self, ChatConfig},
};
use pgp::types::{KeyDetails, PublicKeyTrait};
use serde::{Deserialize, Serialize};
use std::{
    fs::{self, File},
    io::Write,
    path::PathBuf,
};

#[tauri::command]
pub fn get_my_key_fingerprint<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
) -> Result<String, String> {
    let key = crypto::pgp::get_my_key(&app).map_err(|e| e.to_string())?;
    Ok(key.public_key().fingerprint().to_string())
}

#[tauri::command]
pub fn export_secret_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<(), String> {
    let secret_key = crypto::pgp::get_my_key(&app).map_err(|e| e.to_string())?;
    let key_data = crypto::pgp::get_armored_private_key(&secret_key).map_err(|e| e.to_string())?;
    let mut file = File::create(path).map_err(|e| e.to_string())?;
    file.write_all(key_data.as_bytes())
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub fn export_public_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<(), String> {
    let secret_key = crypto::pgp::get_my_key(&app).map_err(|e| e.to_string())?;
    let key_data = crypto::pgp::get_armored_public_key(&secret_key).map_err(|e| e.to_string())?;
    let mut file = File::create(path).map_err(|e| e.to_string())?;
    file.write_all(key_data.as_bytes())
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct PublicKeyFingerprints {
    primary: String,
    encryption_keys: Vec<String>,
}

#[tauri::command]
pub fn load_public_key(path: &str) -> Result<PublicKeyFingerprints, String> {
    let key = crypto::pgp::load_public_key_from_file(path).map_err(|e| e.to_string())?;
    let mut encryption_keys = Vec::new();
    for subkey in &key.public_subkeys {
        if subkey.is_encryption_key() {
            encryption_keys.push(subkey.fingerprint().to_string());
        }
    }
    if encryption_keys.len() == 0 {
        return Err("No encryption key found".into());
    }
    Ok(PublicKeyFingerprints {
        primary: key.fingerprint().to_string(),
        encryption_keys,
    })
}

#[tauri::command]
pub fn save_chat_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    key_file: &str,
    encryption_key_fingerprint: &str,
    chat_id: i64,
) -> Result<(), String> {
    let app_folder = store::get_application_folder(&app);
    let mut target_key_file = PathBuf::from(app_folder);
    target_key_file.push("keys");
    fs::create_dir_all(target_key_file.clone()).map_err(|e| e.to_string())?;
    target_key_file.push(format!("{}.key", chat_id));
    fs::copy(key_file, target_key_file).map_err(|e| e.to_string())?;
    store::set_chat_config(
        &app,
        ChatConfig {
            chat_id,
            key: encryption_key_fingerprint.to_string(),
        },
    );
    Ok(())
}

#[tauri::command]
pub fn remove_chat_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
) -> Result<(), String> {
    store::remove_chat_config(&app, chat_id);
    Ok(())
}

#[tauri::command]
pub fn get_chat_key<R: tauri::Runtime>(app: tauri::AppHandle<R>, chat_id: i64) -> Option<String> {
    let config = store::get_chat_config(&app, chat_id);
    config.map(|c| c.key)
}
