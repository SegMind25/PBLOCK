use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use aes_gcm::{
    aead::{Aead, AeadCore, KeyInit, OsRng as AesOsRng},
    Aes256Gcm, Nonce,
};
use base64::{engine::general_purpose, Engine as _};
use rand::Rng;

use crate::errors::AppError;

pub fn hash_password(password: &str) -> Result<String, AppError> {
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();

    let password_hash = argon2
        .hash_password(password.as_bytes(), &salt)
        .map_err(|e| AppError::InternalError(format!("Failed to hash password: {}", e)))?
        .to_string();

    Ok(password_hash)
}

pub fn verify_password(password: &str, password_hash: &str) -> Result<bool, AppError> {
    let parsed_hash = PasswordHash::new(password_hash)
        .map_err(|e| AppError::InternalError(format!("Failed to parse hash: {}", e)))?;

    let argon2 = Argon2::default();

    Ok(argon2
        .verify_password(password.as_bytes(), &parsed_hash)
        .is_ok())
}

pub fn generate_recovery_code() -> String {
    let mut rng = rand::thread_rng();
    let code: String = (0..6)
        .map(|_| rng.gen_range(0..10).to_string())
        .collect();
    code
}

pub fn encrypt_data(data: &str, key: &[u8; 32]) -> Result<String, AppError> {
    let cipher = Aes256Gcm::new(key.into());
    let nonce = Aes256Gcm::generate_nonce(&mut AesOsRng);

    let ciphertext = cipher
        .encrypt(&nonce, data.as_bytes())
        .map_err(|e| AppError::InternalError(format!("Encryption failed: {}", e)))?;

    let mut result = nonce.to_vec();
    result.extend_from_slice(&ciphertext);

    Ok(general_purpose::STANDARD.encode(result))
}

pub fn decrypt_data(encrypted: &str, key: &[u8; 32]) -> Result<String, AppError> {
    let decoded = general_purpose::STANDARD
        .decode(encrypted)
        .map_err(|e| AppError::InternalError(format!("Base64 decode failed: {}", e)))?;

    if decoded.len() < 12 {
        return Err(AppError::InternalError("Invalid encrypted data".to_string()));
    }

    let (nonce_bytes, ciphertext) = decoded.split_at(12);
    let nonce = Nonce::from_slice(nonce_bytes);

    let cipher = Aes256Gcm::new(key.into());

    let plaintext = cipher
        .decrypt(nonce, ciphertext)
        .map_err(|e| AppError::InternalError(format!("Decryption failed: {}", e)))?;

    String::from_utf8(plaintext)
        .map_err(|e| AppError::InternalError(format!("UTF-8 conversion failed: {}", e)))
}

pub fn generate_jwt_secret() -> String {
    let mut rng = rand::thread_rng();
    let random_bytes: Vec<u8> = (0..32).map(|_| rng.gen()).collect();
    hex::encode(random_bytes)
}
