use crate::crypto::utils;
use crate::{state, store};
use pgp::composed::{
    ArmorOptions, Deserializable, KeyType, Message, MessageBuilder, PublicSubkey,
    SecretKeyParamsBuilder, SignedPublicKey, SignedSecretKey, SubkeyParamsBuilder,
};
use pgp::crypto::sym::SymmetricKeyAlgorithm;
use pgp::types::{KeyDetails, PublicKeyTrait};
use rand::thread_rng;
use std::fs;
use std::io::{Cursor, Read, Write};
use std::path::PathBuf;
use std::{error::Error, fs::File, io::BufReader};

fn encrypt(
    keys: Vec<PublicSubkey>,
    bytes: impl Into<pgp::bytes::Bytes>,
) -> Result<Vec<u8>, Box<dyn Error>> {
    log::trace!("encrypt");
    let mut rng = rand::thread_rng();
    let mut builder =
        MessageBuilder::from_bytes("", bytes).seipd_v1(&mut rng, SymmetricKeyAlgorithm::default());
    for key in keys {
        builder.encrypt_to_key(&mut rng, &key)?;
    }
    let mut data = vec![];
    builder.to_writer(&mut rng, &mut data)?;
    Ok(data)
}

pub fn encrypt_string_to_string(
    keys: Vec<PublicSubkey>,
    input: &str,
) -> Result<String, Box<dyn Error>> {
    log::trace!("encrypt_string_to_string");
    let bytes = pgp::bytes::Bytes::from(input.to_string());
    encrypt_to_string(keys, bytes)
}

fn encrypt_to_string(
    keys: Vec<PublicSubkey>,
    input: impl Into<pgp::bytes::Bytes>,
) -> Result<String, Box<dyn Error>> {
    log::trace!("encrypt_to_string");
    let data = encrypt(keys, input)?;
    let armored = MessageBuilder::from_bytes("", data)
        .to_armored_string(thread_rng(), ArmorOptions::default())?;
    Ok(armored)
}

pub fn encrypt_string_to_file(
    keys: Vec<PublicSubkey>,
    input: &str,
    target_path: &str,
) -> Result<(), Box<dyn Error>> {
    log::trace!("encrypt_string_to_file");
    let bytes = pgp::bytes::Bytes::from(input.to_string());
    let data = encrypt(keys, bytes)?;
    let mut file = File::create(target_path)?;
    file.write_all(&data)?;
    Ok(())
}

pub fn encrypt_file_to_file(
    keys: Vec<PublicSubkey>,
    source_path: &str,
    target_path: &str,
) -> Result<(), Box<dyn Error>> {
    log::trace!("encrypt_file_to_file");
    let source_file = File::open(source_path)?;
    let mut buf = Vec::new();
    let mut reader = BufReader::new(source_file);
    reader.read_to_end(&mut buf)?;
    let data = encrypt(keys, buf)?;
    let mut target_file = File::create(target_path)?;
    target_file.write_all(&data)?;
    Ok(())
}

fn decrypt_armored(
    key: &SignedSecretKey,
    password: &str,
    input: Vec<u8>,
) -> Result<Vec<u8>, Box<dyn Error>> {
    log::trace!("decrypt_armored");
    let buf = Cursor::new(input);
    let mut dearmored = Message::from_reader(buf)?.0;
    let data = dearmored.as_data_vec()?;
    let buf = Cursor::new(data);
    let message = Message::from_bytes(buf)?;
    let mut decrypted = message.decrypt(&password.into(), key)?;
    Ok(decrypted.as_data_vec()?)
}

