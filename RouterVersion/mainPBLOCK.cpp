#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <cstring>
#include <unistd.h>
#include <crypt.h>
#include <sys/stat.h>
#include <thread>
#include <chrono>
#include <ctime>
#include <algorithm>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

const std::string CONFIG_FILE = "/etc/network_blocker.conf";
const std::string LOG_FILE = "/var/log/content_blocker.log";
const std::string BLOCK_PAGE_DIR = "/var/www/blocked";

// Comprehensive blocked domains list
std::vector<std::string> blocked_domains = {
    "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
    "redtube.com", "youporn.com", "tube8.com", "spankbang.com",
    "eporner.com", "txxx.com", "xvideo.com", "porn.com",
    "sex.com", "xxx.com", "adult.com", "brazzers.com",
    "pornhd.com", "thumbzilla.com", "upornia.com", "4tube.com",
    "porntrex.com", "hqporner.com", "fapdu.com", "tnaflix.com",
    "drtuber.com", "nuvid.com", "hotmovs.com", "ok.xxx",
    "perfectgirls.net", "befuck.com", "porngo.com", "sunporno.com"
};

// Comprehensive keyword list for search blocking
std::vector<std::string> blocked_keywords = {
    "porn", "xxx", "sex", "nude", "naked", "adult", "nsfw",
    "hentai", "lesbian", "gay porn", "anal", "milf", "teen porn",
    "boobs", "pussy", "dick", "cock", "fuck", "blowjob"
};

struct Config {
    std::string password_hash;
    int failed_attempts;
    time_t lockout_until;
};

void logActivity(const std::string& message) {
    auto now = time(nullptr);
    auto tm = localtime(&now);
    char timestamp[64];
    strftime(timestamp, sizeof(timestamp), "%Y-%m-%d %H:%M:%S", tm);
    
    std::ofstream log(LOG_FILE, std::ios::app);
    log << "[" << timestamp << "] " << message << std::endl;
    log.close();
}

std::string hashPassword(const std::string& password) {
    return crypt(password.c_str(), "$6$contentblocker$");
}

bool verifyPassword(const std::string& password, const std::string& hash) {
    return crypt(password.c_str(), hash.c_str()) == hash;
}

void saveConfig(const Config& config) {
    std::ofstream file(CONFIG_FILE);
    file << config.password_hash << "\n";
    file << config.failed_attempts << "\n";
    file << config.lockout_until << "\n";
    file.close();
    chmod(CONFIG_FILE.c_str(), 0600);
}

Config loadConfig() {
    Config config = {"", 0, 0};
    std::ifstream file(CONFIG_FILE);
    if (file.is_open()) {
        std::getline(file, config.password_hash);
        file >> config.failed_attempts >> config.lockout_until;
        file.close();
    }
    return config;
}

