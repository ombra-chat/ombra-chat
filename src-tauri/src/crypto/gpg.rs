use crate::{store, AppState};
use pgp::composed::{
    ArmorOptions, Deserializable, KeyType, Message, MessageBuilder, PublicSubkey,
    SecretKeyParamsBuilder, SignedPublicKey, SignedSecretKey, SubkeyParamsBuilder,
};
use pgp::crypto::sym::SymmetricKeyAlgorithm;
use pgp::types::{KeyDetails, PublicKeyTrait};
use rand::thread_rng;
use std::io::Cursor;
use std::sync::Mutex;
use std::{error::Error, fs::File, io::BufReader};
use tauri::Manager;

fn encrypt(
    keys: Vec<&PublicSubkey>,
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
    keys: Vec<&PublicSubkey>,
    input: String,
) -> Result<String, Box<dyn Error>> {
    log::trace!("encrypt_string_to_string");
    let bytes = pgp::bytes::Bytes::from(input);
    encrypt_to_string(keys, bytes)
}

fn encrypt_to_string(
    keys: Vec<&PublicSubkey>,
    input: impl Into<pgp::bytes::Bytes>,
) -> Result<String, Box<dyn Error>> {
    log::trace!("encrypt_to_string");
    let data = encrypt(keys, input)?;
    let armored = MessageBuilder::from_bytes("", data)
        .to_armored_string(thread_rng(), ArmorOptions::default())?;
    Ok(armored)
}

fn decrypt(
    key: &SignedSecretKey,
    password: &str,
    input: Vec<u8>,
) -> Result<Vec<u8>, Box<dyn Error>> {
    log::trace!("decrypt");
    let buf = Cursor::new(input);
    let message = Message::from_bytes(buf)?;
    let mut decrypted = message.decrypt(&password.into(), key)?;
    Ok(decrypted.as_data_vec()?)
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

pub fn decrypt_string_to_string<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    armored_input: String,
) -> Result<String, Box<dyn Error>> {
    log::trace!("decrypt_string_to_string");
    let key = get_my_key(app)?;
    let passphrase = get_gpg_passphrase(app);
    let data = decrypt_armored(&key, &passphrase, armored_input.into_bytes())?;
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

pub fn load_secret_key(armored: &str) -> Result<SignedSecretKey, Box<dyn Error>> {
    log::trace!("load_secret_key");
    let key = SignedSecretKey::from_string(armored)?.0;
    Ok(key)
}

pub fn load_public_key(armored: &str) -> Result<SignedPublicKey, Box<dyn Error>> {
    log::trace!("load_public_key");
    let key = SignedPublicKey::from_string(armored)?.0;
    Ok(key)
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

pub fn get_my_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
) -> Result<SignedSecretKey, Box<dyn Error>> {
    log::trace!("get_my_key");
    let armored = store::get_secret_key(app);
    let key = SignedSecretKey::from_string(&armored)?.0;
    Ok(key)
}

fn get_gpg_passphrase<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> String {
    log::trace!("get_gpg_passphrase");
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.gpg_passphrase.to_string()
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

        let encrypted =
            encrypt_string_to_string(vec![&enc_key1, &enc_key2], String::from(message)).unwrap();

        let decrypted1 =
            decrypt_armored(&sec_key1, passphrase1, encrypted.clone().into_bytes()).unwrap();
        let decrypted2 = decrypt_armored(&sec_key2, passphrase2, encrypted.into_bytes()).unwrap();

        assert_eq!(String::from_utf8(decrypted1).unwrap(), message);
        assert_eq!(String::from_utf8(decrypted2).unwrap(), message);
    }
}
