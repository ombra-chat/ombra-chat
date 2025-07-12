use serde::{Deserialize, Serialize};
use tauri_plugin_store::StoreExt;

static API_ID: &str = "api-id";
static API_HASH: &str = "api-hash";
static APPLICATION_FOLDER: &str = "application-folder";
static SECRET_KEY: &str = "secret-key";
static ENCRYPT_DATABASE: &str = "encrypt-database";
static ENCRYPTED_DATABASE_PASSWORD: &str = "encrypted-database-password";
static ENCRYPTION_SALT: &str = "encryption-salt";
static INITIAL_CONFIG_DONE: &str = "initial-config-done";
static CHATS: &str = "chats";

fn get_string<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    key: &str,
    default_value: &str,
) -> String {
    if let Some(value) = get_value(app, key) {
        if let Some(value) = value.as_str() {
            return String::from(value);
        }
    }
    String::from(default_value)
}

fn get_int<R: tauri::Runtime>(app: &tauri::AppHandle<R>, key: &str) -> i64 {
    if let Some(value) = get_value(app, key) {
        if let Some(value) = value.as_i64() {
            return value;
        }
    }
    return 0;
}

fn get_bool<R: tauri::Runtime>(app: &tauri::AppHandle<R>, key: &str) -> bool {
    if let Some(value) = get_value(app, key) {
        if let Some(value) = value.as_bool() {
            return value;
        }
    }
    return false;
}

fn get_value<R: tauri::Runtime>(app: &tauri::AppHandle<R>, key: &str) -> Option<serde_json::Value> {
    if let Ok(store) = app.store("store.json") {
        if let Some(value) = store.get(key) {
            return Some(value);
        } else {
            log::trace!("No value found for {}", key);
            return None;
        }
    }
    log::error!("Unable to open data store");
    None
}

fn set_value<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    key: &str,
    value: impl Into<tauri_plugin_store::JsonValue>,
) {
    if let Ok(store) = app.store("store.json") {
        store.set(key, value);
        if let Ok(()) = store.save() {
            return;
        }
    }
    log::warn!("Unable to save {} to store", key);
}

pub fn get_api_id<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> i32 {
    get_int(app, API_ID) as i32
}

pub fn set_api_id<R: tauri::Runtime>(app: &tauri::AppHandle<R>, api_id: i32) {
    set_value(app, API_ID, api_id as i64)
}

pub fn get_api_hash<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    get_string(app, API_HASH, "")
}

pub fn set_api_hash<R: tauri::Runtime>(app: &tauri::AppHandle<R>, api_hash: &str) {
    set_value(app, API_HASH, api_hash)
}

pub fn get_application_folder<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    get_string(app, APPLICATION_FOLDER, ".")
}

pub fn set_application_folder<R: tauri::Runtime>(app: &tauri::AppHandle<R>, folder: &str) {
    set_value(app, APPLICATION_FOLDER, folder)
}

pub fn get_secret_key<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    get_string(app, SECRET_KEY, "")
}

pub fn set_secret_key<R: tauri::Runtime>(app: &tauri::AppHandle<R>, secret_key: &str) {
    set_value(app, SECRET_KEY, secret_key)
}

pub fn get_encrypt_database<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> bool {
    get_bool(app, ENCRYPT_DATABASE)
}

pub fn set_encrypt_database<R: tauri::Runtime>(app: &tauri::AppHandle<R>, encrypt: bool) {
    set_value(app, ENCRYPT_DATABASE, encrypt)
}

pub fn get_encrypted_database_password<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    get_string(app, ENCRYPTED_DATABASE_PASSWORD, "")
}

pub fn set_encrypted_database_password<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    encrypted_password: &str,
) {
    set_value(app, ENCRYPTED_DATABASE_PASSWORD, encrypted_password)
}

pub fn get_encryption_salt<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    get_string(app, ENCRYPTION_SALT, "")
}

pub fn set_encryption_salt<R: tauri::Runtime>(app: &tauri::AppHandle<R>, encryption_salt: &str) {
    set_value(app, ENCRYPTION_SALT, encryption_salt)
}

pub fn set_initial_config_done<R: tauri::Runtime>(app: &tauri::AppHandle<R>, done: bool) {
    set_value(app, INITIAL_CONFIG_DONE, done)
}

#[derive(Clone, Debug, PartialEq, Deserialize, Serialize)]
pub struct ChatConfig {
    pub chat_id: i64,
    pub key: String,
}

pub fn set_chat_config<R: tauri::Runtime>(app: &tauri::AppHandle<R>, new_config: ChatConfig) {
    let mut configs = get_chats_config(app);

    // Remove existing config with the same chat_id
    configs = configs
        .into_iter()
        .filter(|config| config.chat_id != new_config.chat_id)
        .collect();

    configs.push(new_config);

    set_chats_config(app, configs);
}

pub fn remove_chat_config<R: tauri::Runtime>(app: &tauri::AppHandle<R>, chat_id: i64) {
    let mut configs = get_chats_config(app);
    configs = configs
        .into_iter()
        .filter(|config| config.chat_id != chat_id)
        .collect();
    set_chats_config(app, configs);
}

pub fn get_chat_config<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    chat_id: i64,
) -> Option<ChatConfig> {
    let configs = get_chats_config(app);
    configs.into_iter().find(|config| config.chat_id == chat_id)
}

fn get_chats_config<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> Vec<ChatConfig> {
    let chats_config = get_string(app, CHATS, "");
    if chats_config != "" {
        if let Ok(value) = serde_json::from_str(&chats_config) {
            return value;
        }
    }
    return Vec::new();
}

fn set_chats_config<R: tauri::Runtime>(app: &tauri::AppHandle<R>, chat_configs: Vec<ChatConfig>) {
    match serde_json::to_string(&chat_configs) {
        Ok(value) => set_value(app, CHATS, value),
        Err(err) => {
            log::error!("{}", err);
        }
    }
}
