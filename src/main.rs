mod handlers;
mod signal_handler;
mod thread_safe_queue;

use actix_cors::Cors;
use actix_web::{middleware, web, App, HttpServer};
use log::{error, info};
use std::env;
use std::sync::Arc;
use thread_safe_queue::ThreadSafeQueue;

#[tokio::main]
async fn main() -> std::io::Result<()> {
    if env::var("RUST_LOG").is_err() {
        env::set_var("RUST_LOG", "info");
    }

    env_logger::init();
    info!("Starting InvisibleQueue service...");

    let queue = Arc::new(ThreadSafeQueue::<String>::new(10_000));

    let server = HttpServer::new(move || {
        let cors = Cors::default()
            .allow_any_origin()
            .allow_any_method()
            .allow_any_header();

        App::new()
            .wrap(cors)
            .wrap(middleware::Logger::default())
            .app_data(web::Data::new(queue.clone()))
            .route("/add-user", web::post().to(handlers::add_user))
            .route("/get-user", web::get().to(handlers::get_user))
            .route("/healthz", web::get().to(handlers::health_check))
            .route("/metrics", web::get().to(handlers::metrics))
    })
    .bind(("0.0.0.0", 8080))?;

    tokio::select! {
        res = server.run() => {
            if let Err(err) = res {
                error!("HttpServer error: {}", err);
            }
        }
        _ = signal_handler::handle_shutdown_signal() => {
            info!("Shutdown signal received. Shutting down server...");
        }
    }

    info!("Server shut down gracefully.");
    Ok(())
}
