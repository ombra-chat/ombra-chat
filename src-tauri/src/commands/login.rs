use crate::telegram::client::get_client_id;

#[tauri::command]
pub async fn set_authentication_phone_number<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    phone_number: &str,
) -> Result<(), String> {
    tdlib::functions::set_authentication_phone_number(
        String::from(phone_number),
        None,
        get_client_id(&app),
    )
    .await
    .map_err(|e| e.message)
}

#[tauri::command]
pub async fn set_authentication_code<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    code: &str,
) -> Result<(), String> {
    tdlib::functions::check_authentication_code(String::from(code), get_client_id(&app))
        .await
        .map_err(|e| e.message)
}

#[tauri::command]
pub async fn set_authentication_password<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    password: &str,
) -> Result<(), String> {
    tdlib::functions::check_authentication_password(String::from(password), get_client_id(&app))
        .await
        .map_err(|e| e.message)
}
