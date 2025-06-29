mod commands;
mod crypto;
mod settings;

use std::sync::Mutex;
use tauri::Manager;

#[derive(Default)]
struct AppState {
    gpg_password: String,
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_store::Builder::new().build())
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            commands::init::check_initial_config,
            commands::init::generate_gpg_key,
            commands::init::import_gpg_key,
            commands::init::save_initial_config,
            commands::init::check_gpg_password,
        ])
        .setup(|app| {
            app.manage(Mutex::new(AppState {
                gpg_password: "".into(),
            }));
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
