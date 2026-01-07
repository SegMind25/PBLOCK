use actix_web::{error::ResponseError, http::StatusCode, HttpResponse};
use serde::Serialize;
use std::fmt;

#[derive(Debug, Serialize)]
pub struct ErrorResponse {
    pub error: String,
    pub message: String,
    pub status: u16,
}

#[derive(Debug)]
pub enum AppError {
    DatabaseError(String),
    AuthenticationError(String),
    ValidationError(String),
    NotFoundError(String),
    InternalError(String),
    UnauthorizedError(String),
    AlreadyExistsError(String),
    SystemError(String),
}

impl fmt::Display for AppError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            AppError::DatabaseError(msg) => write!(f, "Database error: {}", msg),
            AppError::AuthenticationError(msg) => write!(f, "Authentication error: {}", msg),
            AppError::ValidationError(msg) => write!(f, "Validation error: {}", msg),
            AppError::NotFoundError(msg) => write!(f, "Not found: {}", msg),
            AppError::InternalError(msg) => write!(f, "Internal error: {}", msg),
            AppError::UnauthorizedError(msg) => write!(f, "Unauthorized: {}", msg),
            AppError::AlreadyExistsError(msg) => write!(f, "Already exists: {}", msg),
            AppError::SystemError(msg) => write!(f, "System error: {}", msg),
        }
    }
}

impl ResponseError for AppError {
    fn status_code(&self) -> StatusCode {
        match self {
            AppError::DatabaseError(_) => StatusCode::INTERNAL_SERVER_ERROR,
            AppError::AuthenticationError(_) => StatusCode::UNAUTHORIZED,
            AppError::ValidationError(_) => StatusCode::BAD_REQUEST,
            AppError::NotFoundError(_) => StatusCode::NOT_FOUND,
            AppError::InternalError(_) => StatusCode::INTERNAL_SERVER_ERROR,
            AppError::UnauthorizedError(_) => StatusCode::FORBIDDEN,
            AppError::AlreadyExistsError(_) => StatusCode::CONFLICT,
            AppError::SystemError(_) => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }

    fn error_response(&self) -> HttpResponse {
        let status = self.status_code();
        let error_response = ErrorResponse {
            error: format!("{:?}", self).split('(').next().unwrap_or("Error").to_string(),
            message: self.to_string(),
            status: status.as_u16(),
        };

        HttpResponse::build(status).json(error_response)
    }
}

impl From<sqlx::Error> for AppError {
    fn from(err: sqlx::Error) -> Self {
        AppError::DatabaseError(err.to_string())
    }
}

impl From<std::io::Error> for AppError {
    fn from(err: std::io::Error) -> Self {
        AppError::SystemError(err.to_string())
    }
}