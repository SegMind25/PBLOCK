mod blocker;
mod server;
mod config;

use std::sync::Arc;
use tokio::sync::RwLock;

#[tokio::main]
async fn main() {
    println!("🛡️  Adult Content Blocker Starting...");
    
    // Initialize blocker
    let blocker = Arc::new(RwLock::new(blocker::Blocker::new()));
    
    // Load blocklist
    {
        let mut b = blocker.write().await;
        if let Err(e) = b.load_blocklist() {
            eprintln!("Warning: Could not load blocklist: {}", e);
        }
    }
    
    // Check if already activated
    if config::is_activated() {
        println!("✅ Blocker is already activated and running");
        // Apply system-level blocks
        if let Err(e) = blocker::apply_system_blocks() {
            eprintln!("Error applying system blocks: {}", e);
        }
    }
    
    // Start web server
    server::start_server(blocker).await;
}
