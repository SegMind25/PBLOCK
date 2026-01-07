use serde::Deserialize;
use std::env;

#[derive(Debug, Clone, Deserialize)]
pub struct Settings {
    pub server_host: String,
    pub server_port: u16,
    pub database_url: String,
    pub jwt_secret: String,
    pub jwt_expiration_hours: i64,
    pub encryption_key: String,
    pub enable_dns_filtering: bool,
    pub enable_host_file_blocking: bool,
    pub enable_tamper_protection: bool,
    pub monitor_interval_seconds: u64,
}

impl Settings {
    pub fn new() -> Result<Self, config::ConfigError> {
        let settings = Settings {
            server_host: env::var("SERVER_HOST")
                .unwrap_or_else(|_| "127.0.0.1".to_string()),
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8080".to_string())
                .parse()
                .unwrap_or(8080),
            database_url: env::var("DATABASE_URL")
                .unwrap_or_else(|_| "sqlite://web_guardian.db".to_string()),
            jwt_secret: env::var("JWT_SECRET")
                .expect("JWT_SECRET must be set in .env"),
            jwt_expiration_hours: env::var("JWT_EXPIRATION_HOURS")
                .unwrap_or_else(|_| "24".to_string())
                .parse()
                .unwrap_or(24),
            encryption_key: env::var("ENCRYPTION_KEY")
                .expect("ENCRYPTION_KEY must be set in .env"),
            enable_dns_filtering: env::var("ENABLE_DNS_FILTERING")
                .unwrap_or_else(|_| "true".to_string())
                .parse()
                .unwrap_or(true),
            enable_host_file_blocking: env::var("ENABLE_HOST_FILE_BLOCKING")
                .unwrap_or_else(|_| "true".to_string())
                .parse()
                .unwrap_or(true),
            enable_tamper_protection: env::var("ENABLE_TAMPER_PROTECTION")
                .unwrap_or_else(|_| "true".to_string())
                .parse()
                .unwrap_or(true),
            monitor_interval_seconds: env::var("MONITOR_INTERVAL_SECONDS")
                .unwrap_or_else(|_| "5".to_string())
                .parse()
                .unwrap_or(5),
        };

        Ok(settings)
    }
}