pub fn decrypt_file_to_string<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    path: &str,
) -> Result<String, Box<dyn Error>> {
    log::trace!("decrypt_file_to_string: {}", path);
    let key = state::get_my_key(app)?;
    let passphrase = state::get_pgp_passphrase(app);
    let source_file = File::open(path)?;
    let mut data = Vec::new();
    let mut reader = BufReader::new(source_file);
    reader.read_to_end(&mut data)?;
    let buf = Cursor::new(data);
    let message = Message::from_bytes(buf)?;
    let mut decrypted = message.decrypt(&passphrase.into(), &key)?;
    let mut plaintext = String::new();
    decrypted.read_to_string(&mut plaintext)?;
    Ok(plaintext)
}

pub fn decrypt_file_to_file<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    path: &str,
) -> Result<String, Box<dyn Error>> {
    log::trace!("decrypt_file_to_file: {}", path);
    let key = state::get_my_key(app)?;
    let passphrase = state::get_pgp_passphrase(app);
    let source_file = File::open(path)?;
    let mut data = Vec::new();
    let mut reader = BufReader::new(source_file);
    reader.read_to_end(&mut data)?;
    let buf = Cursor::new(data);
    let message = Message::from_bytes(buf)?;
    let mut decrypted = message.decrypt(&passphrase.into(), &key)?;
    let target_path = &path[..path.len() - 4];
    let mut file = File::create(target_path)?;
    file.write_all(&decrypted.as_data_vec()?)?;
    Ok(target_path.to_string())
}

pub fn decrypt_string_to_string<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    armored_input: &str,
) -> Result<String, Box<dyn Error>> {
    log::trace!("decrypt_string_to_string");
    let key = state::get_my_key(app)?;
    let passphrase = state::get_pgp_passphrase(app);
    let data = decrypt_armored(&key, &passphrase, armored_input.to_string().into_bytes())?;
    Ok(String::from_utf8(data)?)
}

pub fn generate_key(passphrase: &str) -> Result<SignedSecretKey, Box<dyn Error>> {
    log::trace!("generate_key");
    let key_params = SecretKeyParamsBuilder::default()
        .key_type(KeyType::Ed25519)
        .can_certify(true)
        .can_sign(true)
        .passphrase(Some(passphrase.into()))
        .subkey(
            SubkeyParamsBuilder::default()
                .key_type(KeyType::X25519)
                .can_encrypt(true)
                .build()?,
        )
        .build()?;

    let mut rng: rand::prelude::ThreadRng = rand::thread_rng();
    let key = key_params
        .generate(&mut rng)?
        .sign(&mut rng, &passphrase.into())?;
    Ok(key)
}

pub fn get_encryption_key_from_secret_key(
    key: &SignedSecretKey,
) -> Result<PublicSubkey, Box<dyn Error>> {
    log::trace!("get_encryption_key_from_secret_key");
    for subkey in &key.secret_subkeys {
        if subkey.public_key().is_encryption_key() {
            return Ok(subkey.public_key());
        }
    }
    Err(format!("No encryption key found for {}", key.fingerprint()).into())
}

pub fn get_encryption_key_from_public_key(
    key: &SignedPublicKey,
) -> Result<PublicSubkey, Box<dyn Error>> {
    log::trace!("get_encryption_key_from_public_key");
    for subkey in &key.public_subkeys {
        if subkey.is_encryption_key() {
            return Ok(subkey.as_unsigned());
        }
    }
    Err(format!("No encryption key found for {}", key.fingerprint()).into())
}

pub fn get_armored_private_key(secret_key: &SignedSecretKey) -> Result<String, Box<dyn Error>> {
    log::trace!("get_armored_private_key");
    let key = secret_key.to_armored_string(ArmorOptions::default())?;
    Ok(key)
}

pub fn get_armored_public_key(secret_key: &SignedSecretKey) -> Result<String, Box<dyn Error>> {
    log::trace!("get_armored_public_key");
    let key = secret_key
        .signed_public_key()
        .to_armored_string(ArmorOptions::default())?;
    Ok(key)
}

pub fn load_public_key(armored: &str) -> Result<SignedPublicKey, Box<dyn Error>> {
    log::trace!("load_public_key");
    let key = SignedPublicKey::from_string(armored)?.0;
    Ok(key)
}

