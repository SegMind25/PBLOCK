Write-Host "🛡️  Installing Adult Content Blocker..." -ForegroundColor Green

# Check if running as administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "Please run as Administrator!" -ForegroundColor Red
    exit 1
}

# Install Rust
if (-not (Get-Command cargo -ErrorAction SilentlyContinue)) {
    Write-Host "Installing Rust..."
    Invoke-WebRequest -Uri "https://win.rustup.rs/x86_64" -OutFile "rustup-init.exe"
    .\rustup-init.exe -y
    Remove-Item rustup-init.exe
}

# Install Node.js and pnpm
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "Please install Node.js from https://nodejs.org/"
    exit 1
}

if (-not (Get-Command pnpm -ErrorAction SilentlyContinue)) {
    Write-Host "Installing pnpm..."
    npm install -g pnpm
}

# Build backend
Set-Location backend
Write-Host "Building Rust backend..."
cargo build --release

# Build frontend
Set-Location ..\frontend
Write-Host "Building Vue.js frontend..."
pnpm install
pnpm build

# Create Windows service
$servicePath = Join-Path $PSScriptRoot "backend\target\release\adult-content-blocker.exe"
$workingDir = Join-Path $PSScriptRoot "backend"

# Use NSSM to create service (download if needed)
if (-not (Test-Path "nssm.exe")) {
    Write-Host "Please install NSSM from https://nssm.cc/ to create Windows service"
}

Write-Host "✅ Build complete!" -ForegroundColor Green
Write-Host "🌐 Run the backend executable and access at: http://localhost:3000" -ForegroundColor Cyan
Write-Host "⚠️  Remember: Once activated, it cannot be disabled!" -ForegroundColor Yellow
