pub mod crypto;
pub mod validator;

use std::env;

#[cfg(target_os = "windows")]
pub fn check_admin_privileges() -> bool {
    use std::os::windows::process::CommandExt;
    use std::process::Command;
    
    const CREATE_NO_WINDOW: u32 = 0x08000000;
    
    let output = Command::new("net")
        .args(&["session"])
        .creation_flags(CREATE_NO_WINDOW)
        .output();
    
    matches!(output, Ok(output) if output.status.success())
}

#[cfg(target_os = "linux")]
pub fn check_admin_privileges() -> bool {
    unsafe { libc::geteuid() == 0 }
}

#[cfg(target_os = "macos")]
pub fn check_admin_privileges() -> bool {
    unsafe { libc::geteuid() == 0 }
}

pub fn get_hosts_file_path() -> String {
    if cfg!(target_os = "windows") {
        "C:\\Windows\\System32\\drivers\\etc\\hosts".to_string()
    } else {
        "/etc/hosts".to_string()
    }
}