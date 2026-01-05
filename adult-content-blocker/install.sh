#!/bin/bash

echo "🛡️  Installing Adult Content Blocker..."

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo ./install.sh)"
  exit 1
fi

# Install Rust if not present
if ! command -v cargo &>/dev/null; then
  echo "Installing Rust..."
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
  source $HOME/.cargo/env
fi

# Install Node.js and pnpm if not present
if ! command -v node &>/dev/null; then
  echo "Installing Node.js..."
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
fi

if ! command -v pnpm &>/dev/null; then
  echo "Installing pnpm..."
  npm install -g pnpm
fi

# Build backend
cd backend
echo "Building Rust backend..."
cargo build --release

# Build frontend
cd ../frontend
echo "Building Vue.js frontend..."
pnpm install
pnpm build

# Create systemd service
cat >/etc/systemd/system/adult-content-blocker.service <<EOF
[Unit]
Description=Adult Content Blocker
After=network.target

[Service]
Type=simple
ExecStart=$(pwd)/../backend/target/release/adult-content-blocker
WorkingDirectory=$(pwd)/../backend
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
systemctl daemon-reload
systemctl enable adult-content-blocker
systemctl start adult-content-blocker

echo "✅ Installation complete!"
echo "🌐 Access the blocker at: http://localhost:3000"
echo "⚠️  Remember: Once activated, it cannot be disabled!"
