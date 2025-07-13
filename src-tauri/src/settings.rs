use crate::{
    crypto::{self, pgp}, state, store
};
use std::{env, error::Error, fs, path::PathBuf};

pub fn get_default_ombra_chat_folder() -> Option<PathBuf> {
    if let Some(home_dir) = env::home_dir() {
        let mut pgp_path = home_dir;
        pgp_path.push(".ombra-chat");
        Some(pgp_path)
    } else {
        None
    }
}

pub fn get_default_folder() -> String {
    get_default_ombra_chat_folder()
        .map(|path| path.to_string_lossy().into_owned())
        .unwrap_or_else(|| String::new())
}

pub fn save_initial_config<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    api_id: &str,
    api_hash: &str,
    folder: &str,
    pgp_password: &str,
    encrypt_db: bool,
) -> Result<(), Box<dyn Error>> {
    log::trace!("save_initial_config");

    let key = state::get_my_key(app)?;
    pgp::check_secret_key(&key, pgp_password)?;

    let api_id: i32 = api_id.parse::<i32>()?;

    let mut encrypted_password: Option<String> = None;
    if encrypt_db {
        let password = crypto::utils::generate_random_password();
        let key = pgp::get_encryption_key_from_secret_key(&key)?;
        let password = pgp::encrypt_string_to_string(vec![key], &password)?;
        encrypted_password = Some(password.to_string());
    }

    // Create the app directory if it doesn't exist
    fs::create_dir_all(folder)?;

    // Save configuration
    store::set_api_id(app, api_id);
    store::set_api_hash(app, api_hash);
    store::set_application_folder(app, folder);
    store::set_encrypt_database(app, encrypt_db);
    if encrypt_db && encrypted_password.is_some() {
        store::set_encrypted_database_password(app, &encrypted_password.unwrap());
        store::set_encryption_salt(app, &crypto::utils::generate_random_salt());
    }
    store::set_initial_config_done(app, true);

    Ok(())
}

#[derive(Debug)]
pub struct TdlibParameters {
    pub database_directory: String,
    pub api_id: i32,
    pub api_hash: String,
    pub database_encryption_key: String,
}

pub fn get_tdlib_parameters<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
) -> Result<TdlibParameters, Box<dyn Error>> {
    log::trace!("get_tdlib_parameters");

    let application_folder = store::get_application_folder(app);

    let mut database_directory = PathBuf::from(application_folder);
    database_directory.push("tdlib");
    let database_directory = database_directory.to_string_lossy().to_string();

    // Create the database directory if it doesn't exist
    fs::create_dir_all(database_directory.to_string())?;

    let api_id = store::get_api_id(app);
    let api_hash = store::get_api_hash(app);

    let encrypt_database = store::get_encrypt_database(app);

    let mut database_encryption_key = String::new();
    if encrypt_database {
        let encrypted_password = store::get_encrypted_database_password(app);
        let password = pgp::decrypt_string_to_string(app, &encrypted_password)?;
        let salt = store::get_encryption_salt(app);
        database_encryption_key = crypto::utils::generate_derived_key(&password, &salt)?;
    }

    let params = TdlibParameters {
        database_directory,
        api_id,
        api_hash,
        database_encryption_key,
    };
    log::debug!("TdlibParameters: {:#?}", params);
    Ok(params)
}
