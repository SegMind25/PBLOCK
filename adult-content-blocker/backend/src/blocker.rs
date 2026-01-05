use regex::Regex;
use std::fs;
use std::io::Write;
use std::process::Command;

pub struct Blocker {
    blocked_domains: Vec<String>,
    patterns: Vec<Regex>,
}

impl Blocker {
    pub fn new() -> Self {
        Self {
            blocked_domains: Vec::new(),
            patterns: Vec::new(),
        }
    }
    
    pub fn load_blocklist(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        let content = fs::read_to_string("blocklist.txt")?;
        
        for line in content.lines() {
            let line = line.trim();
            if line.is_empty() || line.starts_with('#') {
                continue;
            }
            
            self.blocked_domains.push(line.to_string());
            
            // Create regex pattern
            let pattern = line.replace(".", r"\.");
            let pattern = format!(r"(^|\.){}", pattern);
            if let Ok(regex) = Regex::new(&pattern) {
                self.patterns.push(regex);
            }
        }
        
        println!("📋 Loaded {} blocked domains", self.blocked_domains.len());
        Ok(())
    }
    
    pub fn is_blocked(&self, domain: &str) -> bool {
        for pattern in &self.patterns {
            if pattern.is_match(domain) {
                return true;
            }
        }
        false
    }
}

pub fn apply_system_blocks() -> Result<(), Box<dyn std::error::Error>> {
    // Modify hosts file
    modify_hosts_file()?;
    
    // Set up firewall rules (if possible)
    #[cfg(target_os = "linux")]
    setup_linux_firewall()?;
    
    #[cfg(target_os = "windows")]
    setup_windows_firewall()?;
    
    Ok(())
}

fn modify_hosts_file() -> Result<(), Box<dyn std::error::Error>> {
    let hosts_path = if cfg!(target_os = "windows") {
        r"C:\Windows\System32\drivers\etc\hosts"
    } else {
        "/etc/hosts"
    };
    
    let blocklist_content = fs::read_to_string("blocklist.txt")?;
    let mut hosts_content = fs::read_to_string(hosts_path)
        .unwrap_or_default();
    
    // Add marker
    if !hosts_content.contains("# ADULT-CONTENT-BLOCKER-START") {
        hosts_content.push_str("\n# ADULT-CONTENT-BLOCKER-START\n");
        
        for line in blocklist_content.lines() {
            let line = line.trim();
            if !line.is_empty() && !line.starts_with('#') {
                hosts_content.push_str(&format!("127.0.0.1 {}\n", line));
                hosts_content.push_str(&format!("127.0.0.1 www.{}\n", line));
            }
        }
        
        hosts_content.push_str("# ADULT-CONTENT-BLOCKER-END\n");
        
        // Write back (requires admin/root)
        fs::write(hosts_path, hosts_content)?;
        println!("✅ Hosts file updated");
    }
    
    Ok(())
}

#[cfg(target_os = "linux")]
fn setup_linux_firewall() -> Result<(), Box<dyn std::error::Error>> {
    // This requires root privileges
    Command::new("iptables")
        .args(&["-A", "OUTPUT", "-m", "string", "--string", "pornhub", "--algo", "bm", "-j", "DROP"])
        .output()?;
    
    Ok(())
}

#[cfg(target_os = "windows")]
fn setup_windows_firewall() -> Result<(), Box<dyn std::error::Error>> {
    // Windows firewall rules would go here
    // Requires admin privileges
    Ok(())
}
