use regex::Regex;

pub fn is_valid_domain(domain: &str) -> bool {
    let domain_regex = Regex::new(
        r"^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$"
    ).unwrap();
    
    domain_regex.is_match(&domain.to_lowercase())
}

pub fn sanitize_domain(domain: &str) -> String {
    domain
        .trim()
        .to_lowercase()
        .trim_start_matches("http://")
        .trim_start_matches("https://")
        .trim_start_matches("www.")
        .split('/')
        .next()
        .unwrap_or("")
        .to_string()
}

pub fn is_valid_email(email: &str) -> bool {
    let email_regex = Regex::new(
        r"^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    ).unwrap();
    
    email_regex.is_match(email)
}

pub fn is_strong_password(password: &str) -> (bool, Vec<String>) {
    let mut errors = Vec::new();
    let mut is_valid = true;

    if password.len() < 8 {
        errors.push("Password must be at least 8 characters long".to_string());
        is_valid = false;
    }

    if !password.chars().any(|c| c.is_uppercase()) {
        errors.push("Password must contain at least one uppercase letter".to_string());
        is_valid = false;
    }

    if !password.chars().any(|c| c.is_lowercase()) {
        errors.push("Password must contain at least one lowercase letter".to_string());
        is_valid = false;
    }

    if !password.chars().any(|c| c.is_numeric()) {
        errors.push("Password must contain at least one number".to_string());
        is_valid = false;
    }

    if !password.chars().any(|c| "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(c)) {
        errors.push("Password must contain at least one special character".to_string());
        is_valid = false;
    }

    (is_valid, errors)
}