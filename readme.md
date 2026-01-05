🛡️ Safeguard Blocker
A cross-platform content filtering application built with Rust and Vue.js that provides unstoppable protection against harmful websites.

Features
✅ Cross-platform: Works on Windows, macOS, and Linux
🔒 Password-protected: Master password required for all configuration changes
🚫 Multi-layer blocking: Hosts file + DNS + HTTP proxy filtering
🔄 Auto-start: Runs automatically on system boot
💪 Self-protecting: Cannot be easily disabled or terminated
📊 Statistics: Track blocked attempts and view analytics
🎨 Modern UI: Clean, responsive Vue.js interface
Prerequisites
Rust (1.70+): Install from rustup.rs
Node.js (18+): Install from nodejs.org
pnpm: Will be installed automatically
Admin/sudo privileges: Required for system-level modifications
Quick Start
Option 1: Automated Setup (Linux/macOS)
bash
chmod +x setup.sh
sudo ./setup.sh
Option 2: Manual Setup
Create the files manually using the artifacts I provided above
Install dependencies:
bash
# Install pnpm globally
npm install -g pnpm

# Install frontend dependencies
cd frontend
pnpm install
cd ..
Build the frontend:
bash
cd frontend
pnpm run build
cd ..
Build the backend:
bash
cd backend
cargo build --release
Run the application:
bash
# Linux/macOS
cd backend
sudo cargo run --release

# Windows (run as Administrator)
cd backend
cargo run --release
Open your browser:
Navigate to http://localhost:8080/setup to complete the initial setup.

File Structure
safeguard-blocker/
├── backend/                    # Rust backend
│   ├── src/
│   │   ├── main.rs            # Main application entry
│   │   ├── blocker/           # Blocking engine
│   │   ├── platform/          # OS-specific code
│   │   ├── auth/              # Authentication
│   │   ├── config/            # Configuration
│   │   ├── protection/        # Self-protection
│   │   └── database/          # Data storage
│   ├── resources/
│   │   └── blocklist.json     # Default blocklist
│   └── Cargo.toml
│
├── frontend/                   # Vue.js frontend
│   ├── src/
│   │   ├── main.ts
│   │   ├── App.vue
│   │   ├── views/             # Pages
│   │   ├── components/        # UI components
│   │   ├── router/            # Routing
│   │   ├── stores/            # State management
│   │   └── api/               # API client
│   └── package.json
│
└── pnpm-workspace.yaml
How It Works
1. Hosts File Blocking
Modifies /etc/hosts (Linux/macOS) or C:\Windows\System32\drivers\etc\hosts (Windows) to redirect blocked domains to 127.0.0.1.

2. DNS Filtering
Intercepts DNS queries and blocks resolution of harmful domains.

3. System Service
Runs as a background service:

Linux: systemd service
macOS: launchd daemon
Windows: Windows Service
4. Self-Protection
Ignores termination signals
High process priority
Auto-restart on failure
Password-protected configuration
Usage
First-Time Setup
Launch the application
Navigate to the setup page
Create a strong master password
Choose auto-start option
Complete setup
Managing Blocklist
Go to Settings (requires password)
Add or remove domains
Changes apply immediately
Viewing Statistics
Visit the Dashboard to see:

Protection status
Blocked attempts today
Total blocks
Recent activity
Configuration
Configuration files are stored in:

Linux: ~/.config/safeguard-blocker/
macOS: ~/Library/Application Support/safeguard-blocker/
Windows: %APPDATA%\safeguard-blocker\
Development
Frontend Development
bash
cd frontend
pnpm run dev
The dev server runs on http://localhost:3000 with proxy to backend.

Backend Development
bash
cd backend
cargo watch -x run
Building for Production
bash
# Frontend
cd frontend
pnpm run build

# Backend
cd backend
cargo build --release
Platform-Specific Notes
Linux
Requires root privileges:

bash
sudo ./target/release/safeguard-blocker
macOS
May require disabling System Integrity Protection for some features:

bash
csrutil disable  # In recovery mode
Windows
Must run as Administrator. Use the installer or:

powershell
Start-Process -FilePath ".\target\release\safeguard-blocker.exe" -Verb RunAs
Troubleshooting
"Permission denied" errors
Ensure you're running with sudo/admin privileges
Check file permissions in config directory
Service won't start
Check system logs: journalctl -u safeguard-blocker (Linux)
Verify service installation
Ensure no port conflicts (8080)
Blocklist not updating
Verify master password is correct
Check hosts file permissions
Restart the service
Security Considerations
Master password: Use a strong, unique password
File permissions: Config files are readable only by root/admin
Network: Runs locally on 127.0.0.1 only
Updates: Keep the blocklist updated regularly
Contributing
Contributions are welcome! Please:

Fork the repository
Create a feature branch
Make your changes
Submit a pull request
License
MIT License - see LICENSE file for details

Disclaimer
This software is intended for personal use to help with self-control and digital wellbeing. It should not be used to restrict others without their consent. Users are responsible for compliance with local laws and regulations.

Support
For issues and feature requests, please open an issue on GitHub.

Made with 🦀 Rust and 💚 Vue.js


