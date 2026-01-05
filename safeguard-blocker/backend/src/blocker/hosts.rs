use std::fs::{self, OpenOptions};
use std::io::{Read, Write};

pub struct HostsFileManager {
    hosts_path: PathBuf,
    marker_start: String,
    marker_end: String,
}

impl HostsFileManager {
    pub fn new() -> Result<Self, Box<dyn std::error::Error>> {
        let hosts_path = if cfg!(windows) {
            PathBuf::from("C:\\Windows\\System32\\drivers\\etc\\hosts")
        } else {
            PathBuf::from("/etc/hosts")
        };

        Ok(Self {
            hosts_path,
            marker_start: "# BEGIN SAFEGUARD BLOCKER".to_string(),
            marker_end: "# END SAFEGUARD BLOCKER".to_string(),
        })
    }

    pub fn update_blocked_domains(&self, domains: &[String]) -> Result<(), Box<dyn std::error::Error>> {
        // Read current hosts file
        let mut content = String::new();
        if self.hosts_path.exists() {
            let mut file = fs::File::open(&self.hosts_path)?;
            file.read_to_string(&mut content)?;
        }

        // Remove old blocked section
        content = self.remove_old_section(&content);

        // Add new blocked section
        let mut blocked_section = format!("\n{}\n", self.marker_start);
        for domain in domains {
            blocked_section.push_str(&format!("127.0.0.1 {}\n", domain));
            blocked_section.push_str(&format!("127.0.0.1 www.{}\n", domain));
        }
        blocked_section.push_str(&format!("{}\n", self.marker_end));

        content.push_str(&blocked_section);

        // Write back to hosts file (requires admin/root)
        let mut file = OpenOptions::new()
            .write(true)
            .truncate(true)
            .create(true)
            .open(&self.hosts_path)?;
        
        file.write_all(content.as_bytes())?;

        Ok(())
    }

    fn remove_old_section(&self, content: &str) -> String {
        if let Some(start) = content.find(&self.marker_start) {
            if let Some(end) = content[start..].find(&self.marker_end) {
                let before = &content[..start];
                let after = &content[start + end + self.marker_end.len()..];
                return format!("{}{}", before, after);
            }
        }
        content.to_string()
    }
}
