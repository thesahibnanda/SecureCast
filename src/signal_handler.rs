use tokio::signal;

pub async fn handle_shutdown_signal() {
    tokio::select! {
        _ = signal::ctrl_c() => {
            println!("Shutdown signal received. Cleaning up...");
        }
    }
}
