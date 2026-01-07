pub mod auth;
pub mod filter;
pub mod health;

use actix_web::web;

pub fn config_routes(cfg: &mut web::ServiceConfig) {
    cfg.service(
        web::scope("/api")
            .service(health::health_check)
            .service(
                web::scope("/auth")
                    .service(auth::register)
                    .service(auth::login)
                    .service(auth::check_setup)
                    .service(auth::get_current_user)
            )
            .service(
                web::scope("/filter")
                    .service(filter::get_blocked_sites)
                    .service(filter::add_blocked_site)
                    .service(filter::delete_blocked_site)
                    .service(filter::check_domain)
                    .service(filter::get_access_logs)
                    .service(filter::get_stats)
                    .service(filter::enable_filter)
                    .service(filter::disable_filter)
            ),
    );
}