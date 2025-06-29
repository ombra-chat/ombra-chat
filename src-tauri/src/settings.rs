use std::{env, error::Error, fs, path::PathBuf};
use tauri_plugin_store::StoreExt;

use crate::crypto::{self, gpg};

pub fn get_default_ombra_chat_folder() -> Option<PathBuf> {
    if let Some(home_dir) = env::home_dir() {
        let mut gpg_path = home_dir;
        gpg_path.push(".ombra-chat");
        Some(gpg_path)
    } else {
        None
    }
}

#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InitialConfigCheckResult {
    default_folder: String,
}

pub fn check_initial_config() -> InitialConfigCheckResult {
    let default_folder = get_default_ombra_chat_folder()
        .map(|path| path.to_string_lossy().into_owned())
        .unwrap_or_else(|| String::new());

    InitialConfigCheckResult { default_folder }
}

pub fn save_initial_config<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    api_id: &str,
    api_hash: &str,
    folder: &str,
    gpg_password: &str,
    encrypt_db: bool,
) -> Result<(), Box<dyn Error>> {
    gpg::check_secret_key(app, gpg_password)?;

    let mut encrypted_password: Option<String> = None;
    if encrypt_db {
        let password = crypto::utils::generate_random_password();
        let password = gpg::encrypt_user_secret(app, &password)?;
        encrypted_password = Some(password);
    }

    // Create the app directory if it doesn't exist
    fs::create_dir_all(folder)?;

    // Save configuration
    let store = app.store("store.json")?;
    store.set("api-id", api_id);
    store.set("api-hash", api_hash);
    store.set("application-folder", folder);
    store.set("encrypt-database", encrypt_db);
    store.set("database-encrypted-password", encrypt_db);
    if encrypt_db {
        store.set("encrypted-password", encrypted_password);
    }
    store.set("initial-config-done", true);

    Ok(())
}
