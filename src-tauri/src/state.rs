use pgp::composed::{PublicSubkey, SignedSecretKey};
use std::{collections::HashMap, error::Error, sync::Mutex};
use tauri::Manager;

use crate::crypto;

#[derive(Default)]
pub struct AppState {
    my_key: Option<SignedSecretKey>,
    keys_cache: HashMap<i64, PublicSubkey>,
    pgp_passphrase: String,
    client_id: i32,
    logged_in: bool,
    close_requested: bool,
}

impl AppState {
    pub fn new() -> Self {
        AppState {
            my_key: None,
            keys_cache: HashMap::new(),
            pgp_passphrase: "".into(),
            client_id: 0,
            logged_in: false,
            close_requested: false,
        }
    }
}

pub fn set_pgp_passphrase<R: tauri::Runtime>(app: &tauri::AppHandle<R>, pgp_passphrase: &str) {
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    state.pgp_passphrase = String::from(pgp_passphrase);
}

pub fn get_pgp_passphrase<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.pgp_passphrase.clone()
}

pub fn set_client_id<R: tauri::Runtime>(app: &tauri::AppHandle<R>, client_id: i32) {
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    state.client_id = client_id;
}

pub fn get_client_id<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> i32 {
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.client_id
}

pub fn request_close<R: tauri::Runtime>(app: &tauri::AppHandle<R>) {
    log::trace!("Set closing state");
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    state.close_requested = true;
}

pub fn close_requested<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> bool {
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.close_requested
}

pub fn is_logged_in<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> bool {
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.logged_in
}

pub fn set_logged_in<R: tauri::Runtime>(app: &tauri::AppHandle<R>, logged_in: bool) {
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    state.logged_in = logged_in;
}

pub fn get_my_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
) -> Result<SignedSecretKey, Box<dyn Error>> {
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    match &state.my_key {
        Some(key) => {
            return Ok(key.clone());
        }
        None => match crypto::pgp::load_my_key(&app) {
            Ok(key) => {
                state.my_key = Some(key.clone());
                return Ok(key);
            }
            Err(err) => {
                return Err(err);
            }
        },
    }
}

pub fn get_my_encryption_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
) -> Result<PublicSubkey, Box<dyn Error>> {
    let key = get_my_key(app)?;
    Ok(crypto::pgp::get_encryption_key_from_secret_key(&key)?)
}

pub fn get_chat_encryption_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    chat_id: i64,
) -> Result<PublicSubkey, Box<dyn Error>> {
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    let keys = &mut state.keys_cache;
    match keys.get(&chat_id) {
        Some(key) => {
            return Ok(key.clone());
        }
        None => {
            let key_path = crypto::pgp::get_chat_key_path(app, chat_id)?;
            let public_key = crypto::pgp::load_public_key_from_file(&key_path)?;
            let encryption_key = crypto::pgp::get_encryption_key_from_public_key(&public_key)?;
            keys.insert(chat_id, encryption_key.clone());
            return Ok(encryption_key);
        }
    }
}