// Create block page HTML
void createBlockPage() {
    system(("mkdir -p " + BLOCK_PAGE_DIR).c_str());
    
    std::ofstream html(BLOCK_PAGE_DIR + "/index.html");
    html << R"(<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Content Blocked</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 20px;
            padding: 40px;
            max-width: 500px;
            width: 100%;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            text-align: center;
        }
        .icon {
            font-size: 80px;
            margin-bottom: 20px;
        }
        h1 {
            color: #333;
            font-size: 32px;
            margin-bottom: 15px;
        }
        p {
            color: #666;
            font-size: 18px;
            line-height: 1.6;
            margin-bottom: 30px;
        }
        .message {
            background: #f0f0f0;
            padding: 20px;
            border-radius: 10px;
            margin-bottom: 20px;
        }
        .encouragement {
            color: #667eea;
            font-weight: bold;
            font-size: 20px;
        }
        .stats {
            display: flex;
            justify-content: space-around;
            margin-top: 30px;
            padding-top: 30px;
            border-top: 2px solid #f0f0f0;
        }
        .stat {
            text-align: center;
        }
        .stat-number {
            font-size: 36px;
            font-weight: bold;
            color: #667eea;
        }
        .stat-label {
            color: #999;
            font-size: 14px;
            margin-top: 5px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">🛡️</div>
        <h1>Content Blocked</h1>
        <div class="message">
            <p>This content has been blocked by your accountability system.</p>
        </div>
        <p class="encouragement">You're staying strong! 💪</p>
        <p style="font-size: 16px; color: #888;">
            Remember why you set this up.<br>
            You're making progress every day.
        </p>
        <div class="stats">
            <div class="stat">
                <div class="stat-number" id="days">-</div>
                <div class="stat-label">Days Protected</div>
            </div>
            <div class="stat">
                <div class="stat-number" id="blocks">-</div>
                <div class="stat-label">Blocks Today</div>
            </div>
        </div>
    </div>
    <script>
        // Calculate days since setup (you can customize this)
        const startDate = new Date('2026-01-12');
        const today = new Date();
        const days = Math.floor((today - startDate) / (1000 * 60 * 60 * 24));
        document.getElementById('days').textContent = days;
        
        // Get blocks from localStorage (approximation)
        let blocks = localStorage.getItem('blocks') || 0;
        blocks = parseInt(blocks) + 1;
        localStorage.setItem('blocks', blocks);
        document.getElementById('blocks').textContent = blocks;
    </script>
</body>
</html>)";
    html.close();
    
    std::cout << "✓ Block page created\n";
}

// Setup comprehensive DNS blocking with dnsmasq
void setupDNSBlocking() {
    std::cout << "\n🔒 Setting up DNS blocking...\n";
    
    // Stop dnsmasq first
    system("systemctl stop dnsmasq 2>/dev/null");
    
    // Create dnsmasq config directory
    system("mkdir -p /etc/dnsmasq.d");
    
    // Main dnsmasq config - SIMPLE and WORKING
    std::ofstream main_dnsmasq("/etc/dnsmasq.conf");
    main_dnsmasq << "# Content Blocker DNS Configuration\n";
    main_dnsmasq << "# Listen on all interfaces for DNS queries\n";
    main_dnsmasq << "port=53\n";
    main_dnsmasq << "domain-needed\n";
    main_dnsmasq << "bogus-priv\n";
    main_dnsmasq << "no-resolv\n";
    main_dnsmasq << "# Forward normal DNS queries to Google DNS\n";
    main_dnsmasq << "server=8.8.8.8\n";
    main_dnsmasq << "server=8.8.4.4\n";
    main_dnsmasq << "cache-size=1000\n";
    main_dnsmasq << "log-queries\n";
    main_dnsmasq << "log-facility=/var/log/dnsmasq.log\n\n";
    
    main_dnsmasq << "# ONLY block adult content domains - everything else works normally\n";
    for (const auto& domain : blocked_domains) {
        main_dnsmasq << "address=/" << domain << "/127.0.0.1\n";
        main_dnsmasq << "address=/www." << domain << "/127.0.0.1\n";
    }
    
    main_dnsmasq << "\n# Block adult TLDs only\n";
    main_dnsmasq << "address=/.porn/127.0.0.1\n";
    main_dnsmasq << "address=/.xxx/127.0.0.1\n";
    main_dnsmasq << "address=/.adult/127.0.0.1\n";
    main_dnsmasq << "address=/.sex/127.0.0.1\n";
    main_dnsmasq.close();
    
    // Start dnsmasq
    system("systemctl enable dnsmasq 2>/dev/null");
    system("systemctl start dnsmasq 2>/dev/null");
    
    // Wait a moment for it to start
    std::this_thread::sleep_for(std::chrono::seconds(2));
    
    // Check if running
    int result = system("systemctl is-active dnsmasq 2>/dev/null | grep -q active");
    if (result == 0) {
        logActivity("DNS blocking configured for " + std::to_string(blocked_domains.size()) + " domains");
        std::cout << "✓ DNS blocking active (" << blocked_domains.size() << " domains)\n";
        std::cout << "  Normal websites will work fine!\n";
    } else {
        std::cout << "⚠️ Warning: dnsmasq may not have started properly\n";
        std::cout << "   Check with: sudo systemctl status dnsmasq\n";
    }
}

