use crate::errors::AppError;
use crate::utils::crypto::{encrypt_data, decrypt_data};
use hex;

pub struct EncryptionService {
    key: [u8; 32],
}

impl EncryptionService {
    pub fn new(key_hex: &str) -> Result<Self, AppError> {
        let key_bytes = hex::decode(key_hex)
            .map_err(|e| AppError::InternalError(format!("Invalid encryption key: {}", e)))?;

        if key_bytes.len() != 32 {
            return Err(AppError::InternalError(
                "Encryption key must be 32 bytes".to_string(),
            ));
        }

        let mut key = [0u8; 32];
        key.copy_from_slice(&key_bytes);

        Ok(EncryptionService { key })
    }

    pub fn encrypt(&self, data: &str) -> Result<String, AppError> {
        encrypt_data(data, &self.key)
    }

    pub fn decrypt(&self, encrypted: &str) -> Result<String, AppError> {
        decrypt_data(encrypted, &self.key)
    }
}