use crate::database::DbPool;
use crate::errors::AppError;
use crate::models::*;
use chrono::Utc;
use sqlx::Row;

// User operations
pub async fn create_user(
    pool: &DbPool,
    username: &str,
    password_hash: &str,
    email: Option<&str>,
    is_parent: bool,
) -> Result<User, AppError> {
    let user = sqlx::query_as::<_, User>(
        r#"
        INSERT INTO users (username, password_hash, email, is_parent, created_at)
        VALUES (?, ?, ?, ?, ?)
        RETURNING *
        "#,
    )
    .bind(username)
    .bind(password_hash)
    .bind(email)
    .bind(is_parent)
    .bind(Utc::now())
    .fetch_one(pool)
    .await?;

    Ok(user)
}

pub async fn get_user_by_username(pool: &DbPool, username: &str) -> Result<User, AppError> {
    let user = sqlx::query_as::<_, User>(
        r#"SELECT * FROM users WHERE username = ?"#
    )
    .bind(username)
    .fetch_one(pool)
    .await
    .map_err(|_| AppError::NotFoundError("User not found".to_string()))?;

    Ok(user)
}

pub async fn get_user_by_id(pool: &DbPool, user_id: i64) -> Result<User, AppError> {
    let user = sqlx::query_as::<_, User>(
        r#"SELECT * FROM users WHERE id = ?"#
    )
    .bind(user_id)
    .fetch_one(pool)
    .await
    .map_err(|_| AppError::NotFoundError("User not found".to_string()))?;

    Ok(user)
}

pub async fn update_last_login(pool: &DbPool, user_id: i64) -> Result<(), AppError> {
    sqlx::query(r#"UPDATE users SET last_login = ? WHERE id = ?"#)
        .bind(Utc::now())
        .bind(user_id)
        .execute(pool)
        .await?;

    Ok(())
}

pub async fn has_parent_user(pool: &DbPool) -> Result<bool, AppError> {
    let count: i64 = sqlx::query_scalar(
        r#"SELECT COUNT(*) FROM users WHERE is_parent = true"#
    )
    .fetch_one(pool)
    .await?;

    Ok(count > 0)
}

// Blocked site operations
pub async fn create_blocked_site(
    pool: &DbPool,
    domain: &str,
    category: &str,
    reason: Option<&str>,
    user_id: i64,
) -> Result<BlockedSite, AppError> {
    let site = sqlx::query_as::<_, BlockedSite>(
        r#"
        INSERT INTO blocked_sites (domain, category, is_active, added_by_user_id, reason, created_at, updated_at)
        VALUES (?, ?, true, ?, ?, ?, ?)
        RETURNING *
        "#,
    )
    .bind(domain.to_lowercase())
    .bind(category)
    .bind(user_id)
    .bind(reason)
    .bind(Utc::now())
    .bind(Utc::now())
    .fetch_one(pool)
    .await?;

    Ok(site)
}

pub async fn get_all_blocked_sites(pool: &DbPool) -> Result<Vec<BlockedSite>, AppError> {
    let sites = sqlx::query_as::<_, BlockedSite>(
        r#"SELECT * FROM blocked_sites WHERE is_active = true ORDER BY created_at DESC"#
    )
    .fetch_all(pool)
    .await?;

    Ok(sites)
}

pub async fn get_blocked_site_by_id(pool: &DbPool, site_id: i64) -> Result<BlockedSite, AppError> {
    let site = sqlx::query_as::<_, BlockedSite>(
        r#"SELECT * FROM blocked_sites WHERE id = ?"#
    )
    .bind(site_id)
    .fetch_one(pool)
    .await
    .map_err(|_| AppError::NotFoundError("Blocked site not found".to_string()))?;

    Ok(site)
}

pub async fn delete_blocked_site(pool: &DbPool, site_id: i64) -> Result<(), AppError> {
    let result = sqlx::query(r#"DELETE FROM blocked_sites WHERE id = ?"#)
        .bind(site_id)
        .execute(pool)
        .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFoundError("Blocked site not found".to_string()));
    }

    Ok(())
}

pub async fn is_domain_blocked(pool: &DbPool, domain: &str) -> Result<bool, AppError> {
    let count: i64 = sqlx::query_scalar(
        r#"SELECT COUNT(*) FROM blocked_sites WHERE domain = ? AND is_active = true"#
    )
    .bind(domain.to_lowercase())
    .fetch_one(pool)
    .await?;

    Ok(count > 0)
}

// Access log operations
pub async fn create_access_log(
    pool: &DbPool,
    domain: &str,
    blocked: bool,
    user_agent: Option<&str>,
    ip_address: Option<&str>,
) -> Result<AccessLog, AppError> {
    let log = sqlx::query_as::<_, AccessLog>(
        r#"
        INSERT INTO access_logs (domain, blocked, user_agent, ip_address, timestamp)
        VALUES (?, ?, ?, ?, ?)
        RETURNING *
        "#,
    )
    .bind(domain)
    .bind(blocked)
    .bind(user_agent)
    .bind(ip_address)
    .bind(Utc::now())
    .fetch_one(pool)
    .await?;

    Ok(log)
}

pub async fn get_recent_access_logs(pool: &DbPool, limit: i64) -> Result<Vec<AccessLog>, AppError> {
    let logs = sqlx::query_as::<_, AccessLog>(
        r#"SELECT * FROM access_logs ORDER BY timestamp DESC LIMIT ?"#
    )
    .bind(limit)
    .fetch_all(pool)
    .await?;

    Ok(logs)
}

pub async fn get_access_log_stats(pool: &DbPool) -> Result<AccessLogStats, AppError> {
    let total_attempts: i64 = sqlx::query_scalar(
        r#"SELECT COUNT(*) FROM access_logs"#
    )
    .fetch_one(pool)
    .await?;

    let blocked_attempts: i64 = sqlx::query_scalar(
        r#"SELECT COUNT(*) FROM access_logs WHERE blocked = true"#
    )
    .fetch_one(pool)
    .await?;

    let unique_domains: i64 = sqlx::query_scalar(
        r#"SELECT COUNT(DISTINCT domain) FROM access_logs"#
    )
    .fetch_one(pool)
    .await?;

    let top_blocked_domains = sqlx::query_as::<_, DomainCount>(
        r#"
        SELECT domain, COUNT(*) as count 
        FROM access_logs 
        WHERE blocked = true 
        GROUP BY domain 
        ORDER BY count DESC 
        LIMIT 10
        "#
    )
    .fetch_all(pool)
    .await?;

    Ok(AccessLogStats {
        total_attempts,
        blocked_attempts,
        unique_domains,
        top_blocked_domains,
    })
}