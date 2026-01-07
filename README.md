# Web Guardian - Parental Control Application

A robust parental control application built with Rust (backend) and Vue.js (frontend) to prevent access to inappropriate websites.

## 🏗️ Project Structure

```
web-guardian/
├── README.md
├── .gitignore
├── docker-compose.yml (optional)
│
├── backend/                      # Rust Backend
│   ├── Cargo.toml
│   ├── Cargo.lock
│   ├── .env.example
│   ├── src/
│   │   ├── main.rs
│   │   ├── lib.rs
│   │   ├── config/
│   │   │   ├── mod.rs
│   │   │   └── settings.rs
│   │   ├── models/
│   │   │   ├── mod.rs
│   │   │   ├── user.rs
│   │   │   ├── blocked_site.rs
│   │   │   └── access_log.rs
│   │   ├── services/
│   │   │   ├── mod.rs
│   │   │   ├── dns_filter.rs
│   │   │   ├── host_file_manager.rs
│   │   │   ├── process_monitor.rs
│   │   │   └── encryption.rs
│   │   ├── handlers/
│   │   │   ├── mod.rs
│   │   │   ├── auth.rs
│   │   │   ├── filter.rs
│   │   │   └── health.rs
│   │   ├── middleware/
│   │   │   ├── mod.rs
│   │   │   ├── auth.rs
│   │   │   └── cors.rs
│   │   ├── database/
│   │   │   ├── mod.rs
│   │   │   ├── schema.rs
│   │   │   └── connection.rs
│   │   ├── utils/
│   │   │   ├── mod.rs
│   │   │   ├── crypto.rs
│   │   │   └── validator.rs
│   │   └── errors/
│   │       ├── mod.rs
│   │       └── app_error.rs
│   │
│   └── migrations/
│       └── 001_initial_setup.sql
│
├── frontend/                     # Vue.js Frontend
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── vite.config.js
│   ├── index.html
│   ├── .env.example
│   ├── public/
│   │   └── favicon.ico
│   │
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── assets/
│       │   ├── styles/
│       │   │   ├── main.css
│       │   │   └── variables.css
│       │   └── images/
│       │       └── logo.png
│       ├── components/
│       │   ├── common/
│       │   │   ├── Button.vue
│       │   │   ├── Input.vue
│       │   │   ├── Modal.vue
│       │   │   └── Loader.vue
│       │   ├── layout/
│       │   │   ├── Header.vue
│       │   │   ├── Sidebar.vue
│       │   │   └── Footer.vue
│       │   └── dashboard/
│       │       ├── StatusCard.vue
│       │       ├── BlockedSitesList.vue
│       │       └── ActivityLog.vue
│       ├── views/
│       │   ├── InitialSetup.vue
│       │   ├── ParentLogin.vue
│       │   ├── Dashboard.vue
│       │   ├── Settings.vue
│       │   └── Locked.vue
│       ├── router/
│       │   └── index.js
│       ├── stores/
│       │   ├── auth.js
│       │   ├── filter.js
│       │   └── settings.js
│       ├── services/
│       │   ├── api.js
│       │   ├── auth.service.js
│       │   └── filter.service.js
│       ├── composables/
│       │   ├── useAuth.js
│       │   └── useFilter.js
│       └── utils/
│           ├── constants.js
│           ├── validators.js
│           └── helpers.js
│
└── installer/                    # System Installer
    ├── windows/
    │   ├── setup.nsi            # NSIS installer script
    │   └── install.ps1          # PowerShell installer
    ├── linux/
    │   ├── install.sh
    │   └── systemd/
    │       └── web-guardian.service
    └── macos/
        └── install.sh
```

## 🎯 Features

### Core Functionality
- **Persistent Protection**: Once activated, cannot be disabled without parent authentication
- **System-Level Blocking**: Modifies hosts file and DNS settings for robust filtering
- **Process Monitoring**: Prevents tampering with system files or services
- **Encrypted Storage**: All sensitive data encrypted at rest
- **Stealth Mode**: Minimal UI footprint, runs as system service

