use std::fs;
use std::path::PathBuf;

fn get_config_path() -> PathBuf {
    if cfg!(target_os = "windows") {
        PathBuf::from(r"C:\ProgramData\AdultContentBlocker\config.lock")
    } else {
        PathBuf::from("/etc/adult-content-blocker/config.lock")
    }
}

pub fn is_activated() -> bool {
    get_config_path().exists()
}

pub fn set_activated() -> Result<(), Box<dyn std::error::Error>> {
    let config_path = get_config_path();
    
    // Create parent directory if it doesn't exist
    if let Some(parent) = config_path.parent() {
        fs::create_dir_all(parent)?;
    }
    
    // Create lock file
    fs::write(&config_path, "ACTIVATED")?;
    
    // Make it read-only (additional protection)
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mut perms = fs::metadata(&config_path)?.permissions();
        perms.set_mode(0o444); // Read-only
        fs::set_permissions(&config_path, perms)?;
    }
    
    #[cfg(windows)]
    {
        // On Windows, use attrib command to make it read-only and hidden
        std::process::Command::new("attrib")
            .args(&["+R", "+H", config_path.to_str().unwrap()])
            .output()?;
    }
    
    Ok(())
}
