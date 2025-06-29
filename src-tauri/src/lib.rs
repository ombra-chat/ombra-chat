mod commands;
mod crypto;
mod settings;

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
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
