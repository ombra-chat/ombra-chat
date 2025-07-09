use std::sync::Mutex;
use tauri::Manager;

#[derive(Default)]
pub struct AppState {
    gpg_passphrase: String,
    client_id: i32,
    logged_in: bool,
    close_requested: bool,
}

impl AppState {
    pub fn new() -> Self {
        AppState {
            gpg_passphrase: "".into(),
            client_id: 0,
            logged_in: false,
            close_requested: false,
        }
    }
}

pub fn set_gpg_passphrase<R: tauri::Runtime>(app: &tauri::AppHandle<R>, gpg_passphrase: &str) {
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    state.gpg_passphrase = String::from(gpg_passphrase);
}

pub fn get_gpg_passphrase<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.gpg_passphrase.clone()
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
