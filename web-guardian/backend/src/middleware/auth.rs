use actix_web::{
    dev::{forward_ready, Service, ServiceRequest, ServiceResponse, Transform},
    Error, HttpMessage,
};
use futures_util::future::LocalBoxFuture;
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use std::env;
use std::future::{ready, Ready};

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Claims {
    pub sub: i64,
    pub username: String,
    pub is_parent: bool,
    pub exp: usize,
}

impl Claims {
    pub fn new(user_id: i64, username: String, is_parent: bool, expiration_hours: i64) -> Self {
        let expiration = chrono::Utc::now()
            .checked_add_signed(chrono::Duration::hours(expiration_hours))
            .expect("valid timestamp")
            .timestamp() as usize;

        Claims {
            sub: user_id,
            username,
            is_parent,
            exp: expiration,
        }
    }

    pub fn encode(&self) -> Result<String, jsonwebtoken::errors::Error> {
        let secret = env::var("JWT_SECRET").expect("JWT_SECRET must be set");
        encode(
            &Header::default(),
            &self,
            &EncodingKey::from_secret(secret.as_ref()),
        )
    }

    pub fn decode(token: &str) -> Result<Self, jsonwebtoken::errors::Error> {
        let secret = env::var("JWT_SECRET").expect("JWT_SECRET must be set");
        let token_data = decode::<Claims>(
            token,
            &DecodingKey::from_secret(secret.as_ref()),
            &Validation::default(),
        )?;
        Ok(token_data.claims)
    }
}

pub struct AuthMiddleware;

impl<S, B> Transform<S, ServiceRequest> for AuthMiddleware
where
    S: Service<ServiceRequest, Response = ServiceResponse<B>, Error = Error>,
    S::Future: 'static,
    B: 'static,
{
    type Response = ServiceResponse<B>;
    type Error = Error;
    type InitError = ();
    type Transform = AuthMiddlewareService<S>;
    type Future = Ready<Result<Self::Transform, Self::InitError>>;

    fn new_transform(&self, service: S) -> Self::Future {
        ready(Ok(AuthMiddlewareService { service }))
    }
}

pub struct AuthMiddlewareService<S> {
    service: S,
}

impl<S, B> Service<ServiceRequest> for AuthMiddlewareService<S>
where
    S: Service<ServiceRequest, Response = ServiceResponse<B>, Error = Error>,
    S::Future: 'static,
    B: 'static,
{
    type Response = ServiceResponse<B>;
    type Error = Error;
    type Future = LocalBoxFuture<'static, Result<Self::Response, Self::Error>>;

    forward_ready!(service);

    fn call(&self, req: ServiceRequest) -> Self::Future {
        let auth_header = req.headers().get("Authorization");

        let token = match auth_header {
            Some(header) => {
                let header_str = match header.to_str() {
                    Ok(s) => s,
                    Err(_) => {
                        return Box::pin(async {
                            Err(actix_web::error::ErrorUnauthorized("Invalid authorization header"))
                        });
                    }
                };

                if let Some(token) = header_str.strip_prefix("Bearer ") {
                    token
                } else {
                    return Box::pin(async {
                        Err(actix_web::error::ErrorUnauthorized("Invalid authorization format"))
                    });
                }
            }
            None => {
                return Box::pin(async {
                    Err(actix_web::error::ErrorUnauthorized("Missing authorization header"))
                });
            }
        };

        let claims = match Claims::decode(token) {
            Ok(claims) => claims,
            Err(_) => {
                return Box::pin(async {
                    Err(actix_web::error::ErrorUnauthorized("Invalid token"))
                });
            }
        };

        req.extensions_mut().insert(claims);

        let fut = self.service.call(req);
        Box::pin(async move {
            let res = fut.await?;
            Ok(res)
        })
    }
}
