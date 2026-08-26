# PBLOCK - Linux GUI Version

A GTK3 graphical parental content filtering tool for Linux.

## Build from source

```bash
# Install dependencies (Ubuntu/Debian)
sudo apt install libgtk-3-dev build-essential

# Build
make

# Run
sudo ./pblock
```

## Build .deb package

```bash
sudo apt install fakeroot
make deb
sudo dpkg -i pblock_1.0_amd64.deb
```

## Install

```bash
sudo make install
```

## Usage

```bash
sudo pblock          # Launch GUI
sudo pblock block    # Enable blocking (CLI)
sudo pblock unblock  # Disable blocking (CLI)
sudo pblock status   # Show status (CLI)
```

## Features

- Blocks 200+ adult websites
- Forces SafeSearch on Google and Bing
- Password-protected settings
- 10-puzzle challenge to disable blocking
- GUI with GTK3
- Survives system reboot (hosts file modification)

## Requirements

- Linux with root access
- GTK3 libraries
