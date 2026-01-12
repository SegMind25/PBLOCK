#include <windows.h>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
#include <shlobj.h>

const std::string HOSTS_FILE = "C:\\Windows\\System32\\drivers\\etc\\hosts";
const std::string CONFIG_DIR = std::string(getenv("APPDATA")) + "\\ContentBlocker";
const std::string CONFIG_FILE = CONFIG_DIR + "\\config.dat";

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
    // Add more as needed
};

// Simple XOR encryption for password storage
std::string xorEncrypt(const std::string& data, const std::string& key) {
    std::string result = data;
    for (size_t i = 0; i < data.length(); i++) {
        result[i] = data[i] ^ key[i % key.length()];
    }
    return result;
}

bool isAdmin() {
    BOOL isAdmin = FALSE;
    PSID adminGroup = NULL;
    SID_IDENTIFIER_AUTHORITY ntAuthority = SECURITY_NT_AUTHORITY;
    
    if (AllocateAndInitializeSid(&ntAuthority, 2, SECURITY_BUILTIN_DOMAIN_RID,
        DOMAIN_ALIAS_RID_ADMINS, 0, 0, 0, 0, 0, 0, &adminGroup)) {
        CheckTokenMembership(NULL, adminGroup, &isAdmin);
        FreeSid(adminGroup);
    }
    return isAdmin;
}

void ensureConfigDir() {
    CreateDirectoryA(CONFIG_DIR.c_str(), NULL);
}

void savePasswordHash(const std::string& password) {
    ensureConfigDir();
    std::string encrypted = xorEncrypt(password, "ContentBlockerKey2024");
    
    std::ofstream config(CONFIG_FILE, std::ios::binary);
    if (config.is_open()) {
        config << encrypted;
        config.close();
        SetFileAttributesA(CONFIG_FILE.c_str(), FILE_ATTRIBUTE_HIDDEN | FILE_ATTRIBUTE_SYSTEM);
    }
}

std::string loadPasswordHash() {
    std::ifstream config(CONFIG_FILE, std::ios::binary);
    std::string encrypted, decrypted;
    
    if (config.is_open()) {
        std::getline(config, encrypted);
        decrypted = xorEncrypt(encrypted, "ContentBlockerKey2024");
        config.close();
    }
    return decrypted;
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
        std::cerr << "Error: Cannot open hosts file. Run as Administrator.\n";
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

    // Flush DNS cache
    system("ipconfig /flushdns > nul");

    std::cout << "\n=================================\n";
    std::cout << "  CONTENT BLOCKING ACTIVATED\n";
    std::cout << "=================================\n";
    std::cout << blocked_domains.size() << " domains now blocked.\n";
    std::cout << "DNS cache flushed.\n\n";
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

    system("ipconfig /flushdns > nul");
    std::cout << "\nContent blocking removed.\n";
}

void setupPassword() {
    std::string password, confirm;
    
    std::cout << "\n=========================================\n";
    std::cout << "  SET UP ACCOUNTABILITY PASSWORD\n";
    std::cout << "=========================================\n\n";
    std::cout << "This password will be required to disable blocking.\n";
    std::cout << "IMPORTANT: Make it strong and write it down!\n";
    std::cout << "Store it with someone you trust if possible.\n\n";
    
    std::cout << "Enter password: ";
    std::cin >> password;
    std::cout << "Confirm password: ";
    std::cin >> confirm;

    if (password != confirm) {
        std::cout << "\nError: Passwords don't match!\n";
        return;
    }

    if (password.length() < 8) {
        std::cout << "\nError: Password too short! Use at least 8 characters.\n";
        return;
    }

    savePasswordHash(password);
    std::cout << "\n✓ Password set successfully!\n";
    std::cout << "WRITE IT DOWN NOW and keep it safe.\n\n";
}

bool authenticateWithDelay() {
    std::string stored_password = loadPasswordHash();
    if (stored_password.empty()) {
        std::cout << "No password set. Run 'blocker.exe setup' first.\n";
        return false;
    }

    std::cout << "\n=========================================\n";
    std::cout << "  INTENTIONAL 30-SECOND DELAY\n";
    std::cout << "=========================================\n";
    std::cout << "Use this time to reconsider.\n";
    std::cout << "Is this really what you want to do?\n\n";
    
    for (int i = 30; i > 0; i--) {
        std::cout << "  " << i << " seconds remaining...\r" << std::flush;
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    std::cout << "\n\n";

    std::string password;
    std::cout << "Enter password to disable blocking: ";
    std::cin >> password;

    if (password == stored_password) {
        std::cout << "\n✓ Authentication successful.\n";
        return true;
    } else {
        std::cout << "\n✗ Wrong password!\n";
        std::this_thread::sleep_for(std::chrono::seconds(3));
        return false;
    }
}

void showStatus() {
    std::cout << "\n=========================================\n";
    std::cout << "    CONTENT BLOCKER STATUS\n";
    std::cout << "=========================================\n";
    std::cout << "Status: " << (isBlocked() ? "ACTIVE ✓" : "INACTIVE ✗") << "\n";
    std::cout << "Blocked domains: " << blocked_domains.size() << "\n";
    std::cout << "Password set: " << (!loadPasswordHash().empty() ? "Yes ✓" : "No ✗") << "\n";
    std::cout << "=========================================\n\n";
}

void addToStartup() {
    char exePath[MAX_PATH];
    GetModuleFileNameA(NULL, exePath, MAX_PATH);
    
    HKEY hKey;
    const char* subKey = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    
    if (RegOpenKeyExA(HKEY_CURRENT_USER, subKey, 0, KEY_WRITE, &hKey) == ERROR_SUCCESS) {
        std::string command = std::string(exePath) + " block";
        RegSetValueExA(hKey, "ContentBlocker", 0, REG_SZ, 
                      (BYTE*)command.c_str(), command.length() + 1);
        RegCloseKey(hKey);
        std::cout << "✓ Added to Windows startup.\n";
    }
}

void removeFromStartup() {
    HKEY hKey;
    const char* subKey = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    
    if (RegOpenKeyExA(HKEY_CURRENT_USER, subKey, 0, KEY_WRITE, &hKey) == ERROR_SUCCESS) {
        RegDeleteValueA(hKey, "ContentBlocker");
        RegCloseKey(hKey);
        std::cout << "✓ Removed from Windows startup.\n";
    }
}

int main(int argc, char* argv[]) {
    if (!isAdmin()) {
        std::cerr << "\nERROR: This program requires Administrator privileges.\n";
        std::cerr << "Right-click and select 'Run as Administrator'\n\n";
        system("pause");
        return 1;
    }

    if (argc < 2) {
        std::cout << "\n=========================================\n";
        std::cout << "  CONTENT BLOCKER - Self-Accountability\n";
        std::cout << "=========================================\n\n";
        std::cout << "Commands:\n";
        std::cout << "  blocker.exe setup      - Set up password\n";
        std::cout << "  blocker.exe block      - Enable blocking\n";
        std::cout << "  blocker.exe unblock    - Disable (needs password)\n";
        std::cout << "  blocker.exe status     - Show current status\n";
        std::cout << "  blocker.exe startup    - Run on Windows startup\n";
        std::cout << "  blocker.exe nostartup  - Remove from startup\n\n";
        system("pause");
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
    } else if (command == "startup") {
        addToStartup();
    } else if (command == "nostartup") {
        removeFromStartup();
    } else {
        std::cout << "Unknown command: " << command << "\n";
    }

    if (argc < 3 || std::string(argv[argc-1]) != "silent") {
        std::cout << "\n";
        system("pause");
    }

    return 0;
}
