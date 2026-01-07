use crate::errors::AppError;
use std::fs::{self, OpenOptions};
use std::io::{Read, Write};
use std::path::Path;
use crate::utils::get_hosts_file_path;

const GUARDIAN_MARKER_START: &str = "# === WEB GUARDIAN START ===";
const GUARDIAN_MARKER_END: &str = "# === WEB GUARDIAN END ===";

#[derive(Clone)]
pub struct HostFileManager {
    hosts_path: String,
}

impl HostFileManager {
    pub fn new() -> Self {
        HostFileManager {
            hosts_path: get_hosts_file_path(),
        }
    }

    pub fn add_blocked_domains(&self, domains: &[String]) -> Result<(), AppError> {
        let hosts_path = Path::new(&self.hosts_path);
        
        // Read current hosts file
        let mut content = String::new();
        if hosts_path.exists() {
            let mut file = fs::File::open(hosts_path)?;
            file.read_to_string(&mut content)?;
        }

        // Remove existing Web Guardian section if present
        content = self.remove_guardian_section(&content);

        // Build new blocked domains section
        let mut guardian_section = format!("\n{}\n", GUARDIAN_MARKER_START);
        for domain in domains {
            guardian_section.push_str(&format!("127.0.0.1 {}\n", domain));
            guardian_section.push_str(&format!("127.0.0.1 www.{}\n", domain));
            guardian_section.push_str(&format!("::1 {}\n", domain));
            guardian_section.push_str(&format!("::1 www.{}\n", domain));
        }
        guardian_section.push_str(&format!("{}\n", GUARDIAN_MARKER_END));

        // Append guardian section
        content.push_str(&guardian_section);

        // Write back to hosts file
        let mut file = OpenOptions::new()
            .write(true)
            .truncate(true)
            .create(true)
            .open(hosts_path)?;

        file.write_all(content.as_bytes())?;

        tracing::info!("✅ Added {} domains to hosts file", domains.len());
        
        // Flush DNS cache
        self.flush_dns_cache()?;

        Ok(())
    }

    pub fn remove_blocked_domains(&self, domains: &[String]) -> Result<(), AppError> {
        let hosts_path = Path::new(&self.hosts_path);
        
        if !hosts_path.exists() {
            return Ok(());
        }

        let mut content = String::new();
        let mut file = fs::File::open(hosts_path)?;
        file.read_to_string(&mut content)?;

        // Remove specific domains
        for domain in domains {
            content = content
                .lines()
                .filter(|line| {
                    !line.contains(domain) || 
                    (!line.starts_with("127.0.0.1") && !line.starts_with("::1"))
                })
                .collect::<Vec<_>>()
                .join("\n");
        }

        let mut file = OpenOptions::new()
            .write(true)
            .truncate(true)
            .open(hosts_path)?;

        file.write_all(content.as_bytes())?;

        tracing::info!("✅ Removed {} domains from hosts file", domains.len());
        
        self.flush_dns_cache()?;

        Ok(())
    }

    pub fn get_blocked_domains(&self) -> Result<Vec<String>, AppError> {
        let hosts_path = Path::new(&self.hosts_path);
        
        if !hosts_path.exists() {
            return Ok(Vec::new());
        }

        let mut content = String::new();
        let mut file = fs::File::open(hosts_path)?;
        file.read_to_string(&mut content)?;

        let mut domains = Vec::new();
        let mut in_guardian_section = false;

        for line in content.lines() {
            if line.contains(GUARDIAN_MARKER_START) {
                in_guardian_section = true;
                continue;
            }
            if line.contains(GUARDIAN_MARKER_END) {
                break;
            }

            if in_guardian_section && (line.starts_with("127.0.0.1") || line.starts_with("::1")) {
                if let Some(domain) = line.split_whitespace().nth(1) {
                    if !domain.starts_with("www.") {
                        domains.push(domain.to_string());
                    }
                }
            }
        }

        Ok(domains)
    }

    fn remove_guardian_section(&self, content: &str) -> String {
        let mut result = String::new();
        let mut skip = false;

        for line in content.lines() {
            if line.contains(GUARDIAN_MARKER_START) {
                skip = true;
                continue;
            }
            if line.contains(GUARDIAN_MARKER_END) {
                skip = false;
                continue;
            }
            if !skip {
                result.push_str(line);
                result.push('\n');
            }
        }

        result
    }

    fn flush_dns_cache(&self) -> Result<(), AppError> {
        #[cfg(target_os = "windows")]
        {
            use std::process::Command;
            Command::new("ipconfig")
                .args(&["/flushdns"])
                .output()
                .map_err(|e| AppError::SystemError(format!("Failed to flush DNS cache: {}", e)))?;
        }

        #[cfg(target_os = "linux")]
        {
            use std::process::Command;
            // Try systemd-resolved first
            let _ = Command::new("systemctl")
                .args(&["restart", "systemd-resolved"])
                .output();
            
            // Fallback to nscd
            let _ = Command::new("systemctl")
                .args(&["restart", "nscd"])
                .output();
        }

        #[cfg(target_os = "macos")]
        {
            use std::process::Command;
            Command::new("dscacheutil")
                .args(&["-flushcache"])
                .output()
                .map_err(|e| AppError::SystemError(format!("Failed to flush DNS cache: {}", e)))?;
            
            Command::new("killall")
                .args(&["-HUP", "mDNSResponder"])
                .output()
                .ok();
        }

        tracing::info!("✅ DNS cache flushed");
        Ok(())
    }

    pub fn backup_hosts_file(&self) -> Result<(), AppError> {
        let backup_path = format!("{}.backup", self.hosts_path);
        fs::copy(&self.hosts_path, backup_path)?;
        tracing::info!("✅ Hosts file backed up");
        Ok(())
    }

    pub fn restore_hosts_file(&self) -> Result<(), AppError> {
        let backup_path = format!("{}.backup", self.hosts_path);
        if Path::new(&backup_path).exists() {
            fs::copy(backup_path, &self.hosts_path)?;
            tracing::info!("✅ Hosts file restored from backup");
            Ok(())
        } else {
            Err(AppError::NotFoundError("Backup file not found".to_string()))
        }
    }
}