mod commands;
mod crypto;
mod settings;
mod state;
mod store;
mod telegram;
use serde::Serialize;
use std::sync::Mutex;
use tauri::{Emitter, Manager};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    env_logger::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_store::Builder::new().build())
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            commands::init::get_default_folder,
            commands::init::generate_gpg_key,
            commands::init::import_gpg_key,
            commands::init::save_initial_config,
            commands::init::check_gpg_passphrase,
            commands::init::start_telegram_client,
            commands::login::set_authentication_phone_number,
            commands::login::set_authentication_code,
            commands::login::set_authentication_password,
            commands::login::is_logged_in,
            commands::login::logout,
            commands::chats::load_chats,
            commands::chats::open_chat,
            commands::chats::close_chat,
            commands::chats::get_chat_history,
        ])
        .setup(|app| {
            app.manage(Mutex::new(state::AppState::new()));
            Ok(())
        })
        .on_window_event(|window, event| match event {
            tauri::WindowEvent::CloseRequested { .. } => {
                log::trace!("CloseRequested {}", window.label());
                if window.label() == "main" {
                    state::request_close(window.app_handle());
                }
            }
            _ => {}
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

pub fn emit<S: Serialize + Clone, R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    event: &str,
    payload: S,
) {
    log::trace!("Emitting event {}", event);
    app.emit(event, payload).unwrap_or_else(|err| {
        log::error!("Error emitting event: {}", err);
    });
}
