use sequoia_openpgp::parse::Parse;
use sequoia_openpgp::policy::StandardPolicy;
use sequoia_openpgp::serialize::stream::{Armorer, Encryptor, LiteralWriter, Message};
use sequoia_openpgp::serialize::SerializeInto;
use sequoia_openpgp::{
    cert::{CertBuilder, CipherSuite},
    Cert,
};
use std::io::{Cursor, Write};
use std::{error::Error, fs::File, io::BufReader};
use tauri_plugin_store::StoreExt;

pub fn generate_gpg_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    password: &str,
) -> Result<String, Box<dyn Error>> {
    // Generate a new ECC key pair using Curve25519
    let (cert, _) = CertBuilder::new()
        .set_cipher_suite(CipherSuite::Cv25519)
        .add_storage_encryption_subkey()
        .set_password(Some(password.into()))
        .generate()?;

    // Save armored key into the store
    let armored = String::from_utf8(cert.as_tsk().armored().to_vec()?)?;
    let store = app.store("store.json")?;
    store.set("secret-key", armored);

    let fingerprint = cert.fingerprint().to_string();
    Ok(fingerprint)
}

pub fn import_gpg_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    key_path: &str,
    password: &str,
) -> Result<String, Box<dyn Error>> {
    let file = File::open(key_path)?;
    let mut reader = BufReader::new(file);
    let cert = Cert::from_reader(&mut reader)?;

    let key = cert.primary_key().key().clone().parts_into_secret()?;
    key.decrypt_secret(&password.into())?;

    let armored = String::from_utf8(cert.as_tsk().armored().to_vec()?)?;
    let store = app.store("store.json")?;
    store.set("secret-key", armored);

    let fingerprint = cert.fingerprint().to_string();
    Ok(fingerprint)
}

pub fn check_secret_key<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    password: &str,
) -> Result<(), Box<dyn Error>> {
    let cert = get_user_cert(app)?;
    let key = cert.primary_key().key().clone().parts_into_secret()?;
    key.decrypt_secret(&password.into())?;
    Ok(())
}

pub fn encrypt_user_secret<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    secret: &str,
) -> Result<String, Box<dyn Error>> {
    let cert = get_user_cert(app)?;
    let encrypted_text = encrypt_text(vec![cert], secret)?;
    Ok(encrypted_text)
}

pub fn encrypt_text(recipient_certs: Vec<Cert>, plaintext: &str) -> Result<String, Box<dyn Error>> {
    let p = &StandardPolicy::new();

    let mut recipients = Vec::new();
    for cert in recipient_certs.iter() {
        // Make sure we add at least one subkey from every certificate.
        let mut found_one = false;
        for key in cert
            .keys()
            .with_policy(p, None)
            .supported()
            .alive()
            .revoked(false)
            .for_storage_encryption()
        {
            recipients.push(key);
            found_one = true;
        }

        if !found_one {
            return Err(format!("No suitable encryption subkey for {}", cert).into());
        }
    }

    let mut sink = vec![];
    let message = Message::new(&mut sink);
    let message = Encryptor::for_recipients(message, recipients).build()?;
    let mut w = LiteralWriter::new(message).build()?;
    w.write_all(plaintext.as_bytes())?;
    w.finalize()?;

    let mut armored_sink = vec![];
    let message = Message::new(&mut armored_sink);
    let message = Armorer::new(message).build()?;
    let mut message = LiteralWriter::new(message).build()?;
    message.write_all(&sink)?;
    message.finalize()?;

    let encrypted_string = std::str::from_utf8(&armored_sink)?;
    Ok(encrypted_string.into())
}

fn get_user_cert<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> Result<Cert, Box<dyn Error>> {
    let store = app.store("store.json")?;
    let key = store
        .get("secret-key")
        .ok_or_else(|| "No secret key found. Please generate or import one".to_string())?;
    let key = key
        .as_str()
        .ok_or_else(|| "Unable to read secret key from store".to_string())?;
    let mut reader = BufReader::new(Cursor::new(key));
    let cert = Cert::from_reader(&mut reader)?;
    Ok(cert)
}
