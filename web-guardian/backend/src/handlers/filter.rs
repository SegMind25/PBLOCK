use actix_web::{delete, get, post, put, web, HttpRequest, HttpResponse, HttpMessage};
use validator::Validate;

use crate::database::{schema, DbPool};
use crate::errors::AppError;
use crate::middleware::Claims;
use crate::models::{
    AccessLogResponse, BlockedSiteResponse, CreateBlockedSiteRequest,
};
use crate::services::HostFileManager;
use crate::utils::validator::sanitize_domain;

#[get("/blocked-sites")]
pub async fn get_blocked_sites(
    pool: web::Data<DbPool>,
    req: HttpRequest,
) -> Result<HttpResponse, AppError> {
    let claims = req
        .extensions()
        .get::<Claims>()
        .cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    if !claims.is_parent {
        return Err(AppError::UnauthorizedError(
            "Only parents can view blocked sites".to_string(),
        ));
    }

    let sites = schema::get_all_blocked_sites(&pool).await?;
    let responses: Vec<BlockedSiteResponse> = sites.into_iter().map(|s| s.into()).collect();

    Ok(HttpResponse::Ok().json(responses))
}

#[post("/blocked-sites")]
pub async fn add_blocked_site(
    pool: web::Data<DbPool>,
    host_manager: web::Data<HostFileManager>,
    req: HttpRequest,
    site_data: web::Json<CreateBlockedSiteRequest>,
) -> Result<HttpResponse, AppError> {
    let claims = req
        .extensions()
        .get::<Claims>()
        .cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    if !claims.is_parent {
        return Err(AppError::UnauthorizedError(
            "Only parents can add blocked sites".to_string(),
        ));
    }

    site_data
        .validate()
        .map_err(|e| AppError::ValidationError(e.to_string()))?;

    let domain = sanitize_domain(&site_data.domain);

    if schema::is_domain_blocked(&pool, &domain).await? {
        return Err(AppError::AlreadyExistsError(
            "Domain already blocked".to_string(),
        ));
    }

    let site = schema::create_blocked_site(
        &pool,
        &domain,
        &site_data.category,
        site_data.reason.as_deref(),
        claims.sub,
    )
    .await?;

    host_manager.add_blocked_domains(&[domain.clone()])?;

    tracing::info!("✅ Domain {} blocked by user {}", domain, claims.username);

    Ok(HttpResponse::Created().json(BlockedSiteResponse::from(site)))
}

#[delete("/blocked-sites/{id}")]
pub async fn delete_blocked_site(
    pool: web::Data<DbPool>,
    host_manager: web::Data<HostFileManager>,
    req: HttpRequest,
    path: web::Path<i64>,
) -> Result<HttpResponse, AppError> {
    let claims = req
        .extensions()
        .get::<Claims>()
        .cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    if !claims.is_parent {
        return Err(AppError::UnauthorizedError(
            "Only parents can delete blocked sites".to_string(),
        ));
    }

    let site_id = path.into_inner();
    let site = schema::get_blocked_site_by_id(&pool, site_id).await?;

    host_manager.remove_blocked_domains(&[site.domain.clone()])?;

    schema::delete_blocked_site(&pool, site_id).await?;

    tracing::info!(
        "✅ Domain {} unblocked by user {}",
        site.domain,
        claims.username
    );

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "message": "Blocked site deleted successfully",
        "domain": site.domain
    })))
}

#[get("/check/{domain}")]
pub async fn check_domain(
    pool: web::Data<DbPool>,
    path: web::Path<String>,
) -> Result<HttpResponse, AppError> {
    let domain = sanitize_domain(&path.into_inner());
    let is_blocked = schema::is_domain_blocked(&pool, &domain).await?;

    schema::create_access_log(&pool, &domain, is_blocked, None, None).await?;

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "domain": domain,
        "blocked": is_blocked
    })))
}

#[get("/logs")]
pub async fn get_access_logs(
    pool: web::Data<DbPool>,
    req: HttpRequest,
    query: web::Query<serde_json::Value>,
) -> Result<HttpResponse, AppError> {
    let claims = req
        .extensions()
        .get::<Claims>()
        .cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    if !claims.is_parent {
        return Err(AppError::UnauthorizedError(
            "Only parents can view access logs".to_string(),
        ));
    }

    let limit = query
        .get("limit")
        .and_then(|v| v.as_i64())
        .unwrap_or(100);

    let logs = schema::get_recent_access_logs(&pool, limit).await?;
    let responses: Vec<AccessLogResponse> = logs.into_iter().map(|l| l.into()).collect();

    Ok(HttpResponse::Ok().json(responses))
}

#[get("/stats")]
pub async fn get_stats(
    pool: web::Data<DbPool>,
    req: HttpRequest,
) -> Result<HttpResponse, AppError> {
    let claims = req
        .extensions()
        .get::<Claims>()
        .cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    if !claims.is_parent {
        return Err(AppError::UnauthorizedError(
            "Only parents can view statistics".to_string(),
        ));
    }

    let stats = schema::get_access_log_stats(&pool).await?;

    Ok(HttpResponse::Ok().json(stats))
}

#[put("/enable")]
pub async fn enable_filter(
    pool: web::Data<DbPool>,
    host_manager: web::Data<HostFileManager>,
    req: HttpRequest,
) -> Result<HttpResponse, AppError> {
    let claims = req
        .extensions()
        .get::<Claims>()
        .cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    if !claims.is_parent {
        return Err(AppError::UnauthorizedError(
            "Only parents can enable filter".to_string(),
        ));
    }

    let sites = schema::get_all_blocked_sites(&pool).await?;
    let domains: Vec<String> = sites.into_iter().map(|s| s.domain).collect();

    host_manager.add_blocked_domains(&domains)?;

    tracing::info!("✅ Filter enabled by user {}", claims.username);

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "message": "Filter enabled successfully",
        "blocked_domains_count": domains.len()
    })))
}

#[put("/disable")]
pub async fn disable_filter(
    _pool: web::Data<DbPool>,
    host_manager: web::Data<HostFileManager>,
    req: HttpRequest,
) -> Result<HttpResponse, AppError> {
    let claims = req
        .extensions()
        .get::<Claims>()
        .cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    if !claims.is_parent {
        return Err(AppError::UnauthorizedError(
            "Only parents can disable filter".to_string(),
        ));
    }

    let domains = host_manager.get_blocked_domains()?;
    host_manager.remove_blocked_domains(&domains)?;

    tracing::warn!("⚠️  Filter disabled by user {}", claims.username);

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "message": "Filter disabled successfully",
        "warning": "Protection is now disabled"
    })))
}
