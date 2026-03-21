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

// Comprehensive list of adult domains to block (redirected to 127.0.0.1)
std::vector<std::string> blocked_domains = {
    // Major adult sites
    "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
    "redtube.com", "youporn.com", "tube8.com", "spankbang.com",
    "eporner.com", "txxx.com", "beeg.com", "fuq.com",
    "hqporner.com", "4tube.com", "drtuber.com", "tnaflix.com",
    "nuvid.com", "porn.com", "ixxx.com", "porntrex.com",
    "thumbzilla.com", "motherless.com", "slutload.com",
    "youjizz.com", "porntube.com", "pornone.com", "fapvid.com",
    "porndig.com", "3movs.com", "fux.com", "anyporn.com",
    "vporn.com", "sunporno.com", "pornoxo.com", "watchmygf.me",
    "empflix.com", "porndoe.com", "pornhat.com", "pornpics.com",
    "pornpics.de", "hentaihaven.xxx", "hanime.tv", "nhentai.net",
    "rule34.xxx", "e-hentai.org", "hentai2read.com",
    // Cam/live sites
    "chaturbate.com", "bongacams.com", "stripchat.com",
    "cam4.com", "myfreecams.com", "livejasmin.com",
    "camsoda.com", "flirt4free.com", "streamate.com",
    // OnlyFans and similar
    "onlyfans.com", "fansly.com", "manyvids.com",
    // Image boards / forums
    "imagefap.com", "sex.com", "literotica.com",
    "eroshare.com", "reddit.com/r/nsfw", "reddit.com/r/gonewild",
    // Aggregators / tubes
    "alohatube.com", "xxxbunker.com", "pornmd.com",
    "nudevista.com", "tubegalore.com", "porn300.com",
    "lobstertube.com", "bellesa.co", "daftsex.com",
    "sxyprn.com", "ok.xxx", "megatube.xxx",
    "pornzog.com", "upornia.com", "hdporncomics.com",
    "vqporn.com", "vrporn.com", "sexlikereal.com",
    "pornkai.com", "fapcat.com", "ashemaletube.com",
    "shemale.com", "trannytube.tv", "zbporn.com",
    "bravotube.net", "gotporn.com", "hellporno.com",
    "yespornplease.to", "porngo.com", "cliphunter.com",
    "eskimotube.com", "fantasti.cc", "freeones.com",
    "heavy-r.com", "hclips.com", "hdporn.net",
    "hotmovs.com", "hotscope.tv", "jizzbunker.com",
    "justporno.tv", "katestube.com", "keezmovies.com",
    "largepornfilms.com", "megatube.xxx", "movieshark.com",
    "mrstiff.com", "mypornbible.com", "netfapx.com",
    "palimas.com", "pichunter.com", "pinkrod.com",
    "porndish.com", "pornhd.com", "pornlib.com",
    "pornoeggs.com", "pornolab.net", "pornoreino.com",
    "pornrabbit.com", "porntop.com", "pornyeah.com",
    "proporn.com", "ro89.com", "rexporn.com",
    "sexu.com", "shameless.com", "silverdaddies.com",
    "sleazyneasy.com", "tastyblacks.com",
    "theporndude.com", "tiava.com", "tjoob.com",
    "tobyporn.com", "torrentday.com", "trannytube.tv",
    "tubedupe.com", "tubekitty.com", "tubesafari.com",
    "tubewolf.com", "vintagetube.xxx", "voyeurhit.com",
    "wetplace.com", "worldsex.com", "xbabe.com",
    "xcafe.com", "xmoviesforyou.com", "xxxdan.com",
    "xxxtentacion.org", "yourlust.com", "yuvutu.com",
    "zedporn.com"
};

// Search engine SafeSearch enforcement
// Google SafeSearch VIP IP - forces SafeSearch on all Google searches
const std::string GOOGLE_SAFESEARCH_IP = "216.239.38.120";
// Bing strict mode IP - forces strict SafeSearch on Bing
const std::string BING_STRICT_IP = "204.79.197.220";

// Google domains to redirect to SafeSearch VIP
std::vector<std::string> google_domains = {
    "google.com", "www.google.com",
    "google.co.uk", "www.google.co.uk",
    "google.fr", "www.google.fr",
    "google.de", "www.google.de",
    "google.es", "www.google.es",
    "google.it", "www.google.it",
    "google.ca", "www.google.ca",
    "google.com.au", "www.google.com.au",
    "google.co.in", "www.google.co.in",
    "google.com.br", "www.google.com.br",
    "google.co.jp", "www.google.co.jp",
    "google.ru", "www.google.ru",
    "google.com.mx", "www.google.com.mx",
    "google.nl", "www.google.nl",
    "google.be", "www.google.be",
    "google.pt", "www.google.pt",
    "google.co.za", "www.google.co.za",
    "google.com.ar", "www.google.com.ar",
    "google.com.eg", "www.google.com.eg",
    "google.com.sa", "www.google.com.sa",
    "google.ae", "www.google.ae",
    "google.co.ma", "www.google.co.ma",
    "google.dz", "www.google.dz",
    "google.com.tr", "www.google.com.tr",
    "google.pl", "www.google.pl",
    "google.se", "www.google.se",
    "google.no", "www.google.no",
    "google.dk", "www.google.dk",
    "google.fi", "www.google.fi",
    "google.at", "www.google.at",
    "google.ch", "www.google.ch"
};

