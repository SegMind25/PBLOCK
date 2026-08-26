#!/bin/bash
# ============================================================
# PBLOCK - Private DNS Setup Guide for Android
# ============================================================
# This script configures Private DNS on Android 9+ to block
# NSFW content WITHOUT requiring root access.
#
# It uses ADB to set the Private DNS provider to a family-safe
# DNS service that blocks adult content automatically.
#
# Requirements:
#   - Android 9 (Pie) or higher
#   - ADB installed on your computer
#   - USB Debugging enabled on your phone
#   - Phone connected via USB cable
#
# Supported DNS providers (all block adult content):
#   1. CleanBrowsing Family Filter (recommended)
#   2. AdGuard Family Protection
#   3. OpenDNS FamilyShield
#   4. Cloudflare for Families
# ============================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  PBLOCK - Private DNS Setup (No Root)${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check if ADB is installed
if ! command -v adb &> /dev/null; then
    echo -e "${RED}ADB is not installed.${NC}"
    echo ""
    echo -e "${YELLOW}You can set this up manually on your phone:${NC}"
    echo ""
    echo "  1. Open Settings"
    echo "  2. Go to Network & Internet (or Connections)"
    echo "  3. Tap 'Private DNS' (or 'More connection settings' > 'Private DNS')"
    echo "  4. Select 'Private DNS provider hostname'"
    echo "  5. Enter one of these hostnames:"
    echo ""
    echo -e "     ${CYAN}family-filter-dns.cleanbrowsing.org${NC}  (CleanBrowsing - recommended)"
    echo -e "     ${CYAN}dns.adguard-dns.com${NC}                  (AdGuard Family)"
    echo -e "     ${CYAN}family.cloudflare-dns.com${NC}            (Cloudflare for Families)"
    echo ""
    echo "  6. Tap 'Save'"
    echo ""
    echo "That's it! Adult content will be blocked on all apps and browsers."
    exit 0
fi

# Check for connected device
echo -e "${YELLOW}Checking for connected Android device...${NC}"
DEVICE_COUNT=$(adb devices | grep -c "device$" || true)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${RED}No Android device found.${NC}"
    echo ""
    echo "Connect your phone via USB with USB Debugging enabled."
    echo "Or set up Private DNS manually (see instructions above)."
    exit 1
fi

echo -e "${GREEN}Device found!${NC}"
echo ""

# Check Android version
ANDROID_VERSION=$(adb shell getprop ro.build.version.sdk 2>/dev/null || echo "0")
if [ "$ANDROID_VERSION" -lt 28 ]; then
    echo -e "${RED}Error: Your device is running Android $(adb shell getprop ro.build.version.release).${NC}"
    echo "Private DNS requires Android 9 (Pie) or higher."
    echo ""
    echo "For older Android versions, use the ADB hosts method instead:"
    echo "  ./block_nsfw_adb.sh"
    exit 1
fi

echo "Choose a DNS provider that blocks adult content:"
echo ""
echo -e "  1) ${CYAN}CleanBrowsing Family Filter${NC} (recommended)"
echo "     Blocks adult content, mixed content, and malware"
echo ""
echo -e "  2) ${CYAN}AdGuard Family Protection${NC}"
echo "     Blocks adult content, ads, and trackers"
echo ""
echo -e "  3) ${CYAN}Cloudflare for Families${NC}"
echo "     Blocks adult content and malware (fast DNS)"
echo ""
echo "  4) Remove Private DNS (restore default)"
echo ""
echo "  5) Check current Private DNS status"
echo ""
read -p "Enter choice (1-5): " choice

case $choice in
    1)
        DNS_HOST="family-filter-dns.cleanbrowsing.org"
        DNS_NAME="CleanBrowsing Family Filter"
        ;;
    2)
        DNS_HOST="dns.adguard-dns.com"
        DNS_NAME="AdGuard Family Protection"
        ;;
    3)
        DNS_HOST="family.cloudflare-dns.com"
        DNS_NAME="Cloudflare for Families"
        ;;
    4)
        echo ""
        echo -e "${YELLOW}Removing Private DNS configuration...${NC}"
        adb shell "settings put global private_dns_mode off" 2>/dev/null
        echo -e "${GREEN}Private DNS removed. Default DNS restored.${NC}"
        echo ""
        echo "Note: It may take a few minutes for the change to take effect."
        echo "Toggle airplane mode on/off to apply changes immediately."
        exit 0
        ;;
    5)
        echo ""
        echo -e "${YELLOW}Checking Private DNS status...${NC}"
        DNS_MODE=$(adb shell "settings get global private_dns_mode" 2>/dev/null)
        DNS_PROVIDER=$(adb shell "settings get global private_dns_specifier" 2>/dev/null)

        if [ "$DNS_MODE" = "hostname" ] && [ -n "$DNS_PROVIDER" ] && [ "$DNS_PROVIDER" != "null" ]; then
            echo -e "${GREEN}Private DNS: ACTIVE${NC}"
            echo -e "${GREEN}Provider: $DNS_PROVIDER${NC}"

            case "$DNS_PROVIDER" in
                *cleanbrowsing*) echo -e "${GREEN}Type: CleanBrowsing Family Filter${NC}" ;;
                *adguard*) echo -e "${GREEN}Type: AdGuard Family Protection${NC}" ;;
                *cloudflare*) echo -e "${GREEN}Type: Cloudflare for Families${NC}" ;;
                *) echo -e "${YELLOW}Type: Custom DNS provider${NC}" ;;
            esac
        else
            echo -e "${RED}Private DNS: NOT CONFIGURED${NC}"
            echo "Run this script again and choose a DNS provider."
        fi
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid choice.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${YELLOW}Setting Private DNS to $DNS_NAME...${NC}"

# Set Private DNS mode to hostname
adb shell "settings put global private_dns_mode hostname"

# Set the DNS provider hostname
adb shell "settings put global private_dns_specifier $DNS_HOST"

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Private DNS configured successfully!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "Provider: ${CYAN}$DNS_NAME${NC}"
echo -e "Hostname: ${CYAN}$DNS_HOST${NC}"
echo ""
echo "Adult content is now blocked on ALL apps and browsers."
echo ""
echo "To verify:"
echo "  1. Open your phone's Settings"
echo "  2. Go to Network & Internet > Private DNS"
echo "  3. You should see: $DNS_HOST"
echo ""
echo "Toggle airplane mode on/off if changes don't apply immediately."
echo ""
echo -e "${YELLOW}Note: This blocks content at the DNS level. It works on${NC}"
echo -e "${YELLOW}all apps (Chrome, Firefox, social media, etc.) without root.${NC}"
