use crate::crypto;
use pgp::types::KeyDetails;
use std::{fs::File, io::Write};

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
