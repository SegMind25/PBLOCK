#!/bin/bash

PROJECT_NAME="safeguard-blocker"

# Create root
mkdir -p "$PROJECT_NAME"

# Root files
touch "$PROJECT_NAME/pnpm-workspace.yaml"
touch "$PROJECT_NAME/.gitignore"
touch "$PROJECT_NAME/README.md"
touch "$PROJECT_NAME/LICENSE"

# Backend
mkdir -p "$PROJECT_NAME/backend/src/{platform,blocker,database,api,auth,protection,config}"
mkdir -p "$PROJECT_NAME/backend/resources/{windows,macos,linux}"

# Backend files
touch "$PROJECT_NAME/backend/Cargo.toml"
touch "$PROJECT_NAME/backend/build.rs"
touch "$PROJECT_NAME/backend/src/main.rs"
touch "$PROJECT_NAME/backend/src/lib.rs"

# Platform modules
touch "$PROJECT_NAME/backend/src/platform/mod.rs"
touch "$PROJECT_NAME/backend/src/platform/windows.rs"
touch "$PROJECT_NAME/backend/src/platform/macos.rs"
touch "$PROJECT_NAME/backend/src/platform/linux.rs"

# Blocker
touch "$PROJECT_NAME/backend/src/blocker/mod.rs"
touch "$PROJECT_NAME/backend/src/blocker/filter.rs"
touch "$PROJECT/MODULE/backend/src/blocker/dns_filter.rs"
touch "$PROJECT_NAME/backend/src/blocker/proxy.rs"
touch "$PROJECT_NAME/backend/src/blocker/hosts.rs"

# Database
touch "$PROJECT_NAME/backend/src/database/mod.rs"
touch "$PROJECT_NAME/backend/src/database/schema.rs"
touch "$PROJECT_NAME/backend/src/database/models.rs"

# API
touch "$PROJECT_NAME/backend/src/api/mod.rs"
touch "$PROJECT_NAME/backend/src/api/routes.rs"
touch "$PROJECT_NAME/backend/src/api/handlers.rs"

# Auth
touch "$PROJECT_NAME/backend/src/auth/mod.rs"
touch "$PROJECT_NAME/backend/src/auth/password.rs"

# Protection
touch "$PROJECT_NAME/backend/src/protection/mod.rs"
touch "$PROJECT_NAME/backend/src/protection/self_protect.rs"
touch "$PROJECT_NAME/backend/src/protection/tamper.rs"

# Config
touch "$PROJECT_NAME/backend/src/config/mod.rs"
touch "$PROJECT_NAME/backend/src/config/settings.rs"

# Resources
touch "$PROJECT_NAME/backend/resources/blocklist.json"
touch "$PROJECT_NAME/backend/resources/windows/service.xml"
touch "$PROJECT_NAME/backend/resources/macos/com.safeguard.plist"
touch "$PROJECT_NAME/backend/resources/linux/safeguard.service"

# Frontend
mkdir -p "$PROJECT_NAME/frontend/src/{views,components,router,stores,composables,api,assets/{styles,images}}"
mkdir -p "$PROJECT_NAME/frontend/public"

# Frontend files
touch "$PROJECT_NAME/frontend/package.json"
touch "$PROJECT_NAME/frontend/vite.config.ts"
touch "$PROJECT_NAME/frontend/tsconfig.json"
touch "$PROJECT_NAME/frontend/index.html"

touch "$PROJECT_NAME/frontend/src/main.ts"
touch "$PROJECT_NAME/frontend/src/App.vue"

# Views
touch "$PROJECT_NAME/frontend/src/views/Setup.vue"
touch "$PROJECT_NAME/frontend/src/views/Dashboard.vue"
touch "$PROJECT_NAME/frontend/src/views/Settings.vue"
touch "$PROJECT_NAME/frontend/src/views/Blocked.vue"
touch "$PROJECT_NAME/frontend/src/views/Stats.vue"

# Components
touch "$PROJECT_NAME/frontend/src/components/PasswordPrompt.vue"
touch "$PROJECT_NAME/frontend/src/components/BlocklistManager.vue"
touch "$PROJECT_NAME/frontend/src/components/SystemStatus.vue"
touch "$PROJECT_NAME/frontend/src/components/ProtectionToggle.vue"

# Router, Stores, Composables, API
touch "$PROJECT_NAME/frontend/src/router/index.ts"
touch "$PROJECT_NAME/frontend/src/stores/blocker.ts"
touch "$PROJECT_NAME/frontend/src/stores/auth.ts"
touch "$PROJECT_NAME/frontend/src/stores/settings.ts"
touch "$PROJECT_NAME/frontend/src/composables/useBlocker.ts"
touch "$PROJECT_NAME/frontend/src/composables/usePlatform.ts"
touch "$PROJECT_NAME/frontend/src/api/client.ts"

# Public
touch "$PROJECT_NAME/frontend/public/favicon.ico"

# Installer
mkdir -p "$PROJECT_NAME/installer/{windows,macos,linux/debian,linux/rpm}"

touch "$PROJECT_NAME/installer/windows/installer.nsi"
touch "$PROJECT_NAME/installer/windows/setup.iss"
touch "$PROJECT_NAME/installer/macos/Info.plist"
touch "$PROJECT_NAME/installer/macos/create-dmg.sh"
touch "$PROJECT_NAME/installer/linux/debian/control"
touch "$PROJECT_NAME/installer/linux/rpm/safeguard.spec"

# Shared
mkdir -p "$PROJECT_NAME/shared/types"
touch "$PROJECT_NAME/shared/types/api.ts"

# Scripts
mkdir -p "$PROJECT_NAME/scripts"
touch "$PROJECT_NAME/scripts/build-all.sh"
touch "$PROJECT_NAME/scripts/build-windows.sh"
touch "$PROJECT_NAME/scripts/build-macos.sh"
touch "$PROJECT_NAME/scripts/build-linux.sh"

echo "✅ Project structure for '$PROJECT_NAME' created successfully!"
