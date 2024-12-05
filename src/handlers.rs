use crate::thread_safe_queue::ThreadSafeQueue;
use actix_web::{web, HttpResponse};
use log::{info, warn};
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Deserialize)]
pub struct AddUserRequest {
    data: String,
}

#[derive(Serialize)]
struct AddUserResponse {
    error: bool,
    is_added: bool,
}

#[derive(Serialize)]
struct GetUserResponse {
    error: bool,
    data: Option<String>,
    is_returned_data: bool,
}

#[derive(Serialize)]
struct HealthResponse {
    status: String,
}

#[derive(Serialize)]
struct MetricsResponse {
    queue_size: usize,
}

pub async fn add_user(
    queue: web::Data<Arc<ThreadSafeQueue<String>>>,
    req: web::Json<AddUserRequest>,
) -> HttpResponse {
    info!("Received request to add user: {:?}", req.data);

    if req.data.is_empty() {
        warn!("Invalid request payload. 'data' field is missing or empty.");
        return HttpResponse::BadRequest().json(AddUserResponse {
            error: true,
            is_added: false,
        });
    }

    let is_added = queue.enqueue(req.data.clone());
    if !is_added {
        warn!("Queue is full. Unable to add user: {:?}", req.data);
        return HttpResponse::TooManyRequests().json(AddUserResponse {
            error: true,
            is_added: false,
        });
    }

    info!("User added to the queue successfully: {:?}", req.data);
    HttpResponse::Created().json(AddUserResponse {
        error: false,
        is_added: true,
    })
}

pub async fn get_user(queue: web::Data<Arc<ThreadSafeQueue<String>>>) -> HttpResponse {
    info!("Received request to fetch next user.");

    match queue.dequeue() {
        Some(user_data) => {
            info!("User fetched from queue successfully: {:?}", user_data);
            HttpResponse::Ok().json(GetUserResponse {
                error: false,
                data: Some(user_data),
                is_returned_data: true, // Indicate data was successfully returned
            })
        }
        None => {
            warn!("Queue is empty. No users to fetch.");
            HttpResponse::Ok().json(GetUserResponse {
                error: false,
                data: None,
                is_returned_data: false, // Indicate no data was returned
            })
        }
    }
}


pub async fn health_check() -> HttpResponse {
    info!("Health check requested.");
    HttpResponse::Ok().json(HealthResponse {
        status: "healthy".to_string(),
    })
}

pub async fn metrics(queue: web::Data<Arc<ThreadSafeQueue<String>>>) -> HttpResponse {
    let queue_size = queue.size();
    info!("Metrics requested. Queue size: {}", queue_size);
    HttpResponse::Ok().json(MetricsResponse { queue_size })
}