// Bing domains to redirect to strict mode
std::vector<std::string> bing_domains = {
    "bing.com", "www.bing.com",
    "bing.co.uk", "www.bing.co.uk",
    "bing.de", "www.bing.de",
    "bing.fr", "www.bing.fr"
};

// Search engines that don't support forced SafeSearch via DNS - block entirely
std::vector<std::string> blocked_search_engines = {
    "duckduckgo.com", "www.duckduckgo.com",
    "yandex.com", "www.yandex.com", "yandex.ru", "www.yandex.ru",
    "ask.com", "www.ask.com",
    "baidu.com", "www.baidu.com",
    "ecosia.org", "www.ecosia.org",
    "qwant.com", "www.qwant.com",
    "startpage.com", "www.startpage.com",
    "swisscows.com", "www.swisscows.com",
    "gibiru.com", "www.gibiru.com",
    "searx.me", "www.searx.me"
};

std::string generateSalt() {
    const char charset[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./";
    std::string salt = "$6$";
    std::ifstream urandom("/dev/urandom", std::ios::binary);
    if (urandom.is_open()) {
        for (int i = 0; i < 16; i++) {
            unsigned char c;
            urandom.read(reinterpret_cast<char*>(&c), 1);
            salt += charset[c % (sizeof(charset) - 1)];
        }
        urandom.close();
    }
    salt += "$";
    return salt;
}

std::string hashPassword(const std::string& password) {
    std::string salt = generateSalt();
    return crypt(password.c_str(), salt.c_str());
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

    // Block adult domains (redirect to 127.0.0.1)
    for (const auto& domain : blocked_domains) {
        hosts << "127.0.0.1 " << domain << "\n";
        hosts << "127.0.0.1 www." << domain << "\n";
        hosts << "::1 " << domain << "\n";
        hosts << "::1 www." << domain << "\n";
    }

    // Force Google SafeSearch (redirect to SafeSearch VIP)
    hosts << "# Google SafeSearch enforcement\n";
    for (const auto& domain : google_domains) {
        hosts << GOOGLE_SAFESEARCH_IP << " " << domain << "\n";
    }

    // Force Bing strict SafeSearch
    hosts << "# Bing strict SafeSearch enforcement\n";
    for (const auto& domain : bing_domains) {
        hosts << BING_STRICT_IP << " " << domain << "\n";
    }

    // Block search engines without SafeSearch support
    hosts << "# Block other search engines (no SafeSearch support)\n";
    for (const auto& domain : blocked_search_engines) {
        hosts << "127.0.0.1 " << domain << "\n";
        hosts << "::1 " << domain << "\n";
    }

    hosts << "# CONTENT_BLOCKER_END\n";
    hosts.close();

    // Flush DNS cache
    system("systemd-resolve --flush-caches 2>/dev/null || nscd -i hosts 2>/dev/null || true");

    int total = blocked_domains.size() + google_domains.size() + bing_domains.size() + blocked_search_engines.size();
    std::cout << "✓ Content blocking activated.\n";
    std::cout << blocked_domains.size() << " adult domains blocked.\n";
    std::cout << "Google SafeSearch forced on " << google_domains.size() << " Google domains.\n";
    std::cout << "Bing strict mode forced on " << bing_domains.size() << " Bing domains.\n";
    std::cout << blocked_search_engines.size() << " other search engines blocked.\n";
    std::cout << "Total: " << total << " entries.\n";
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
    int total = blocked_domains.size() + google_domains.size() + bing_domains.size() + blocked_search_engines.size();
    std::cout << "\n=== CONTENT BLOCKER STATUS ===\n";
    std::cout << "Status: " << (isBlocked() ? "ACTIVE ✓" : "INACTIVE") << "\n";
    std::cout << "Adult domains blocked: " << blocked_domains.size() << "\n";
    std::cout << "Google SafeSearch domains: " << google_domains.size() << "\n";
    std::cout << "Bing strict domains: " << bing_domains.size() << "\n";
    std::cout << "Other search engines blocked: " << blocked_search_engines.size() << "\n";
    std::cout << "Total entries: " << total << "\n";
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
