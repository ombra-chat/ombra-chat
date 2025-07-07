use crate::telegram;

#[tauri::command]
pub fn start_telegram_client<R: tauri::Runtime>(app: tauri::AppHandle<R>) {
    let client = telegram::client::Client::new(app);
    client.start();
}
