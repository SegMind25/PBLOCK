-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    email TEXT,
    is_parent BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_code_hash TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    CONSTRAINT username_length CHECK (length(username) >= 4),
    CONSTRAINT email_format CHECK (email IS NULL OR email LIKE '%@%.%')
);

-- Create blocked_sites table
CREATE TABLE IF NOT EXISTS blocked_sites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    domain TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    added_by_user_id INTEGER NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (added_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT domain_format CHECK (length(domain) >= 3)
);

-- Create access_logs table
CREATE TABLE IF NOT EXISTS access_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    domain TEXT NOT NULL,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    user_agent TEXT,
    ip_address TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create settings table for application configuration
CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    key TEXT NOT NULL UNIQUE,
    value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indices for better query performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_is_parent ON users(is_parent);
CREATE INDEX IF NOT EXISTS idx_blocked_sites_domain ON blocked_sites(domain);
CREATE INDEX IF NOT EXISTS idx_blocked_sites_is_active ON blocked_sites(is_active);
CREATE INDEX IF NOT EXISTS idx_access_logs_domain ON access_logs(domain);
CREATE INDEX IF NOT EXISTS idx_access_logs_blocked ON access_logs(blocked);
CREATE INDEX IF NOT EXISTS idx_access_logs_timestamp ON access_logs(timestamp);

-- Insert default settings
INSERT OR IGNORE INTO settings (key, value) VALUES 
    ('app_initialized', 'false'),
    ('filter_enabled', 'true'),
    ('last_blocklist_update', ''),
    ('tamper_protection_enabled', 'true');

-- Insert default blocked sites (common adult content domains)
INSERT OR IGNORE INTO blocked_sites (domain, category, is_active, added_by_user_id, reason, created_at, updated_at)
SELECT 
    domain,
    'adult_content',
    TRUE,
    1,
    'Default blocklist',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    VALUES 
        ('pornhub.com'),
        ('xvideos.com'),
        ('xnxx.com'),
        ('redtube.com'),
        ('youporn.com'),
        ('xhamster.com'),
        ('tube8.com'),
        ('spankbang.com'),
        ('beeg.com'),
        ('txxx.com')
) AS default_sites(domain)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE is_parent = TRUE);