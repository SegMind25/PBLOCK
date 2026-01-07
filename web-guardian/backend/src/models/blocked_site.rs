use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use validator::Validate;

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct BlockedSite {
    pub id: i64,
    pub domain: String,
    pub category: String,
    pub is_active: bool,
    pub added_by_user_id: i64,
    pub reason: Option<String>,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize, Validate)]
pub struct CreateBlockedSiteRequest {
    #[validate(length(min = 3, max = 255))]
    pub domain: String,
    
    #[validate(length(min = 1, max = 50))]
    pub category: String,
    
    pub reason: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct UpdateBlockedSiteRequest {
    pub is_active: Option<bool>,
    pub category: Option<String>,
    pub reason: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct BlockedSiteResponse {
    pub id: i64,
    pub domain: String,
    pub category: String,
    pub is_active: bool,
    pub reason: Option<String>,
    pub created_at: DateTime<Utc>,
}

impl From<BlockedSite> for BlockedSiteResponse {
    fn from(site: BlockedSite) -> Self {
        BlockedSiteResponse {
            id: site.id,
            domain: site.domain,
            category: site.category,
            is_active: site.is_active,
            reason: site.reason,
            created_at: site.created_at,
        }
    }
}