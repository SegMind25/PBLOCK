use argon2::{
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use rand::rngs::OsRng;
use std::fs;
use std::path::PathBuf;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
struct AuthData {
    password_hash: String,
}

pub struct AuthManager {
    auth_file: PathBuf,
}

impl AuthManager {
    pub fn new(config: &crate::config::Config) -> Result<Self, Box<dyn std::error::Error>> {
        Ok(Self {
            auth_file: config.config_dir.join("auth.json"),
        })
    }

    pub fn set_password(&self, password: &str) -> Result<(), Box<dyn std::error::Error>> {
        let salt = SaltString::generate(&mut OsRng);
        let argon2 = Argon2::default();
        let password_hash = argon2
            .hash_password(password.as_bytes(), &salt)?
            .to_string();

        let auth_data = AuthData { password_hash };
        let json = serde_json::to_string_pretty(&auth_data)?;
        fs::write(&self.auth_file, json)?;

        Ok(())
    }

    pub fn verify_password(&self, password: &str) -> Result<bool, Box<dyn std::error::Error>> {
        if !self.auth_file.exists() {
            return Ok(false);
        }

        let content = fs::read_to_string(&self.auth_file)?;
        let auth_data: AuthData = serde_json::from_str(&content)?;

        let parsed_hash = PasswordHash::new(&auth_data.password_hash)?;
        let argon2 = Argon2::default();

        Ok(argon2
            .verify_password(password.as_bytes(), &parsed_hash)
            .is_ok())
    }

    pub fn generate_session_token(&self) -> Result<String, Box<dyn std::error::Error>> {
        use rand::Rng;
        let token: String = rand::thread_rng()
            .sample_iter(&rand::distributions::Alphanumeric)
            .take(32)
            .map(char::from)
            .collect();
        Ok(token)
    }

    pub fn verify_token(&self, _token: &str) -> bool {
        // In production, implement proper token validation with expiry
        // For now, we'll use a simple check
        !_token.is_empty()
    }
}
