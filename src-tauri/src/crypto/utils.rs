use rand::{distributions::Alphanumeric, thread_rng, Rng};
use std::error::Error;

const RANDOM_PASSWORD_LENGTH: usize = 25;

pub fn generate_random_string(length: usize) -> String {
    let mut rng = thread_rng();
    std::iter::repeat_with(|| rng.sample(Alphanumeric) as char)
        .take(length)
        .collect()
}

pub fn generate_random_password() -> String {
    generate_random_string(RANDOM_PASSWORD_LENGTH)
}

pub fn generate_random_salt() -> String {
    let mut rng = thread_rng();
    let salt: [u8; 16] = rng.gen(); // salt is 16 bytes length
    hex::encode(&salt)
}

/**
 * Generates the derived key for encrypting local tdlib database. Using PBKDF2-HMAC-SHA256
 * on a password provided by the user is acceptable for desktop applications where the user
 * doesn't rely on notifications arriving in the background when they are not using the app.
 * See https://github.com/tdlib/td/issues/188
 */
pub fn generate_derived_key(password: &str, salt: &str) -> Result<String, Box<dyn Error>> {
    let mut key = vec![0u8; 32]; // key length is 256 bits
    let iterations = 100_000;
    let salt = hex::decode(salt)?;
    pbkdf2::pbkdf2_hmac::<sha2::Sha256>(password.as_bytes(), &salt, iterations, &mut key);
    Ok(hex::encode(&key))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_generate_random_salt() {
        let salt = generate_random_salt();
        assert_eq!(salt.len(), 32);
    }

    #[test]
    fn test_generate_derived_key() {
        let salt = "f07bc256d5aa50d0e0f6583f69eb48b5";
        let password = "foo";
        let key = generate_derived_key(password, salt).unwrap();
        assert_eq!(
            key,
            "8e4005244278f06b6dd9cc5685ba8040e4644dede3ee17545fa4309f008fc8d5"
        );
    }
}