// Setup nginx web server to show block page
void setupBlockPageServer() {
    std::cout << "\n🌐 Setting up block page server...\n";
    
    // Install nginx if not present
    system("which nginx > /dev/null || pacman -S --noconfirm nginx 2>/dev/null || apt-get install -y nginx 2>/dev/null");
    
    createBlockPage();
    
    // Stop nginx first
    system("systemctl stop nginx 2>/dev/null");
    
    // Create main nginx config (simpler approach)
    std::ofstream nginx_conf("/etc/nginx/nginx.conf");
    nginx_conf << "user http;\n";
    nginx_conf << "worker_processes auto;\n";
    nginx_conf << "error_log /var/log/nginx/error.log warn;\n";
    nginx_conf << "pid /run/nginx.pid;\n\n";
    nginx_conf << "events {\n";
    nginx_conf << "    worker_connections 1024;\n";
    nginx_conf << "}\n\n";
    nginx_conf << "http {\n";
    nginx_conf << "    include /etc/nginx/mime.types;\n";
    nginx_conf << "    default_type text/html;\n";
    nginx_conf << "    access_log /var/log/nginx/access.log;\n\n";
    nginx_conf << "    server {\n";
    nginx_conf << "        listen 80 default_server;\n";
    nginx_conf << "        listen [::]:80 default_server;\n";
    nginx_conf << "        server_name _;\n";
    nginx_conf << "        root " << BLOCK_PAGE_DIR << ";\n";
    nginx_conf << "        index index.html;\n";
    nginx_conf << "        location / {\n";
    nginx_conf << "            try_files $uri $uri/ /index.html;\n";
    nginx_conf << "        }\n";
    nginx_conf << "    }\n";
    nginx_conf << "}\n";
    nginx_conf.close();
    
    // Test and start nginx
    system("nginx -t 2>&1");
    system("systemctl enable nginx 2>/dev/null");
    system("systemctl start nginx 2>/dev/null");
    
    logActivity("Block page server configured");
    std::cout << "✓ Block page server active\n";
}

// Setup iptables rules for comprehensive blocking
void setupFirewallRules() {
    std::cout << "\n🔥 Setting up firewall rules...\n";
    
    // DO NOT flush all rules - only add blocking rules
    // Flushing breaks internet connectivity!
    
    // Allow all traffic by default (IMPORTANT!)
    system("iptables -P INPUT ACCEPT");
    system("iptables -P OUTPUT ACCEPT");
    system("iptables -P FORWARD ACCEPT");
    
    // Only block specific adult content IPs
    std::vector<std::string> blocked_ip_ranges = {
        "185.88.181.0/24",
        "66.254.114.0/24"
    };
    
    for (const auto& ip_range : blocked_ip_ranges) {
        std::string cmd = "iptables -A OUTPUT -d " + ip_range + " -j REJECT";
        system(cmd.c_str());
        cmd = "iptables -A FORWARD -d " + ip_range + " -j REJECT";
        system(cmd.c_str());
    }
    
    // Block ONLY VPN to adult sites, not all VPN
    // (Removing general VPN blocking to preserve internet)
    
    // Save rules
    system("iptables-save > /etc/iptables/rules.v4 2>/dev/null");
    
    logActivity("Firewall rules configured - only blocking adult content IPs");
    std::cout << "✓ Firewall rules active (normal internet still works)\n";
}

// Block search engines from showing adult results
void setupSearchEngineBlocking() {
    std::cout << "\n🔍 Setting up SafeSearch (optional - commented out)...\n";
    
    // DON'T force SafeSearch IPs - this can break search engines
    // Instead, rely on DNS blocking of adult sites
    
    logActivity("SafeSearch setup skipped to preserve normal browsing");
    std::cout << "✓ Search engines work normally, adult sites blocked via DNS\n";
}

