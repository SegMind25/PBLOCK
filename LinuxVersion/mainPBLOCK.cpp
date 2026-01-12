#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <unistd.h>
#include <crypt.h>
#include <cstring>
#include <thread>
#include <chrono>
#include <sys/stat.h>

const std::string HOSTS_FILE = "/etc/hosts";
const std::string CONFIG_FILE = "/etc/content_blocker.conf";

// List of domains to block
std::vector<std::string> blocked_domains = {
    "pornhub.com",
    "xvideos.com",
    "xnxx.com",
    "xhamster.com",
    "redtube.com",
    "youporn.com",
    "tube8.com",
    "spankbang.com",
    "eporner.com",
    "txxx.com"
    // Add more domains as needed
};

std::string hashPassword(const std::string& password) {
    return crypt(password.c_str(), "$6$randomsalt");
}

bool verifyPassword(const std::string& password, const std::string& hash) {
    std::string test = crypt(password.c_str(), hash.c_str());
    return test == hash;
}

void savePasswordHash(const std::string& hash) {
    std::ofstream config(CONFIG_FILE);
    if (config.is_open()) {
        config << hash << std::endl;
        config.close();
        chmod(CONFIG_FILE.c_str(), 0600);
    }
}

std::string loadPasswordHash() {
    std::ifstream config(CONFIG_FILE);
    std::string hash;
    if (config.is_open()) {
        std::getline(config, hash);
        config.close();
    }
    return hash;
}

bool isBlocked() {
    std::ifstream hosts(HOSTS_FILE);
    std::string line;
    while (std::getline(hosts, line)) {
        if (line.find("# CONTENT_BLOCKER_START") != std::string::npos) {
            return true;
        }
    }
    return false;
}

void blockContent() {
    if (isBlocked()) {
        std::cout << "Content blocking is already active.\n";
        return;
    }

    std::ofstream hosts(HOSTS_FILE, std::ios::app);
    if (!hosts.is_open()) {
        std::cerr << "Error: Cannot open hosts file. Run with sudo.\n";
        return;
    }

    hosts << "\n# CONTENT_BLOCKER_START - Do not edit this section\n";
    for (const auto& domain : blocked_domains) {
        hosts << "127.0.0.1 " << domain << "\n";
        hosts << "127.0.0.1 www." << domain << "\n";
        hosts << "::1 " << domain << "\n";
        hosts << "::1 www." << domain << "\n";
    }
    hosts << "# CONTENT_BLOCKER_END\n";
    hosts.close();

    std::cout << "✓ Content blocking activated.\n";
    std::cout << blocked_domains.size() << " domains blocked.\n";
}

void unblockContent() {
    std::ifstream hosts_in(HOSTS_FILE);
    std::vector<std::string> lines;
    std::string line;
    bool skip = false;

    while (std::getline(hosts_in, line)) {
        if (line.find("# CONTENT_BLOCKER_START") != std::string::npos) {
            skip = true;
            continue;
        }
        if (line.find("# CONTENT_BLOCKER_END") != std::string::npos) {
            skip = false;
            continue;
        }
        if (!skip) {
            lines.push_back(line);
        }
    }
    hosts_in.close();

    std::ofstream hosts_out(HOSTS_FILE);
    for (const auto& l : lines) {
        hosts_out << l << "\n";
    }
    hosts_out.close();

    std::cout << "✓ Content blocking removed.\n";
}

void setupPassword() {
    std::string password, confirm;
    
    std::cout << "\n=== SET UP YOUR ACCOUNTABILITY PASSWORD ===\n";
    std::cout << "This password will be required to disable blocking.\n";
    std::cout << "Make it strong and WRITE IT DOWN somewhere safe.\n\n";
    
    std::cout << "Enter password: ";
    std::cin >> password;
    std::cout << "Confirm password: ";
    std::cin >> confirm;

    if (password != confirm) {
        std::cout << "Passwords don't match!\n";
        return;
    }

    if (password.length() < 8) {
        std::cout << "Password too short! Use at least 8 characters.\n";
        return;
    }

    std::string hash = hashPassword(password);
    savePasswordHash(hash);
    std::cout << "\n✓ Password set successfully.\n";
}

bool authenticateWithDelay() {
    std::string stored_hash = loadPasswordHash();
    if (stored_hash.empty()) {
        std::cout << "No password set. Please set password first.\n";
        return false;
    }

    std::cout << "\n⏳ Intentional 30-second delay...\n";
    std::cout << "Use this time to reconsider your decision.\n";
    
    for (int i = 30; i > 0; i--) {
        std::cout << i << "... " << std::flush;
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    std::cout << "\n\n";

    std::string password;
    std::cout << "Enter password to disable blocking: ";
    std::cin >> password;

    if (verifyPassword(password, stored_hash)) {
        std::cout << "✓ Authentication successful.\n";
        return true;
    } else {
        std::cout << "✗ Wrong password!\n";
        return false;
    }
}

void showStatus() {
    std::cout << "\n=== CONTENT BLOCKER STATUS ===\n";
    std::cout << "Status: " << (isBlocked() ? "ACTIVE ✓" : "INACTIVE") << "\n";
    std::cout << "Blocked domains: " << blocked_domains.size() << "\n";
    std::cout << "Password set: " << (!loadPasswordHash().empty() ? "Yes" : "No") << "\n";
    std::cout << "============================\n\n";
}

int main(int argc, char* argv[]) {
    if (geteuid() != 0) {
        std::cerr << "This program must be run as root (use sudo).\n";
        return 1;
    }

    if (argc < 2) {
        std::cout << "\nContent Blocker - Self-Accountability Tool\n";
        std::cout << "=========================================\n\n";
        std::cout << "Usage:\n";
        std::cout << "  sudo ./blocker setup    - Set password\n";
        std::cout << "  sudo ./blocker block    - Enable blocking\n";
        std::cout << "  sudo ./blocker unblock  - Disable blocking (requires password)\n";
        std::cout << "  sudo ./blocker status   - Show current status\n\n";
        return 0;
    }

    std::string command = argv[1];

    if (command == "setup") {
        setupPassword();
    } else if (command == "block") {
        blockContent();
    } else if (command == "unblock") {
        if (authenticateWithDelay()) {
            unblockContent();
        }
    } else if (command == "status") {
        showStatus();
    } else {
        std::cout << "Unknown command: " << command << "\n";
    }

    return 0;
}
