use std::time::Duration;
use sysinfo::System;
use tokio::time::sleep;

pub struct ProcessMonitor {
    system: System,
}

impl ProcessMonitor {
    pub fn new() -> Self {
        ProcessMonitor {
            system: System::new_all(),
        }
    }

    pub async fn start_monitoring(&mut self) {
        tracing::info!("🔍 Process monitor started");

        loop {
            self.system.refresh_all();
            
            // Check for suspicious processes trying to modify system files
            self.check_suspicious_processes();
            
            // Check if hosts file was tampered with
            self.check_hosts_file_integrity();

            sleep(Duration::from_secs(5)).await;
        }
    }

    fn check_suspicious_processes(&self) {
        let suspicious_names = vec![
            "notepad.exe",
            "vim",
            "nano",
            "gedit",
            "code",
            "sublime",
        ];

        for (_, process) in self.system.processes() {
            let process_name = process.name().to_lowercase();
            
            for suspicious in &suspicious_names {
                if process_name.contains(suspicious) {
                    let cmd = process.cmd();
                    for arg in cmd {
                        if arg.to_lowercase().contains("hosts") {
                            tracing::warn!(
                                "⚠️  Suspicious process detected: {} trying to access hosts file",
                                process.name()
                            );
                        }
                    }
                }
            }
        }
    }

    fn check_hosts_file_integrity(&self) {
        // This would check if the hosts file has been modified unexpectedly
        // Implementation would involve keeping a hash of the file and comparing
        tracing::debug!("Checking hosts file integrity...");
    }

    pub fn is_process_running(&self, process_name: &str) -> bool {
        self.system.processes().iter().any(|(_, process)| {
            process.name().to_lowercase().contains(&process_name.to_lowercase())
        })
    }
}