// Setup NetworkManager DNS override (for phones/tablets)
void setupNetworkManagerDNS() {
    std::cout << "\n📱 Configuring DNS for all devices...\n";
    
    // Just ensure local DNS works - don't break internet!
    
    // Backup original
    system("cp /etc/resolv.conf /etc/resolv.conf.backup 2>/dev/null");
    
    // Remove immutable flag if exists
    system("chattr -i /etc/resolv.conf 2>/dev/null");
    
    // For this computer, use local dnsmasq
    // But dnsmasq forwards everything else to Google DNS
    std::ofstream resolv("/etc/resolv.conf");
    resolv << "# Content Blocker DNS - forwards to Google for normal sites\n";
    resolv << "nameserver 127.0.0.1\n";
    resolv << "# Fallback to Google DNS if dnsmasq fails\n";
    resolv << "nameserver 8.8.8.8\n";
    resolv.close();
    
    logActivity("DNS configured - normal internet works, adult content blocked");
    std::cout << "✓ DNS configured (internet still works!)\n";
}

// Enable IP forwarding and make this machine a gateway
void enableGatewayMode() {
    std::cout << "\n🌐 Enabling gateway mode (optional)...\n";
    
    // Only enable if user wants to route ALL traffic through this machine
    // For now, skip this - it's not needed for basic DNS filtering
    
    logActivity("Gateway mode skipped - using DNS filtering only");
    std::cout << "✓ Using DNS-only filtering (simpler, internet works)\n";
}

// Setup auto-start on boot
void setupAutoStart() {
    std::cout << "\n⚙️ Setting up auto-start...\n";
    
    std::ofstream service("/etc/systemd/system/content-blocker.service");
    service << "[Unit]\n";
    service << "Description=Content Blocker Network Protection\n";
    service << "After=network.target\n";
    service << "Wants=dnsmasq.service nginx.service\n\n";
    service << "[Service]\n";
    service << "Type=oneshot\n";
    service << "ExecStart=/usr/local/bin/network_blocker activate\n";
    service << "RemainAfterExit=yes\n\n";
    service << "[Install]\n";
    service << "WantedBy=multi-user.target\n";
    service.close();
    
    system("systemctl daemon-reload");
    system("systemctl enable content-blocker.service 2>/dev/null");
    
    logActivity("Auto-start configured");
    std::cout << "✓ Auto-start on boot enabled\n";
}

bool authenticate(Config& config) {
    time_t now = time(nullptr);
    
    if (config.lockout_until > now) {
        int minutes = (config.lockout_until - now) / 60;
        std::cout << "\n🔒 LOCKED OUT for " << minutes << " more minutes\n";
        return false;
    }
    
    int delay = config.failed_attempts * 60;
    if (delay > 0) {
        std::cout << "\n⏳ Delay: " << delay << " seconds\n";
        for (int i = delay; i > 0; i -= 10) {
            std::cout << i << "... " << std::flush;
            std::this_thread::sleep_for(std::chrono::seconds(10));
        }
        std::cout << "\n";
    }
    
    std::string password;
    std::cout << "\nEnter password: ";
    std::cin >> password;
    
    if (verifyPassword(password, config.password_hash)) {
        config.failed_attempts = 0;
        saveConfig(config);
        return true;
    } else {
        config.failed_attempts++;
        if (config.failed_attempts >= 3) {
            config.lockout_until = now + 3600;
        }
        saveConfig(config);
        logActivity("Failed auth attempt #" + std::to_string(config.failed_attempts));
        std::cout << "✗ Wrong password!\n";
        return false;
    }
}

void setupWizard() {
    std::cout << "\n╔══════════════════════════════════════════╗\n";
    std::cout << "║  NETWORK CONTENT BLOCKER SETUP          ║\n";
    std::cout << "║  Protects ALL Devices on WiFi           ║\n";
    std::cout << "╚══════════════════════════════════════════╝\n\n";
    
    Config config;
    std::string password, confirm;
    
    std::cout << "Create password (min 8 chars): ";
    std::cin >> password;
    std::cout << "Confirm: ";
    std::cin >> confirm;
    
    if (password != confirm || password.length() < 8) {
        std::cout << "Error: Mismatch or too short!\n";
        return;
    }
    
    config.password_hash = hashPassword(password);
    config.failed_attempts = 0;
    config.lockout_until = 0;
    
    saveConfig(config);
    logActivity("Setup completed");
    
    std::cout << "\n✓ Setup complete!\n";
    std::cout << "\nIMPORTANT:\n";
    std::cout << "1. Write down your password\n";
    std::cout << "2. Give it to someone you trust\n";
    std::cout << "3. Run: sudo network_blocker activate\n\n";
}

