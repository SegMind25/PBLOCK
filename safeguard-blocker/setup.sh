#!/bin/bash

echo "🚀 Setting up Safeguard Blocker..."

# Check if running as root/sudo
if [ "$EUID" -ne 0 ]; then
  echo "⚠️  This script should be run with sudo privileges"
  echo "Please run: sudo ./setup.sh"
  exit 1
fi

# Create project structure
echo "📁 Creating project structure..."

mkdir -p safeguard-blocker/{backend/{src/{blocker,platform,auth,config,protection,database},resources},frontend/{src/{views,components,router,stores,api,assets/styles},public}}

# Create pnpm-workspace.yaml
cat >safeguard-blocker/pnpm-workspace.yaml <<'EOF'
packages:
  - 'frontend'
EOF

# Create .gitignore
cat >safeguard-blocker/.gitignore <<'EOF'
target/
node_modules/
dist/
.DS_Store
*.log
.env
*.json.lock
pnpm-lock.yaml
EOF

echo "✅ Project structure created"

# Check if Rust is installed
if ! command -v cargo &>/dev/null; then
  echo "❌ Rust is not installed"
  echo "Please install Rust from https://rustup.rs/"
  echo "Run: curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
  exit 1
fi

echo "✅ Rust is installed"

# Check if Node.js and pnpm are installed
if ! command -v node &>/dev/null; then
  echo "❌ Node.js is not installed"
  echo "Please install Node.js from https://nodejs.org/"
  exit 1
fi

if ! command -v pnpm &>/dev/null; then
  echo "📦 Installing pnpm..."
  npm install -g pnpm
fi

echo "✅ Node.js and pnpm are installed"

cd safeguard-blocker

echo "📦 Installing frontend dependencies..."
cd frontend
pnpm install
cd ..

echo "🔨 Building frontend..."
cd frontend
pnpm run build
cd ..

echo "🦀 Building Rust backend..."
cd backend
cargo build --release
cd ..

echo ""
echo "✅ Setup complete!"
echo ""
echo "To run the application:"
echo "  cd safeguard-blocker/backend"
echo "  sudo cargo run --release"
echo ""
echo "Then open your browser to: http://localhost:8080/setup"
echo ""
echo "⚠️  IMPORTANT NOTES:"
echo "1. The application requires sudo/admin privileges to modify system files"
echo "2. Set a strong master password during setup"
echo "3. Once activated, the blocker runs as a system service"
echo "4. The master password is required to change any settings"
echo ""
