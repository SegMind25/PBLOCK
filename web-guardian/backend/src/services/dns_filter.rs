use crate::errors::AppError;
use std::net::IpAddr;
use trust_dns_resolver::config::{ResolverConfig, ResolverOpts};
use trust_dns_resolver::TokioAsyncResolver;

pub struct DnsFilter {
    resolver: TokioAsyncResolver,
}

impl DnsFilter {
    pub fn new() -> Result<Self, AppError> {
        let resolver = TokioAsyncResolver::tokio(
            ResolverConfig::default(),
            ResolverOpts::default(),
        );

        Ok(DnsFilter { resolver })
    }

    pub async fn resolve_domain(&self, domain: &str) -> Result<Vec<IpAddr>, AppError> {
        let response = self
            .resolver
            .lookup_ip(domain)
            .await
            .map_err(|e| AppError::SystemError(format!("DNS lookup failed: {}", e)))?;

        let ips: Vec<IpAddr> = response.iter().collect();
        Ok(ips)
    }

    pub async fn is_domain_accessible(&self, domain: &str) -> bool {
        self.resolve_domain(domain).await.is_ok()
    }
}