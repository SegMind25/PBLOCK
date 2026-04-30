#!/bin/bash
# ============================================================
# PBLOCK - NSFW Content Blocker via ADB
# ============================================================
# This script pushes the NSFW-blocking hosts file to your
# Android phone using ADB (Android Debug Bridge).
#
# Requirements:
#   - ADB installed on your computer
#   - USB Debugging enabled on your phone
#   - Phone connected via USB cable
#
# No root required! Uses ADB remount to push the hosts file.
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HOSTS_FILE="$SCRIPT_DIR/nsfw_hosts.txt"
DEVICE_HOSTS="/system/etc/hosts"
BACKUP_FILE="$SCRIPT_DIR/hosts_backup_$(date +%Y%m%d_%H%M%S).txt"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  PBLOCK - NSFW Content Blocker (ADB)${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check if ADB is installed
if ! command -v adb &> /dev/null; then
    echo -e "${RED}Error: ADB is not installed.${NC}"
    echo ""
    echo "Install ADB:"
    echo "  - Windows: Download from https://developer.android.com/studio/releases/platform-tools"
    echo "  - macOS:   brew install android-platform-tools"
    echo "  - Linux:   sudo apt install adb"
    echo ""
    exit 1
fi

# Check if hosts file exists
if [ ! -f "$HOSTS_FILE" ]; then
    echo -e "${RED}Error: nsfw_hosts.txt not found at $HOSTS_FILE${NC}"
    echo "Make sure you're running this script from the scripts/ directory."
    exit 1
fi

# Check for connected device
echo -e "${YELLOW}Checking for connected Android device...${NC}"
DEVICE_COUNT=$(adb devices | grep -c "device$" || true)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${RED}Error: No Android device found.${NC}"
    echo ""
    echo "Steps to connect your phone:"
    echo "  1. Enable Developer Options: Settings > About Phone > Tap 'Build Number' 7 times"
    echo "  2. Enable USB Debugging: Settings > Developer Options > USB Debugging"
    echo "  3. Connect your phone via USB cable"
    echo "  4. Accept the USB debugging prompt on your phone"
    echo "  5. Run this script again"
    echo ""
    exit 1
fi

echo -e "${GREEN}Device found!${NC}"
echo ""

# Show menu
echo "What would you like to do?"
echo "  1) Block NSFW content (push hosts file)"
echo "  2) Remove NSFW blocking (restore default hosts)"
echo "  3) Check current blocking status"
echo "  4) Backup current hosts file"
echo ""
read -p "Enter choice (1-4): " choice

case $choice in
    1)
        echo ""
        echo -e "${YELLOW}Pushing NSFW-blocking hosts file to device...${NC}"
        echo ""

        # Try ADB root first
        echo "Requesting ADB root access..."
        adb root 2>/dev/null || true
        sleep 2

        # Try to remount system partition
        echo "Remounting system partition..."
        if adb remount 2>/dev/null; then
            echo -e "${GREEN}System remounted successfully.${NC}"
        else
            echo -e "${YELLOW}Trying alternative method...${NC}"
            adb shell "su -c 'mount -o remount,rw /system'" 2>/dev/null || {
                echo -e "${RED}Error: Cannot remount /system.${NC}"
                echo ""
                echo "Your device may not support ADB remount."
                echo "Try one of these alternatives:"
                echo "  1. Use the Private DNS method (see README.md)"
                echo "  2. Root your device first"
                echo "  3. Use a DNS-based blocker app from Play Store"
                echo ""
                exit 1
            }
        fi

        # Backup existing hosts file
        echo "Backing up current hosts file..."
        adb pull "$DEVICE_HOSTS" "$BACKUP_FILE" 2>/dev/null || true
        echo -e "${GREEN}Backup saved to: $BACKUP_FILE${NC}"

        # Push new hosts file
        echo "Pushing NSFW-blocking hosts file..."
        adb push "$HOSTS_FILE" "$DEVICE_HOSTS"

        # Set correct permissions
        adb shell "chmod 644 $DEVICE_HOSTS"

        # Remount read-only
        adb shell "mount -o remount,ro /system" 2>/dev/null || true

        # Flush DNS cache
        echo "Flushing DNS cache..."
        adb shell "ndc resolver flushdefaultif" 2>/dev/null || true
        adb shell "settings put global captive_portal_mode 0" 2>/dev/null || true

        echo ""
        echo -e "${GREEN}NSFW content blocking is now ACTIVE!${NC}"
        echo -e "${GREEN}Restart your browser for changes to take effect.${NC}"
        DOMAIN_COUNT=$(grep -c "^127.0.0.1" "$HOSTS_FILE" || true)
        echo -e "${GREEN}Blocked domains: $DOMAIN_COUNT${NC}"
        ;;

    2)
        echo ""
        echo -e "${YELLOW}Removing NSFW blocking...${NC}"

        # Create default hosts file
        DEFAULT_HOSTS=$(mktemp)
        echo "127.0.0.1 localhost" > "$DEFAULT_HOSTS"
        echo "::1 localhost" >> "$DEFAULT_HOSTS"

        adb root 2>/dev/null || true
        sleep 2
        adb remount 2>/dev/null || adb shell "su -c 'mount -o remount,rw /system'" 2>/dev/null

        adb push "$DEFAULT_HOSTS" "$DEVICE_HOSTS"
        adb shell "chmod 644 $DEVICE_HOSTS"
        adb shell "mount -o remount,ro /system" 2>/dev/null || true
        adb shell "ndc resolver flushdefaultif" 2>/dev/null || true

        rm -f "$DEFAULT_HOSTS"

        echo ""
        echo -e "${GREEN}NSFW blocking removed. Default hosts file restored.${NC}"
        ;;

    3)
        echo ""
        echo -e "${YELLOW}Checking blocking status...${NC}"
        echo ""

        HOSTS_CONTENT=$(adb shell "cat $DEVICE_HOSTS" 2>/dev/null)
        if echo "$HOSTS_CONTENT" | grep -q "CONTENT_BLOCKER_START"; then
            BLOCKED=$(echo "$HOSTS_CONTENT" | grep -c "^127.0.0.1" || true)
            echo -e "${GREEN}Status: NSFW blocking is ACTIVE${NC}"
            echo -e "${GREEN}Blocked entries: $BLOCKED${NC}"
        else
            echo -e "${RED}Status: NSFW blocking is INACTIVE${NC}"
        fi
        ;;

    4)
        echo ""
        echo -e "${YELLOW}Backing up hosts file...${NC}"
        adb pull "$DEVICE_HOSTS" "$BACKUP_FILE" 2>/dev/null
        echo -e "${GREEN}Backup saved to: $BACKUP_FILE${NC}"
        ;;

    *)
        echo -e "${RED}Invalid choice. Please run the script again.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}Done!${NC}"
