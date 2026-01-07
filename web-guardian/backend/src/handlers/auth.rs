use actix_web::{get, post, web, HttpRequest, HttpResponse};
use validator::Validate;

use crate::database::{schema, DbPool};
use crate::errors::AppError;
use crate::middleware::Claims;
use crate::models::{CreateUserRequest, LoginRequest, LoginResponse, UserResponse};
use crate::utils::crypto::{hash_password, verify_password};

#[post("/register")]
pub async fn register(
    pool: web::Data<DbPool>,
    user_data: web::Json<CreateUserRequest>,
) -> Result<HttpResponse, AppError> {
    user_data.validate()
        .map_err(|e| AppError::ValidationError(e.to_string()))?;

    // Check if parent user already exists
    if user_data.is_parent {
        let has_parent = schema::has_parent_user(&pool).await?;
        if has_parent {
            return Err(AppError::AlreadyExistsError(
                "Parent user already exists".to_string(),
            ));
        }
    }

    // Check if username already exists
    if schema::get_user_by_username(&pool, &user_data.username).await.is_ok() {
        return Err(AppError::AlreadyExistsError(
            "Username already exists".to_string(),
        ));
    }

    // Hash password
    let password_hash = hash_password(&user_data.password)?;

    // Create user
    let user = schema::create_user(
        &pool,
        &user_data.username,
        &password_hash,
        user_data.email.as_deref(),
        user_data.is_parent,
    )
    .await?;

    // If this is a parent user, activate the filter by default
    if user.is_parent {
        tracing::info!("✅ Parent user created, activating filter");
        // TODO: Activate filter through service
    }

    Ok(HttpResponse::Created().json(UserResponse::from(user)))
}

#[post("/login")]
pub async fn login(
    pool: web::Data<DbPool>,
    credentials: web::Json<LoginRequest>,
) -> Result<HttpResponse, AppError> {
    credentials.validate()
        .map_err(|e| AppError::ValidationError(e.to_string()))?;

    // Get user by username
    let user = schema::get_user_by_username(&pool, &credentials.username).await?;

    // Verify password
    let is_valid = verify_password(&credentials.password, &user.password_hash)?;
    if !is_valid {
        return Err(AppError::AuthenticationError(
            "Invalid credentials".to_string(),
        ));
    }

    // Update last login
    schema::update_last_login(&pool, user.id).await?;

    // Generate JWT token
    let claims = Claims::new(user.id, user.username.clone(), user.is_parent, 24);
    let token = claims.encode()
        .map_err(|e| AppError::InternalError(format!("Failed to generate token: {}", e)))?;

    Ok(HttpResponse::Ok().json(LoginResponse {
        token,
        user: UserResponse::from(user),
    }))
}

#[get("/check-setup")]
pub async fn check_setup(pool: web::Data<DbPool>) -> Result<HttpResponse, AppError> {
    let has_parent = schema::has_parent_user(&pool).await?;

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "setup_required": !has_parent,
        "has_parent_user": has_parent,
    })))
}

#[get("/me")]
pub async fn get_current_user(
    pool: web::Data<DbPool>,
    req: HttpRequest,
) -> Result<HttpResponse, AppError> {
    let claims = req.extensions().get::<Claims>().cloned()
        .ok_or_else(|| AppError::UnauthorizedError("Not authenticated".to_string()))?;

    let user = schema::get_user_by_id(&pool, claims.sub).await?;

    Ok(HttpResponse::Ok().json(UserResponse::from(user)))
}