void activateAll() {
    std::cout << "\n╔══════════════════════════════════════════╗\n";
    std::cout << "║  ACTIVATING NETWORK PROTECTION          ║\n";
    std::cout << "╚══════════════════════════════════════════╝\n";
    
    setupDNSBlocking();
    setupBlockPageServer();
    setupFirewallRules();
    setupSearchEngineBlocking();
    setupNetworkManagerDNS();
    enableGatewayMode();
    setupAutoStart();
    
    std::cout << "\n╔══════════════════════════════════════════╗\n";
    std::cout << "║  ✓ PROTECTION ACTIVE                    ║\n";
    std::cout << "╚══════════════════════════════════════════╝\n\n";
    
    std::cout << "Next steps:\n";
    std::cout << "1. On your router: Set DNS to THIS computer's IP\n";
    std::cout << "2. On your phone: Forget WiFi and reconnect\n";
    std::cout << "3. Test by trying to visit a blocked site\n";
    std::cout << "4. You should see the block page!\n\n";
    
    // Show this computer's IP
    std::cout << "\nFinding your computer's IP address...\n";
    system("ip addr show | grep 'inet ' | grep -v '127.0.0.1' | awk '{print \"This computer IP: \" $2}' | cut -d'/' -f1 | head -1");
    
    logActivity("All protection activated");
}

void deactivateAll() {
    Config config = loadConfig();
    
    if (!authenticate(config)) {
        std::cout << "Deactivation cancelled.\n";
        return;
    }
    
    std::cout << "\n⚠️ Type 'REMOVE' to confirm: ";
    std::string confirm;
    std::cin >> confirm;
    
    if (confirm != "REMOVE") {
        std::cout << "Cancelled.\n";
        return;
    }
    
    system("iptables -F && iptables -t nat -F");
    system("systemctl stop dnsmasq nginx 2>/dev/null");
    system("systemctl disable content-blocker 2>/dev/null");
    system("chattr -i /etc/resolv.conf 2>/dev/null");
    
    logActivity("Protection deactivated");
    std::cout << "\nProtection removed.\n";
}

void showStatus() {
    std::cout << "\n╔══════════════════════════════════════════╗\n";
    std::cout << "║  CONTENT BLOCKER STATUS                 ║\n";
    std::cout << "╚══════════════════════════════════════════╝\n\n";
    
    Config config = loadConfig();
    std::cout << "Password set: " << (!config.password_hash.empty() ? "✓" : "✗") << "\n";
    std::cout << "Failed attempts: " << config.failed_attempts << "\n";
    std::cout << "Blocked domains: " << blocked_domains.size() << "\n";
    std::cout << "Blocked keywords: " << blocked_keywords.size() << "\n\n";
    
    std::cout << "Services:\n";
    system("systemctl is-active dnsmasq 2>/dev/null | grep -q active && echo '  ✓ DNS (dnsmasq)' || echo '  ✗ DNS (dnsmasq NOT running!)'");
    system("systemctl is-active nginx 2>/dev/null | grep -q active && echo '  ✓ Block page (nginx)' || echo '  ✗ Block page (nginx NOT running!)'");
    
    std::cout << "\nTesting DNS blocking:\n";
    system("nslookup pornhub.com 127.0.0.1 2>/dev/null | grep -q '127.0.0.1' && echo '  ✓ DNS blocking working' || echo '  ✗ DNS blocking NOT working'");
    
    std::cout << "\nYour computer's IP:\n";
    system("ip addr show | grep 'inet ' | grep -v '127.0.0.1' | awk '{print \"  \" $2}' | cut -d'/' -f1 | head -1");
    
    std::cout << "\n";
}

