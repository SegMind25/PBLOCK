use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct AccessLog {
    pub id: i64,
    pub domain: String,
    pub blocked: bool,
    pub user_agent: Option<String>,
    pub ip_address: Option<String>,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Deserialize)]
pub struct CreateAccessLogRequest {
    pub domain: String,
    pub blocked: bool,
    pub user_agent: Option<String>,
    pub ip_address: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct AccessLogResponse {
    pub id: i64,
    pub domain: String,
    pub blocked: bool,
    pub timestamp: DateTime<Utc>,
}

impl From<AccessLog> for AccessLogResponse {
    fn from(log: AccessLog) -> Self {
        AccessLogResponse {
            id: log.id,
            domain: log.domain,
            blocked: log.blocked,
            timestamp: log.timestamp,
        }
    }
}

#[derive(Debug, Serialize)]
pub struct AccessLogStats {
    pub total_attempts: i64,
    pub blocked_attempts: i64,
    pub unique_domains: i64,
    pub top_blocked_domains: Vec<DomainCount>,
}

#[derive(Debug, Serialize, FromRow)]
pub struct DomainCount {
    pub domain: String,
    pub count: i64,
}