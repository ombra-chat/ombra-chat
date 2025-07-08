mod commands;
mod crypto;
mod settings;
mod store;
mod telegram;

use std::sync::Mutex;
use tauri::Manager;

#[derive(Default)]
struct AppState {
    gpg_passphrase: String,
    client_id: i32,
    close: bool,
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    env_logger::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_store::Builder::new().build())
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            commands::init::check_initial_config,
            commands::init::generate_gpg_key,
            commands::init::import_gpg_key,
            commands::init::save_initial_config,
            commands::init::check_gpg_passphrase,
            commands::init::start_telegram_client,
            commands::login::set_authentication_phone_number,
            commands::login::set_authentication_code,
            commands::login::set_authentication_password,
        ])
        .setup(|app| {
            app.manage(Mutex::new(AppState {
                gpg_passphrase: "".into(),
                client_id: 0,
                close: false,
            }));
            Ok(())
        })
        .on_window_event(|window, event| match event {
            tauri::WindowEvent::CloseRequested { .. } => {
                log::trace!("CloseRequested {}", window.label());
                if window.label() == "main" {
                    set_closing_state(window.app_handle());
                }
            }
            _ => {}
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

fn set_closing_state<R: tauri::Runtime>(app: &tauri::AppHandle<R>) {
    log::trace!("Set closing state");
    let state = app.state::<Mutex<AppState>>();
    let mut state = state.lock().unwrap();
    state.close = true;
}

pub fn should_close<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> bool {
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.close
}
