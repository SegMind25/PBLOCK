// backend/src/main.rs
use axum::{
    routing::{get, post},
    Router, Json, response::IntoResponse, http::StatusCode,
};
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::RwLock;
use tower_http::cors::CorsLayer;

mod platform;
mod blocker;
mod auth;
mod protection;
mod config;
mod database;

use blocker::BlockerEngine;
use auth::AuthManager;
use config::Config;

#[derive(Clone)]
struct AppState {
    blocker: Arc<RwLock<BlockerEngine>>,
    auth: Arc<AuthManager>,
    config: Arc<RwLock<Config>>,
}

#[tokio::main]
async fn main() {
    // Initialize tracing
    tracing_subscriber::fmt::init();

    // Load configuration
    let config = Config::load().expect("Failed to load configuration");
    
    // Check if first run
    if config.is_first_run {
        println!("First run detected. Please complete setup via the web interface.");
        println!("Navigate to: http://localhost:8080/setup");
    }

    // Initialize components
    let blocker = BlockerEngine::new(&config).await.expect("Failed to initialize blocker");
    let auth = AuthManager::new(&config).expect("Failed to initialize auth");
    
    // Start blocker engine
    blocker.start().await.expect("Failed to start blocker");

    // Install as system service (platform-specific)
    if config.install_as_service && !platform::is_running_as_service() {
        platform::install_service().expect("Failed to install service");
        println!("Service installed. Restarting...");
        std::process::exit(0);
    }

    // Enable self-protection
    protection::enable_self_protection().expect("Failed to enable protection");

    let state = AppState {
        blocker: Arc::new(RwLock::new(blocker)),
        auth: Arc::new(auth),
        config: Arc::new(RwLock::new(config)),
    };

    // Build API routes
    let app = Router::new()
        .route("/api/health", get(health_check))
        .route("/api/setup", post(initial_setup))
        .route("/api/status", get(get_status))
        .route("/api/auth/verify", post(verify_password))
        .route("/api/blocklist", get(get_blocklist))
        .route("/api/blocklist", post(update_blocklist))
        .route("/api/stats", get(get_stats))
        .layer(CorsLayer::permissive())
        .with_state(state);

    let addr = SocketAddr::from(([127, 0, 0, 1], 8080));
    println!("🚀 Safeguard Blocker running on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

// API Handlers
async fn health_check() -> impl IntoResponse {
    Json(serde_json::json!({ "status": "ok" }))
}

#[derive(Deserialize)]
struct SetupRequest {
    master_password: String,
    auto_start: bool,
}

async fn initial_setup(
    axum::extract::State(state): axum::extract::State<AppState>,
    Json(payload): Json<SetupRequest>,
) -> Result<impl IntoResponse, StatusCode> {
    let mut config = state.config.write().await;
    
    if !config.is_first_run {
        return Err(StatusCode::BAD_REQUEST);
    }

    // Set master password
    state.auth.set_password(&payload.master_password)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    // Configure auto-start
    if payload.auto_start {
        platform::enable_auto_start()
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    }

    config.is_first_run = false;
    config.save().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    // Start protection
    let blocker = state.blocker.write().await;
    blocker.enable_protection()
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok(Json(serde_json::json!({ "success": true })))
}

async fn get_status(
    axum::extract::State(state): axum::extract::State<AppState>,
) -> impl IntoResponse {
    let blocker = state.blocker.read().await;
    let config = state.config.read().await;
    
    Json(serde_json::json!({
        "is_active": blocker.is_active(),
        "is_first_run": config.is_first_run,
        "platform": std::env::consts::OS,
        "protection_enabled": protection::is_protected(),
    }))
}

#[derive(Deserialize)]
struct PasswordRequest {
    password: String,
}

async fn verify_password(
    axum::extract::State(state): axum::extract::State<AppState>,
    Json(payload): Json<PasswordRequest>,
) -> Result<impl IntoResponse, StatusCode> {
    let is_valid = state.auth.verify_password(&payload.password)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    
    if is_valid {
        let token = state.auth.generate_session_token()
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        Ok(Json(serde_json::json!({ "token": token })))
    } else {
        Err(StatusCode::UNAUTHORIZED)
    }
}

async fn get_blocklist(
    axum::extract::State(state): axum::extract::State<AppState>,
) -> impl IntoResponse {
    let blocker = state.blocker.read().await;
    Json(blocker.get_blocklist())
}

#[derive(Deserialize)]
struct UpdateBlocklistRequest {
    token: String,
    domains: Vec<String>,
}

async fn update_blocklist(
    axum::extract::State(state): axum::extract::State<AppState>,
    Json(payload): Json<UpdateBlocklistRequest>,
) -> Result<impl IntoResponse, StatusCode> {
    // Verify admin token
    if !state.auth.verify_token(&payload.token) {
        return Err(StatusCode::UNAUTHORIZED);
    }

    let mut blocker = state.blocker.write().await;
    blocker.update_blocklist(payload.domains)
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok(Json(serde_json::json!({ "success": true })))
}

async fn get_stats(
    axum::extract::State(state): axum::extract::State<AppState>,
) -> impl IntoResponse {
    let blocker = state.blocker.read().await;
    Json(blocker.get_statistics())
}