### Parent Dashboard
- Initial setup wizard for parent authentication
- Real-time monitoring of blocked attempts
- Custom website blocking rules
- Activity logs and reports
- Emergency override with secure PIN

### Technical Features
- **Backend**: Actix-web framework with async runtime
- **Database**: SQLite with encryption
- **Frontend**: Vue 3 with Composition API
- **State Management**: Pinia
- **Build Tool**: Vite + pnpm
- **Security**: JWT authentication, bcrypt password hashing

## 🔧 Technology Stack

### Backend
- **Rust** 1.75+
- **Actix-web** - Web framework
- **SQLx** - Database toolkit
- **Tokio** - Async runtime
- **Argon2** - Password hashing
- **jsonwebtoken** - JWT handling
- **sysinfo** - System monitoring

### Frontend
- **Vue.js** 3.4+
- **Vite** 5.0+
- **Pinia** - State management
- **Vue Router** 4+
- **Axios** - HTTP client
- **TailwindCSS** - Styling

## 📋 Prerequisites

- **Rust**: 1.75 or higher
- **Node.js**: 18.x or higher
- **pnpm**: 8.x or higher
- **Administrator/Root privileges**: Required for system-level modifications

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone git@github.com:SegMind25/PBLOCK.git
cd web-guardian
```

### 2. Backend Setup
```bash
cd backend
cp .env.example .env
# Edit .env with your configuration
cargo build --release
```

### 3. Frontend Setup
```bash
cd ../frontend
pnpm install
cp .env.example .env
# Edit .env with your configuration
pnpm build
```

### 4. Run the Application
```bash
# Backend (from backend directory)
cargo run --release

# Frontend Development (from frontend directory)
pnpm dev

# Frontend Production Build
pnpm build
pnpm preview
```

## 🔐 Security Considerations

1. **Run with appropriate privileges**: The application requires admin/root access to modify system files
2. **Secure your parent PIN**: Use a strong, unique password
3. **Regular updates**: Keep the blocklist updated
4. **Backup configuration**: Store your recovery codes safely

## 📝 Configuration

### Backend (.env)
```env
DATABASE_URL=sqlite://web_guardian.db
JWT_SECRET=your-super-secret-jwt-key
SERVER_HOST=127.0.0.1
SERVER_PORT=8080
RUST_LOG=info
```

### Frontend (.env)
```env
VITE_API_URL=http://localhost:8080
VITE_APP_TITLE=Web Guardian
```

## 🛠️ Development

### Running Tests
```bash
# Backend tests
cd backend
cargo test

# Frontend tests
cd frontend
pnpm test
```

### Code Formatting
```bash
# Rust
cargo fmt

# Vue.js
pnpm format
```

## 📦 Building for Production

### Create System Installer
```bash
# Windows
cd installer/windows
makensis setup.nsi

# Linux
cd installer/linux
chmod +x install.sh
./install.sh

# macOS
cd installer/macos
chmod +x install.sh
./install.sh
```

## 🔄 Update Strategy

- Application checks for updates on startup
- Parent can review and approve updates
- Automatic backup before updates

## ⚠️ Important Notes

1. **One-Time Setup**: The initial configuration is crucial - store recovery codes safely
2. **System Modifications**: This app modifies system files (hosts, DNS) - backup before installation
3. **Tamper Protection**: The service runs with system privileges and monitors for tampering
4. **Legal Compliance**: Ensure compliance with local laws regarding monitoring and parental controls

## 🤝 Contributing

This is a security-focused application. If you find vulnerabilities, please report them responsibly.

## 📄 License

MIT License - See LICENSE file for details

## 🆘 Support

For issues, questions, or support:
- Open an issue on GitHub
- Email: modistotube2004@gmail.com

---

**USING VUE.JS & RUST :)**