pub fn load_public_key_from_file(key_path: &str) -> Result<SignedPublicKey, Box<dyn Error>> {
    log::trace!("load_public_key_from_file");
    let file = File::open(key_path)?;
    let mut reader = BufReader::new(file);
    let mut armored_data = String::new();
    reader.read_to_string(&mut armored_data)?;
    Ok(load_public_key(&armored_data)?)
}

pub fn check_secret_key(
    secret_key: &SignedSecretKey,
    passphrase: &str,
) -> Result<(), Box<dyn Error>> {
    log::trace!("check_secret_key");
    secret_key.unlock(&passphrase.into(), |_, _| Ok(()))??;
    Ok(())
}

pub fn import_secret_key_to_store<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    key_path: &str,
    passphrase: &str,
) -> Result<String, Box<dyn Error>> {
    log::trace!("import_secret_key_to_store");
    let file = File::open(key_path)?;
    let reader = BufReader::new(file);

    let key = SignedSecretKey::from_reader_single(reader)?.0;
    check_secret_key(&key, passphrase)?;

    let armored = key.to_armored_string(ArmorOptions::default())?;
    store::set_secret_key(app, &armored);

    let fingerprint = key.fingerprint().to_string();
    Ok(fingerprint)
}

pub fn load_my_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
) -> Result<SignedSecretKey, Box<dyn Error>> {
    log::trace!("load_my_key");
    let armored = store::get_secret_key(app);
    let key = SignedSecretKey::from_string(&armored)?.0;
    Ok(key)
}

pub fn get_chat_key_path<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    chat_id: i64,
) -> Result<String, Box<dyn Error>> {
    let app_folder = store::get_application_folder(&app);
    let mut target_key_file = PathBuf::from(app_folder);
    target_key_file.push("keys");
    fs::create_dir_all(target_key_file.clone())?;
    target_key_file.push(format!("{}.asc", chat_id));
    Ok(target_key_file.to_string_lossy().to_string())
}

pub fn get_pgp_file_path<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    suffix: &str,
) -> Result<String, Box<dyn Error>> {
    let mut path = PathBuf::from(store::get_application_folder(app));
    path.push("pgp");
    path.push("messages");
    fs::create_dir_all(path.clone())?;
    path.push(format!(
        "ombra-chat-{}{}",
        utils::generate_random_string(10),
        suffix
    ));
    Ok(path.to_string_lossy().to_string())
}

pub fn get_chat_encryption_keys<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    chat_id: i64,
) -> Result<Vec<PublicSubkey>, Box<dyn Error>> {
    let my_key = state::get_my_encryption_key(app)?;
    let other_key = state::get_chat_encryption_key(app, chat_id)?;
    Ok(vec![my_key, other_key])
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encrypt_and_decrypt() {
        let passphrase1 = "foo";
        let passphrase2 = "bar";
        let message = "secret message";

        let sec_key1 = generate_key(passphrase1).unwrap();
        let sec_key2 = generate_key(passphrase2).unwrap();

        let pub_key2_armored = get_armored_public_key(&sec_key2).unwrap();
        let pub_key2 = load_public_key(&pub_key2_armored).unwrap();

        let enc_key1 = get_encryption_key_from_secret_key(&sec_key1).unwrap();
        let enc_key2 = get_encryption_key_from_public_key(&pub_key2).unwrap();

        let encrypted = encrypt_string_to_string(vec![enc_key1, enc_key2], message).unwrap();

        let decrypted1 =
            decrypt_armored(&sec_key1, passphrase1, encrypted.clone().into_bytes()).unwrap();
        let decrypted2 = decrypt_armored(&sec_key2, passphrase2, encrypted.into_bytes()).unwrap();

        assert_eq!(String::from_utf8(decrypted1).unwrap(), message);
        assert_eq!(String::from_utf8(decrypted2).unwrap(), message);
    }
}