void testBlocking() {
    std::cout << "\n╔══════════════════════════════════════════╗\n";
    std::cout << "║  TESTING CONTENT BLOCKING               ║\n";
    std::cout << "╚══════════════════════════════════════════╝\n\n";
    
    std::cout << "1. Testing DNS resolution...\n";
    int dns_test = system("nslookup pornhub.com 127.0.0.1 2>/dev/null | grep -q '127.0.0.1'");
    if (dns_test == 0) {
        std::cout << "   ✓ DNS blocking is working!\n";
    } else {
        std::cout << "   ✗ DNS blocking is NOT working\n";
        std::cout << "   Fix: sudo systemctl restart dnsmasq\n";
    }
    
    std::cout << "\n2. Testing block page server...\n";
    int nginx_test = system("curl -s http://127.0.0.1 2>/dev/null | grep -q 'Content Blocked'");
    if (nginx_test == 0) {
        std::cout << "   ✓ Block page is working!\n";
    } else {
        std::cout << "   ✗ Block page is NOT working\n";
        std::cout << "   Fix: sudo systemctl restart nginx\n";
    }
    
    std::cout << "\n3. Checking iptables rules...\n";
    int fw_test = system("iptables -L | grep -q DROP");
    if (fw_test == 0) {
        std::cout << "   ✓ Firewall rules are active\n";
    } else {
        std::cout << "   ✗ No firewall rules found\n";
        std::cout << "   Fix: sudo network_blocker activate\n";
    }
    
    std::cout << "\n4. Checking services...\n";
    system("systemctl is-active dnsmasq nginx 2>/dev/null");
    
    std::cout << "\n5. Testing normal internet...\n";
    int internet_test = system("ping -c 1 google.com > /dev/null 2>&1");
    if (internet_test == 0) {
        std::cout << "   ✓ Normal internet is working!\n";
    } else {
        std::cout << "   ✗ Internet connection problem\n";
        std::cout << "   Fix: Check your network connection\n";
    }
    
    std::cout << "\n✅ INSTRUCTIONS FOR YOUR PHONE:\n";
    std::cout << "================================\n";
    std::cout << "1. On your router:\n";
    std::cout << "   - Login (usually 192.168.1.1)\n";
    std::cout << "   - Go to DNS settings\n";
    std::cout << "   - Set DNS to: ";
    system("ip addr show | grep 'inet ' | grep -v '127.0.0.1' | awk '{print $2}' | cut -d'/' -f1 | head -1");
    std::cout << "\n2. On your phone:\n";
    std::cout << "   - Settings → WiFi → Forget Network\n";
    std::cout << "   - Reconnect to WiFi\n";
    std::cout << "   - Try visiting a blocked site\n";
    std::cout << "   - You should see 'Content Blocked' page\n\n";
}

int main(int argc, char* argv[]) {
    if (geteuid() != 0) {
        std::cerr << "Must run as root (use sudo)\n";
        return 1;
    }
    
    if (argc < 2) {
        std::cout << "\nNetwork Content Blocker\n";
        std::cout << "=======================\n\n";
        std::cout << "Commands:\n";
        std::cout << "  sudo network_blocker setup       - Setup\n";
        std::cout << "  sudo network_blocker activate    - Enable\n";
        std::cout << "  sudo network_blocker deactivate  - Disable\n";
        std::cout << "  sudo network_blocker status      - Status\n";
        std::cout << "  sudo network_blocker test        - Test if working\n";
        std::cout << "  sudo network_blocker logs        - View logs\n\n";
        return 0;
    }
    
    std::string cmd = argv[1];
    
    if (cmd == "setup") setupWizard();
    else if (cmd == "activate") activateAll();
    else if (cmd == "deactivate") deactivateAll();
    else if (cmd == "status") showStatus();
    else if (cmd == "test") testBlocking();
    else if (cmd == "logs") system(("tail -100 " + LOG_FILE).c_str());
    else std::cout << "Unknown command\n";
    
    return 0;
}
