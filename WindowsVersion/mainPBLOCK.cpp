#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <windows.h>
#include <shlobj.h>
#include <thread>
#include <chrono>

const std::string HOSTS_FILE = "C:\\Windows\\System32\\drivers\\etc\\hosts";
const std::string CONFIG_FILE = "C:\\ProgramData\\content_blocker.conf";

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
};

// Simple hash function for Windows (use stronger hashing in production)
std::string hashPassword(const std::string& password) {
    HCRYPTPROV hProv = 0;
    HCRYPTHASH hHash = 0;
    BYTE hash[32];
    DWORD hashLen = 32;
    std::string result;

    if (CryptAcquireContext(&hProv, NULL, NULL, PROV_RSA_AES, CRYPT_VERIFYCONTEXT)) {
        if (CryptCreateHash(hProv, CALG_SHA_256, 0, 0, &hHash)) {
            if (CryptHashData(hHash, (BYTE*)password.c_str(), password.length(), 0)) {
                if (CryptGetHashParam(hHash, HP_HASHVAL, hash, &hashLen, 0)) {
                    char hex[3];
                    for (DWORD i = 0; i < hashLen; i++) {
                        sprintf_s(hex, "%02x", hash[i]);
                        result += hex;
                    }
                }
            }
            CryptDestroyHash(hHash);
        }
        CryptReleaseContext(hProv, 0);
    }
    return result;
}

void savePasswordHash(const std::string& hash) {
    std::ofstream config(CONFIG_FILE);
    if (config.is_open()) {
        config << hash << std::endl;
        config.close();
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

bool isAdmin() {
    BOOL isAdmin = FALSE;
    SID_IDENTIFIER_AUTHORITY NtAuthority = SECURITY_NT_AUTHORITY;
    PSID AdministratorsGroup;
    
    if (AllocateAndInitializeSid(&NtAuthority, 2, SECURITY_BUILTIN_DOMAIN_RID,
        DOMAIN_ALIAS_RID_ADMINS, 0, 0, 0, 0, 0, 0, &AdministratorsGroup)) {
        CheckTokenMembership(NULL, AdministratorsGroup, &isAdmin);
        FreeSid(AdministratorsGroup);
    }
    return isAdmin;
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

    system("ipconfig /flushdns > nul");
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

    std::string input_hash = hashPassword(password);
    if (input_hash == stored_hash) {
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
    if (!isAdmin()) {
        std::cerr << "This program must be run as Administrator.\n";
        std::cerr << "Right-click and select 'Run as Administrator'.\n";
        system("pause");
        return 1;
    }

    if (argc < 2) {
        std::cout << "\nContent Blocker - Self-Accountability Tool (Windows)\n";
        std::cout << "==================================================\n\n";
        std::cout << "Usage:\n";
        std::cout << "  blocker.exe setup    - Set password\n";
        std::cout << "  blocker.exe block    - Enable blocking\n";
        std::cout << "  blocker.exe unblock  - Disable blocking (requires password)\n";
        std::cout << "  blocker.exe status   - Show current status\n\n";
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
    } else {
        std::cout << "Unknown command: " << command << "\n";
    }

    system("pause");
    return 0;
}
