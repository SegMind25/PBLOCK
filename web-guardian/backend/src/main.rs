use actix_web::{middleware, web, App, HttpServer};
use actix_cors::Cors;
use dotenv::dotenv;
use std::env;
use tracing_subscriber::EnvFilter;

mod config;
mod database;
mod errors;
mod handlers;
mod middleware as app_middleware;
mod models;
mod services;
mod utils;

use config::Settings;
use database::connection::init_db;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    dotenv().ok();
    
    // Initialize logging
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| EnvFilter::new("info"))
        )
        .init();

    tracing::info!("🚀 Starting Web Guardian Backend...");

    // Load configuration
    let settings = Settings::new().expect("Failed to load configuration");
    
    // Initialize database
    let db_pool = init_db(&settings.database_url)
        .await
        .expect("Failed to initialize database");

    tracing::info!("✅ Database initialized");

    // Check if running with admin privileges
    if !utils::check_admin_privileges() {
        tracing::error!("❌ Application must run with administrator privileges");
        std::process::exit(1);
    }

    tracing::info!("✅ Running with administrator privileges");

    // Initialize services
    let host_manager = services::host_file_manager::HostFileManager::new();
    let process_monitor = services::process_monitor::ProcessMonitor::new();

    // Start process monitor in background
    let monitor_handle = tokio::spawn(async move {
        process_monitor.start_monitoring().await;
    });

    let server_host = settings.server_host.clone();
    let server_port = settings.server_port;

    tracing::info!("🌐 Server starting on {}:{}", server_host, server_port);

    // Create HTTP server
    let server = HttpServer::new(move || {
        let cors = Cors::default()
            .allowed_origin("http://localhost:5173")
            .allowed_origin("http://127.0.0.1:5173")
            .allowed_methods(vec!["GET", "POST", "PUT", "DELETE"])
            .allowed_headers(vec![
                actix_web::http::header::AUTHORIZATION,
                actix_web::http::header::ACCEPT,
                actix_web::http::header::CONTENT_TYPE,
            ])
            .max_age(3600);

        App::new()
            .app_data(web::Data::new(db_pool.clone()))
            .app_data(web::Data::new(host_manager.clone()))
            .wrap(cors)
            .wrap(middleware::Logger::default())
            .configure(handlers::config_routes)
    })
    .bind((server_host.as_str(), server_port))?
    .run();

    tracing::info!("✅ Web Guardian Backend is running!");

    // Run server
    tokio::select! {
        result = server => {
            result?;
        }
        _ = tokio::signal::ctrl_c() => {
            tracing::info!("🛑 Shutting down gracefully...");
        }
    }

    monitor_handle.abort();
    Ok(())
}