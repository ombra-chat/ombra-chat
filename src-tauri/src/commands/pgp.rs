use crate::{
    crypto, state,
    store::{self, ChatConfig},
};
use pgp::types::{KeyDetails, PublicKeyTrait};
use serde::{Deserialize, Serialize};
use std::{
    fs::{self, File},
    io::Write,
};

#[tauri::command]
pub fn get_my_key_fingerprint<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
) -> Result<String, String> {
    let key = state::get_my_key(&app).map_err(|e| e.to_string())?;
    Ok(key.public_key().fingerprint().to_string())
}

#[tauri::command]
pub fn export_secret_key<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<(), String> {
    let secret_key = state::get_my_key(&app).map_err(|e| e.to_string())?;
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
    let secret_key = state::get_my_key(&app).map_err(|e| e.to_string())?;
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
    let target_key_file =
        crypto::pgp::get_chat_key_path(&app, chat_id).map_err(|e| e.to_string())?;
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

#[tauri::command]
pub fn create_pgp_text_file<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    text: &str,
    chat_id: i64,
) -> Result<String, String> {
    let target_path =
        crypto::pgp::get_pgp_file_path(&app, ".txt.pgp").map_err(|e| e.to_string())?;
    let keys = crypto::pgp::get_chat_encryption_keys(&app, chat_id).map_err(|e| e.to_string())?;
    crypto::pgp::encrypt_string_to_file(keys, text, &target_path).map_err(|e| e.to_string())?;
    Ok(target_path)
}

#[tauri::command]
pub fn create_pgp_file<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
    chat_id: i64,
) -> Result<String, String> {
    let target_path = crypto::pgp::get_pgp_file_path(&app, ".pgp").map_err(|e| e.to_string())?;
    let keys = crypto::pgp::get_chat_encryption_keys(&app, chat_id).map_err(|e| e.to_string())?;
    crypto::pgp::encrypt_file_to_file(keys, path, &target_path).map_err(|e| e.to_string())?;
    Ok(target_path)
}

#[tauri::command]
pub fn decrypt_file_to_string<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<String, String> {
    let plaintext = crypto::pgp::decrypt_file_to_string(&app, path).map_err(|e| e.to_string())?;
    Ok(plaintext)
}

#[tauri::command]
pub fn decrypt_file<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    path: &str,
) -> Result<String, String> {
    let target_path = crypto::pgp::decrypt_file_to_file(&app, path).map_err(|e| e.to_string())?;
    Ok(target_path)
}

#[tauri::command]
pub fn encrypt_string<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    plaintext: &str,
    chat_id: i64,
) -> Result<String, String> {
    let keys = crypto::pgp::get_chat_encryption_keys(&app, chat_id).map_err(|e| e.to_string())?;
    Ok(crypto::pgp::encrypt_string_to_string(keys, plaintext).map_err(|e| e.to_string())?)
}

#[tauri::command]
pub fn decrypt_string<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    ciphertext: &str,
) -> Result<String, String> {
    Ok(crypto::pgp::decrypt_string_to_string(&app, ciphertext).map_err(|e| e.to_string())?)
}
