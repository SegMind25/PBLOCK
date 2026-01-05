use axum::{
    extract::State,
    http::StatusCode,
    response::IntoResponse,
    routing::{get, post},
    Json, Router,
};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::RwLock;
use tower_http::cors::CorsLayer;
use tower_http::services::ServeDir;

use crate::blocker::Blocker;
use crate::config;

#[derive(Clone)]
struct AppState {
    blocker: Arc<RwLock<Blocker>>,
}

#[derive(Serialize)]
struct StatusResponse {
    activated: bool,
    blocked_count: usize,
}

#[derive(Deserialize)]
struct ActivateRequest {
    password: String,
}

#[derive(Serialize)]
struct ActivateResponse {
    success: bool,
    message: String,
}

pub async fn start_server(blocker: Arc<RwLock<Blocker>>) {
    let state = AppState { blocker };
    
    let app = Router::new()
        .route("/api/status", get(get_status))
        .route("/api/activate", post(activate_blocker))
        .fallback_service(ServeDir::new("../frontend/dist"))
        .layer(CorsLayer::permissive())
        .with_state(state);
    
    let listener = tokio::net::TcpListener::bind("127.0.0.1:3000")
        .await
        .unwrap();
    
    println!("🚀 Server running on http://localhost:3000");
    
    axum::serve(listener, app).await.unwrap();
}

async fn get_status(State(state): State<AppState>) -> impl IntoResponse {
    let blocker = state.blocker.read().await;
    let response = StatusResponse {
        activated: config::is_activated(),
        blocked_count: blocker.blocked_domains.len(),
    };
    
    Json(response)
}

async fn activate_blocker(
    State(state): State<AppState>,
    Json(payload): Json<ActivateRequest>,
) -> impl IntoResponse {
    // Check if already activated
    if config::is_activated() {
        return (
            StatusCode::BAD_REQUEST,
            Json(ActivateResponse {
                success: false,
                message: "Blocker is already activated and cannot be disabled".to_string(),
            }),
        );
    }
    
    // Simple password check (in production, use proper authentication)
    if payload.password.len() < 6 {
        return (
            StatusCode::BAD_REQUEST,
            Json(ActivateResponse {
                success: false,
                message: "Password must be at least 6 characters".to_string(),
            }),
        );
    }
    
    // Apply system-level blocks
    if let Err(e) = crate::blocker::apply_system_blocks() {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(ActivateResponse {
                success: false,
                message: format!("Failed to apply blocks: {}", e),
            }),
        );
    }
    
    // Mark as activated
    if let Err(e) = config::set_activated() {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(ActivateResponse {
                success: false,
                message: format!("Failed to save configuration: {}", e),
            }),
        );
    }
    
    (
        StatusCode::OK,
        Json(ActivateResponse {
            success: true,
            message: "Blocker activated successfully! It will now run permanently.".to_string(),
        }),
    )
}
