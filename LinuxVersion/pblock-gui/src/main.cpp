#include <gtk/gtk.h>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <crypt.h>
#include <cstring>
#include <thread>
#include <chrono>
#include <random>
#include <algorithm>
#include <sstream>
#include <functional>
#include <sys/stat.h>
#include <cstdlib>

static const std::string HOSTS_FILE = "/etc/hosts";
static const std::string CONFIG_FILE = "/etc/pblock.conf";
static const std::string BACKUP_HOSTS = "/etc/pblock_hosts_backup";

static std::vector<std::string> blocked_domains = {
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
    "chaturbate.com", "bongacams.com", "stripchat.com",
    "cam4.com", "myfreecams.com", "livejasmin.com",
    "camsoda.com", "flirt4free.com", "streamate.com",
    "onlyfans.com", "fansly.com", "manyvids.com",
    "imagefap.com", "sex.com", "literotica.com",
    "eroshare.com", "reddit.com/r/nsfw", "reddit.com/r/gonewild",
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
    "largepornfilms.com", "movieshark.com",
    "mrstiff.com", "mypornbible.com", "netfapx.com",
    "palimas.com", "pichunter.com", "pinkrod.com",
    "porndish.com", "pornhd.com", "pornlib.com",
    "pornoeggs.com", "pornolab.net", "pornoreino.com",
    "pornrabbit.com", "porntop.com", "pornyeah.com",
    "proporn.com", "ro89.com", "rexporn.com",
    "sexu.com", "shameless.com", "silverdaddies.com",
    "sleazyneasy.com", "tastyblacks.com",
    "theporndude.com", "tiava.com", "tjoob.com",
    "tobyporn.com", "torrentday.com",
    "tubedupe.com", "tubekitty.com", "tubesafari.com",
    "tubewolf.com", "vintagetube.xxx", "voyeurhit.com",
    "wetplace.com", "worldsex.com", "xbabe.com",
    "xcafe.com", "xmoviesforyou.com", "xxxdan.com",
    "xxxtentacion.org", "yourlust.com", "yuvutu.com",
    "zedporn.com"
};

static const std::string GOOGLE_SAFESEARCH_IP = "216.239.38.120";
static const std::string BING_STRICT_IP = "204.79.197.220";

static std::vector<std::string> google_domains = {
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

static std::vector<std::string> bing_domains = {
    "bing.com", "www.bing.com",
    "bing.co.uk", "www.bing.co.uk",
    "bing.de", "www.bing.de",
    "bing.fr", "www.bing.fr"
};

static std::vector<std::string> blocked_search_engines = {
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

struct Puzzle {
    std::string question;
    int answer;
    std::string hint;
};

static std::string generateSalt() {
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

static std::string hashPassword(const std::string& password) {
    std::string salt = generateSalt();
    return crypt(password.c_str(), salt.c_str());
}

static void savePasswordHash(const std::string& hash) {
    std::ofstream config(CONFIG_FILE);
    if (config.is_open()) {
        config << hash << std::endl;
        config.close();
        chmod(CONFIG_FILE.c_str(), 0600);
    }
}

static std::string loadPasswordHash() {
    std::ifstream config(CONFIG_FILE);
    std::string hash;
    if (config.is_open()) {
        std::getline(config, hash);
        config.close();
    }
    return hash;
}

static bool isBlocked() {
    std::ifstream hosts(HOSTS_FILE);
    std::string line;
    while (std::getline(hosts, line)) {
        if (line.find("# PBLOCK_START") != std::string::npos) return true;
    }
    return false;
}

static int gcd(int a, int b) { while (b) { int t = b; b = a % b; a = t; } return a; }

static int getFibonacciAnswer(int n) {
    std::vector<int> fib = {0, 1};
    for (int i = 2; i <= n + 2; i++) fib.push_back(fib[i-1] + fib[i-2]);
    return fib[n];
}

static int getPrimeCheckAnswer(int n) {
    if (n < 2) return 0;
    for (int i = 2; i * i <= n; i++) if (n % i == 0) return 0;
    return 1;
}

static Puzzle generatePuzzle(std::mt19937& rng) {
    Puzzle p;
    int type = rng() % 10;
    std::stringstream ss;
    switch (type) {
        case 0: {
            int n = 6 + rng() % 5;
            ss << "What is the " << n << "th Fibonacci number? (0, 1, 1, 2, 3, 5, ...)";
            p.question = ss.str();
            p.answer = getFibonacciAnswer(n);
            p.hint = "Each number is the sum of the two before it";
            break;
        }
        case 1: {
            int a = 12 + rng() % 88, b = 12 + rng() % 88;
            ss << "What is GCD(" << a << ", " << b << ")?";
            p.question = ss.str();
            p.answer = gcd(a, b);
            p.hint = "Use Euclidean algorithm";
            break;
        }
        case 2: {
            int primes[] = {101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,193,197,199};
            int nonPrimes[] = {100,102,104,105,106,108,110,111,112,114,115,116,117,118,119,120,121,122,123,124,125};
            int n = (rng()%2==0) ? primes[rng()%21] : nonPrimes[rng()%21];
            ss << "Is " << n << " prime? (1=yes, 0=no)";
            p.question = ss.str();
            p.answer = getPrimeCheckAnswer(n);
            p.hint = "Check divisibility up to sqrt(n)";
            break;
        }
        case 3: {
            int a = 10+rng()%90, b = 2+rng()%8, m = 3+rng()%7;
            ss << "What is (" << a << " * " << b << ") mod " << m << "?";
            p.question = ss.str();
            p.answer = (a*b)%m;
            p.hint = "Calculate product first, then mod";
            break;
        }
        case 4: {
            int start = rng()%10+1, step = rng()%5+2, missing = rng()%4+2;
            ss << "What comes next? ";
            for (int i = 0; i < 5; i++) {
                if (i == missing) ss << "? ";
                else ss << (start + step * i) << " ";
            }
            p.question = ss.str();
            p.answer = start + step * missing;
            p.hint = "Find the pattern (constant difference)";
            break;
        }
        case 5: {
            int n = 2+rng()%6, fact=1;
            for (int i=2;i<=n;i++) fact*=i;
            ss << "What is " << n << "! (factorial)?";
            p.question = ss.str();
            p.answer = fact;
            p.hint = "n! = n * (n-1) * ... * 2 * 1";
            break;
        }
        case 6: {
            int base = 2+rng()%4, exp = 2+rng()%4, result=1;
            for (int i=0;i<exp;i++) result*=base;
            ss << "What is " << base << "^" << exp << "?";
            p.question = ss.str();
            p.answer = result;
            p.hint = "Exponentiation";
            break;
        }
        case 7: {
            int a = 10+rng()%40, b = 10+rng()%40;
            ss << "What is LCM(" << a << ", " << b << ")?";
            p.question = ss.str();
            p.answer = (a*b)/gcd(a,b);
            p.hint = "LCM(a,b) = a*b / GCD(a,b)";
            break;
        }
        case 8: {
            int n = 3+rng()%5;
            ss << "What is the sum of 1 to " << n << "?";
            p.question = ss.str();
            p.answer = n*(n+1)/2;
            p.hint = "Sum = n*(n+1)/2";
            break;
        }
        case 9: {
            int a = 2+rng()%8, b = 2+rng()%8, c = 2+rng()%8;
            ss << "Solve: " << a << "x + " << b << " = " << c << ". What is x?";
            p.question = ss.str();
            p.answer = (c-b)/a;
            p.hint = "x = (c - b) / a";
            break;
        }
    }
    return p;
}

struct AppWidgets {
    GtkWidget *window;
    GtkWidget *stack;
    GtkWidget *status_page;
    GtkWidget *password_page;
    GtkWidget *unblock_page;
    GtkWidget *main_page;

    // Status page
    GtkWidget *status_label;
    GtkWidget *status_icon;
    GtkWidget *enable_btn;
    GtkWidget *disable_btn;
    GtkWidget *settings_btn;

    // Password page
    GtkWidget *pw_entry;
    GtkWidget *pw_confirm;
    GtkWidget *pw_set_btn;
    GtkWidget *pw_back_btn;

    // Unblock page
    GtkWidget *unblock_grid;
    GtkWidget *puzzle_label;
    GtkWidget *puzzle_hint;
    GtkWidget *puzzle_entry;
    GtkWidget *puzzle_submit;
    GtkWidget *puzzle_progress;
    GtkWidget *puzzle_back_btn;
    int puzzle_current;
    int puzzle_total;
    int puzzle_answer;
    std::mt19937 puzzle_rng;

    // Main page
    GtkWidget *main_status;
    GtkWidget *main_host_count;
    GtkWidget *main_pw_status;
    GtkWidget *main_block_btn;
    GtkWidget *main_unblock_btn;
    GtkWidget *main_status_btn;
};

static void updateStatus(AppWidgets *w) {
    bool blocked = isBlocked();
    bool hasPw = !loadPasswordHash().empty();

    int total = blocked_domains.size() + google_domains.size() + bing_domains.size() + blocked_search_engines.size();

    if (blocked) {
        gtk_label_set_markup(GTK_LABEL(w->status_label),
            "<span size='xx-large' weight='bold'>Protection Active</span>");
        gtk_widget_set_visible(w->enable_btn, FALSE);
        gtk_widget_set_visible(w->disable_btn, TRUE);
        gtk_widget_set_visible(w->settings_btn, FALSE);
    } else {
        gtk_label_set_markup(GTK_LABEL(w->status_label),
            "<span size='xx-large' weight='bold'>Protection Inactive</span>");
        gtk_widget_set_visible(w->enable_btn, TRUE);
        gtk_widget_set_visible(w->disable_btn, FALSE);
        gtk_widget_set_visible(w->settings_btn, hasPw);
    }

    if (w->main_status) {
        gtk_label_set_markup(GTK_LABEL(w->main_status),
            blocked
                ? "<span weight='bold' foreground='#2ecc71'>ACTIVE</span>"
                : "<span weight='bold' foreground='#e74c3c'>INACTIVE</span>");
        std::string host_str = std::to_string(total) + " entries configured";
        gtk_label_set_text(GTK_LABEL(w->main_host_count), host_str.c_str());
        gtk_label_set_text(GTK_LABEL(w->main_pw_status), hasPw ? "Password: Set" : "Password: Not set");
        gtk_widget_set_visible(w->main_block_btn, !blocked);
        gtk_widget_set_visible(w->main_unblock_btn, blocked && hasPw);
    }
}

static void backupHostsFile() {
    std::ifstream src(HOSTS_FILE, std::ios::binary);
    if (src.is_open()) {
        std::ofstream dst(BACKUP_HOSTS, std::ios::binary);
        dst << src.rdbuf();
    }
}

static void blockContent(AppWidgets *w) {
    if (isBlocked()) {
        GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
            GTK_DIALOG_MODAL, GTK_MESSAGE_INFO, GTK_BUTTONS_OK,
            "Content blocking is already active.");
        gtk_dialog_run(GTK_DIALOG(dlg));
        gtk_widget_destroy(dlg);
        return;
    }

    backupHostsFile();

    std::ofstream hosts(HOSTS_FILE, std::ios::app);
    if (!hosts.is_open()) {
        GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
            GTK_DIALOG_MODAL, GTK_MESSAGE_ERROR, GTK_BUTTONS_OK,
            "Cannot open hosts file. Run as root (sudo).");
        gtk_dialog_run(GTK_DIALOG(dlg));
        gtk_widget_destroy(dlg);
        return;
    }

    hosts << "\n# PBLOCK_START - Do not edit this section\n";

    for (const auto& domain : blocked_domains) {
        hosts << "127.0.0.1 " << domain << "\n";
        hosts << "127.0.0.1 www." << domain << "\n";
        hosts << "::1 " << domain << "\n";
        hosts << "::1 www." << domain << "\n";
    }

    for (const auto& domain : google_domains)
        hosts << GOOGLE_SAFESEARCH_IP << " " << domain << "\n";

    for (const auto& domain : bing_domains)
        hosts << BING_STRICT_IP << " " << domain << "\n";

    for (const auto& domain : blocked_search_engines) {
        hosts << "127.0.0.1 " << domain << "\n";
        hosts << "::1 " << domain << "\n";
    }

    hosts << "# PBLOCK_END\n";
    hosts.close();

    system("systemd-resolve --flush-caches 2>/dev/null || nscd -i hosts 2>/dev/null || true");

    GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
        GTK_DIALOG_MODAL, GTK_MESSAGE_INFO, GTK_BUTTONS_OK,
        "Content blocking activated!\n\n"
        "%d adult domains blocked.\n"
        "Google SafeSearch forced on %d domains.\n"
        "Bing strict mode forced on %d domains.\n"
        "%d search engines blocked.",
        (int)blocked_domains.size(), (int)google_domains.size(),
        (int)bing_domains.size(), (int)blocked_search_engines.size());
    gtk_dialog_run(GTK_DIALOG(dlg));
    gtk_widget_destroy(dlg);

    updateStatus(w);
}

static void unblockContent(AppWidgets *w) {
    std::ifstream hosts_in(HOSTS_FILE);
    std::vector<std::string> lines;
    std::string line;
    bool skip = false;

    while (std::getline(hosts_in, line)) {
        if (line.find("# PBLOCK_START") != std::string::npos) { skip = true; continue; }
        if (line.find("# PBLOCK_END") != std::string::npos) { skip = false; continue; }
        if (!skip) lines.push_back(line);
    }
    hosts_in.close();

    std::ofstream hosts_out(HOSTS_FILE);
    for (const auto& l : lines) hosts_out << l << "\n";
    hosts_out.close();

    system("systemd-resolve --flush-caches 2>/dev/null || nscd -i hosts 2>/dev/null || true");

    GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
        GTK_DIALOG_MODAL, GTK_MESSAGE_INFO, GTK_BUTTONS_OK,
        "Content blocking has been removed.");
    gtk_dialog_run(GTK_DIALOG(dlg));
    gtk_widget_destroy(dlg);

    updateStatus(w);
}

static void onPuzzleSubmit(GtkWidget*, AppWidgets *w);

static void startPuzzleChallenge(AppWidgets *w) {
    w->puzzle_current = 0;
    w->puzzle_total = 10;
    std::random_device rd;
    w->puzzle_rng = std::mt19937(rd());

    gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "unblock");

    char buf[128];
    Puzzle p = generatePuzzle(w->puzzle_rng);
    w->puzzle_answer = p.answer;

    snprintf(buf, sizeof(buf), "Puzzle 1 / %d", w->puzzle_total);
    gtk_label_set_text(GTK_LABEL(w->puzzle_progress), buf);
    gtk_label_set_text(GTK_LABEL(w->puzzle_label), p.question.c_str());
    gtk_label_set_text(GTK_LABEL(w->puzzle_hint), ("Hint: " + p.hint).c_str());
    gtk_entry_set_text(GTK_ENTRY(w->puzzle_entry), "");
    gtk_widget_grab_focus(w->puzzle_entry);
}

static void nextPuzzle(AppWidgets *w) {
    if (w->puzzle_current >= w->puzzle_total) {
        GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
            GTK_DIALOG_MODAL, GTK_MESSAGE_INFO, GTK_BUTTONS_OK,
            "All 10 puzzles solved!\n\n"
            "Unblocking in 30 seconds...\n"
            "Use this time to reconsider your decision.");
        gtk_dialog_run(GTK_DIALOG(dlg));
        gtk_widget_destroy(dlg);

        for (int i = 30; i > 0; i--) {
            char buf[64];
            snprintf(buf, sizeof(buf), "Unblocking in %d seconds...", i);
            gtk_label_set_text(GTK_LABEL(w->puzzle_progress), buf);
            while (gtk_events_pending()) gtk_main_iteration();
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }

        unblockContent(w);
        gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status");
        return;
    }

    Puzzle p = generatePuzzle(w->puzzle_rng);
    w->puzzle_answer = p.answer;

    char buf[128];
    snprintf(buf, sizeof(buf), "Puzzle %d / %d", w->puzzle_current + 1, w->puzzle_total);
    gtk_label_set_text(GTK_LABEL(w->puzzle_progress), buf);
    gtk_label_set_text(GTK_LABEL(w->puzzle_label), p.question.c_str());
    gtk_label_set_text(GTK_LABEL(w->puzzle_hint), ("Hint: " + p.hint).c_str());
    gtk_entry_set_text(GTK_ENTRY(w->puzzle_entry), "");
    gtk_widget_grab_focus(w->puzzle_entry);
}

static void onPuzzleSubmit(GtkWidget*, AppWidgets *w) {
    const char *text = gtk_entry_get_text(GTK_ENTRY(w->puzzle_entry));
    int answer = atoi(text);

    if (answer == w->puzzle_answer) {
        w->puzzle_current++;
        nextPuzzle(w);
    } else {
        GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
            GTK_DIALOG_MODAL, GTK_MESSAGE_WARNING, GTK_BUTTONS_OK,
            "Wrong answer! The correct answer was %d.\n"
            "Resetting to puzzle 1.", w->puzzle_answer);
        gtk_dialog_run(GTK_DIALOG(dlg));
        gtk_widget_destroy(dlg);
        w->puzzle_current = 0;
        nextPuzzle(w);
    }
}

static void onEnableClicked(GtkWidget*, AppWidgets *w) {
    if (!isBlocked()) {
        bool hasPw = !loadPasswordHash().empty();
        if (!hasPw) {
            gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "password");
            return;
        }
    }
    blockContent(w);
}

static void onDisableClicked(GtkWidget*, AppWidgets *w) {
    startPuzzleChallenge(w);
}

static void onSettingsClicked(GtkWidget*, AppWidgets *w) {
    gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "password");
}

static void onPasswordSet(GtkWidget*, AppWidgets *w) {
    const char *pw = gtk_entry_get_text(GTK_ENTRY(w->pw_entry));
    const char *confirm = gtk_entry_get_text(GTK_ENTRY(w->pw_confirm));

    if (strlen(pw) < 8) {
        GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
            GTK_DIALOG_MODAL, GTK_MESSAGE_WARNING, GTK_BUTTONS_OK,
            "Password must be at least 8 characters.");
        gtk_dialog_run(GTK_DIALOG(dlg));
        gtk_widget_destroy(dlg);
        return;
    }

    if (strcmp(pw, confirm) != 0) {
        GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
            GTK_DIALOG_MODAL, GTK_MESSAGE_WARNING, GTK_BUTTONS_OK,
            "Passwords do not match.");
        gtk_dialog_run(GTK_DIALOG(dlg));
        gtk_widget_destroy(dlg);
        return;
    }

    std::string hash = hashPassword(pw);
    savePasswordHash(hash);

    GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
        GTK_DIALOG_MODAL, GTK_MESSAGE_INFO, GTK_BUTTONS_OK,
        "Password set successfully!\n\n"
        "Write it down somewhere safe.\n"
        "You'll need it to disable blocking.");
    gtk_dialog_run(GTK_DIALOG(dlg));
    gtk_widget_destroy(dlg);

    gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status");
    updateStatus(w);
}

static void onPasswordBack(GtkWidget*, AppWidgets *w) {
    gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status");
}

static void onUnblockBack(GtkWidget*, AppWidgets *w) {
    GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
        GTK_DIALOG_MODAL, GTK_MESSAGE_QUESTION, GTK_BUTTONS_YES_NO,
        "Are you sure you want to cancel?\n"
        "Content blocking will remain active.");
    int result = gtk_dialog_run(GTK_DIALOG(dlg));
    gtk_widget_destroy(dlg);

    if (result == GTK_RESPONSE_YES) {
        gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status");
        updateStatus(w);
    }
}

static void onAbout(GtkWidget*, AppWidgets *w) {
    GtkWidget *dlg = gtk_message_dialog_new(GTK_WINDOW(w->window),
        GTK_DIALOG_MODAL, GTK_MESSAGE_INFO, GTK_BUTTONS_OK,
        "PBLOCK - Family Safety Content Filter\n\n"
        "Version 1.0\n"
        "Copyright 2026 Bekkali - SegMind25\n\n"
        "An open-source parental control tool that helps\n"
        "families maintain a safe online environment.\n\n"
        "Features:\n"
        "  - Blocks 200+ adult websites\n"
        "  - Forces SafeSearch on Google and Bing\n"
        "  - Password-protected settings\n"
        "  - 10-puzzle challenge to disable\n\n"
        "https://github.com/SegMind25/PBLOCK");
    gtk_dialog_run(GTK_DIALOG(dlg));
    gtk_widget_destroy(dlg);
}

static gboolean onKeyPress(GtkWidget*, GdkEventKey *event, AppWidgets *w) {
    if (event->keyval == GDK_KEY_Return || event->keyval == GDK_KEY_KP_Enter) {
        const char *visible = gtk_stack_get_visible_child_name(GTK_STACK(w->stack));
        if (strcmp(visible, "password") == 0) {
            onPasswordSet(NULL, w);
            return TRUE;
        } else if (strcmp(visible, "unblock") == 0) {
            onPuzzleSubmit(NULL, w);
            return TRUE;
        }
    }
    return FALSE;
}

static GtkWidget* createStatusPage(AppWidgets *w) {
    GtkWidget *box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
    gtk_widget_set_halign(box, GTK_ALIGN_CENTER);
    gtk_widget_set_valign(box, GTK_ALIGN_CENTER);
    gtk_container_set_border_width(GTK_CONTAINER(box), 40);

    w->status_icon = gtk_image_new_from_icon_name("dialog-information", GTK_ICON_SIZE_DIALOG);
    gtk_box_pack_start(GTK_BOX(box), w->status_icon, FALSE, FALSE, 0);

    w->status_label = gtk_label_new("");
    gtk_box_pack_start(GTK_BOX(box), w->status_label, FALSE, FALSE, 0);

    GtkWidget *subtitle = gtk_label_new("Parental Content Control & Family Safety");
    gtk_widget_set_opacity(subtitle, 0.6);
    gtk_box_pack_start(GTK_BOX(box), subtitle, FALSE, FALSE, 0);

    GtkWidget *sep = gtk_separator_new(GTK_ORIENTATION_HORIZONTAL);
    gtk_box_pack_start(GTK_BOX(box), sep, FALSE, FALSE, 10);

    w->enable_btn = gtk_button_new_with_label("Enable Content Filter");
    gtk_widget_set_size_request(w->enable_btn, 250, 50);
    GtkStyleContext *ctx = gtk_widget_get_style_context(w->enable_btn);
    gtk_style_context_add_class(ctx, "suggested-action");
    g_signal_connect(w->enable_btn, "clicked", G_CALLBACK(onEnableClicked), w);
    gtk_box_pack_start(GTK_BOX(box), w->enable_btn, FALSE, FALSE, 0);

    w->disable_btn = gtk_button_new_with_label("Disable Content Filter");
    gtk_widget_set_size_request(w->disable_btn, 250, 50);
    ctx = gtk_widget_get_style_context(w->disable_btn);
    gtk_style_context_add_class(ctx, "destructive-action");
    g_signal_connect(w->disable_btn, "clicked", G_CALLBACK(onDisableClicked), w);
    gtk_box_pack_start(GTK_BOX(box), w->disable_btn, FALSE, FALSE, 0);

    w->settings_btn = gtk_button_new_with_label("Change Password");
    gtk_widget_set_size_request(w->settings_btn, 250, 50);
    g_signal_connect(w->settings_btn, "clicked", G_CALLBACK(onSettingsClicked), w);
    gtk_box_pack_start(GTK_BOX(box), w->settings_btn, FALSE, FALSE, 0);

    GtkWidget *about_btn = gtk_button_new_with_label("About");
    g_signal_connect(about_btn, "clicked", G_CALLBACK(onAbout), w);
    gtk_box_pack_start(GTK_BOX(box), about_btn, FALSE, FALSE, 0);

    return box;
}

static GtkWidget* createPasswordPage(AppWidgets *w) {
    GtkWidget *box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
    gtk_widget_set_halign(box, GTK_ALIGN_CENTER);
    gtk_widget_set_valign(box, GTK_ALIGN_CENTER);
    gtk_container_set_border_width(GTK_CONTAINER(box), 40);

    GtkWidget *title = gtk_label_new(NULL);
    gtk_label_set_markup(GTK_LABEL(title),
        "<span size='x-large' weight='bold'>Set Accountability Password</span>");
    gtk_box_pack_start(GTK_BOX(box), title, FALSE, FALSE, 0);

    GtkWidget *desc = gtk_label_new(
        "This password will be required to disable blocking.\n"
        "Make it strong and WRITE IT DOWN somewhere safe.\n"
        "Minimum 8 characters.");
    gtk_label_set_line_wrap(GTK_LABEL(desc), TRUE);
    gtk_widget_set_halign(desc, GTK_ALIGN_CENTER);
    gtk_box_pack_start(GTK_BOX(box), desc, FALSE, FALSE, 10);

    w->pw_entry = gtk_entry_new();
    gtk_entry_set_visibility(GTK_ENTRY(w->pw_entry), FALSE);
    gtk_entry_set_placeholder_text(GTK_ENTRY(w->pw_entry), "Enter password");
    gtk_widget_set_size_request(w->pw_entry, 300, -1);
    gtk_box_pack_start(GTK_BOX(box), w->pw_entry, FALSE, FALSE, 0);

    w->pw_confirm = gtk_entry_new();
    gtk_entry_set_visibility(GTK_ENTRY(w->pw_confirm), FALSE);
    gtk_entry_set_placeholder_text(GTK_ENTRY(w->pw_confirm), "Confirm password");
    gtk_widget_set_size_request(w->pw_confirm, 300, -1);
    gtk_box_pack_start(GTK_BOX(box), w->pw_confirm, FALSE, FALSE, 0);

    w->pw_set_btn = gtk_button_new_with_label("Set Password");
    gtk_widget_set_size_request(w->pw_set_btn, 200, 45);
    GtkStyleContext *ctx2 = gtk_widget_get_style_context(w->pw_set_btn);
    gtk_style_context_add_class(ctx2, "suggested-action");
    g_signal_connect(w->pw_set_btn, "clicked", G_CALLBACK(onPasswordSet), w);
    gtk_box_pack_start(GTK_BOX(box), w->pw_set_btn, FALSE, FALSE, 10);

    w->pw_back_btn = gtk_button_new_with_label("Back");
    g_signal_connect(w->pw_back_btn, "clicked", G_CALLBACK(onPasswordBack), w);
    gtk_box_pack_start(GTK_BOX(box), w->pw_back_btn, FALSE, FALSE, 0);

    return box;
}

static GtkWidget* createUnblockPage(AppWidgets *w) {
    GtkWidget *box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
    gtk_widget_set_halign(box, GTK_ALIGN_CENTER);
    gtk_widget_set_valign(box, GTK_ALIGN_CENTER);
    gtk_container_set_border_width(GTK_CONTAINER(box), 40);

    GtkWidget *title = gtk_label_new(NULL);
    gtk_label_set_markup(GTK_LABEL(title),
        "<span size='x-large' weight='bold'>Algorithm Puzzle Challenge</span>");
    gtk_box_pack_start(GTK_BOX(box), title, FALSE, FALSE, 0);

    GtkWidget *desc = gtk_label_new("Solve 10 puzzles to disable blocking.\nEach puzzle is harder than the last!");
    gtk_widget_set_halign(desc, GTK_ALIGN_CENTER);
    gtk_box_pack_start(GTK_BOX(box), desc, FALSE, FALSE, 5);

    w->puzzle_progress = gtk_label_new("Puzzle 1 / 10");
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_progress, FALSE, FALSE, 0);

    w->puzzle_label = gtk_label_new("");
    gtk_label_set_line_wrap(GTK_LABEL(w->puzzle_label), TRUE);
    gtk_label_set_selectable(GTK_LABEL(w->puzzle_label), TRUE);
    gtk_widget_set_halign(w->puzzle_label, GTK_ALIGN_CENTER);
    PangoAttrList *attrs = pango_attr_list_new();
    pango_attr_list_insert(attrs, pango_attr_size_new(14000));
    pango_attr_list_insert(attrs, pango_attr_weight_new(PANGO_WEIGHT_BOLD));
    gtk_label_set_attributes(GTK_LABEL(w->puzzle_label), attrs);
    pango_attr_list_unref(attrs);
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_label, FALSE, FALSE, 10);

    w->puzzle_hint = gtk_label_new("");
    gtk_widget_set_opacity(w->puzzle_hint, 0.6);
    gtk_widget_set_halign(w->puzzle_hint, GTK_ALIGN_CENTER);
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_hint, FALSE, FALSE, 0);

    w->puzzle_entry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(w->puzzle_entry), "Enter your answer");
    gtk_widget_set_size_request(w->puzzle_entry, 200, -1);
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_entry, FALSE, FALSE, 10);

    w->puzzle_submit = gtk_button_new_with_label("Submit Answer");
    gtk_widget_set_size_request(w->puzzle_submit, 200, 45);
    GtkStyleContext *ctx = gtk_widget_get_style_context(w->puzzle_submit);
    gtk_style_context_add_class(ctx, "suggested-action");
    g_signal_connect(w->puzzle_submit, "clicked", G_CALLBACK(onPuzzleSubmit), w);
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_submit, FALSE, FALSE, 0);

    w->puzzle_back_btn = gtk_button_new_with_label("Cancel");
    g_signal_connect(w->puzzle_back_btn, "clicked", G_CALLBACK(onUnblockBack), w);
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_back_btn, FALSE, FALSE, 5);

    return box;
}

int main(int argc, char *argv[]) {
    if (geteuid() != 0) {
        std::cerr << "PBLOCK must be run as root. Use: sudo pblock\n";
        return 1;
    }

    gtk_init(&argc, &argv);

    AppWidgets *w = new AppWidgets();
    std::random_device rd;
    w->puzzle_rng = std::mt19937(rd());

    w->window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(w->window), "PBLOCK - Family Safety Content Filter");
    gtk_window_set_default_size(GTK_WINDOW(w->window), 500, 600);
    gtk_window_set_position(GTK_WINDOW(w->window), GTK_WIN_POS_CENTER);
    gtk_window_set_resizable(GTK_WINDOW(w->window), FALSE);
    g_signal_connect(w->window, "destroy", G_CALLBACK(gtk_main_quit), NULL);
    g_signal_connect(w->window, "key-press-event", G_CALLBACK(onKeyPress), w);

    GtkCssProvider *css = gtk_css_provider_new();
    gtk_css_provider_load_from_data(css,
        "window { background-color: #f5f5f5; }\n"
        ".title { font-size: 18px; font-weight: bold; }\n"
        "button { padding: 8px 16px; border-radius: 6px; }\n"
        "entry { padding: 8px; border-radius: 6px; }\n"
        ".suggested-action { background-color: #3498db; color: white; }\n"
        ".destructive-action { background-color: #e74c3c; color: white; }\n",
        -1, NULL);
    GtkStyleContext *styleCtx = gtk_widget_get_style_context(w->window);
    gtk_style_context_add_provider(styleCtx, GTK_STYLE_PROVIDER(css),
        GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);

    w->stack = gtk_stack_new();
    gtk_stack_set_transition_type(GTK_STACK(w->stack), GTK_STACK_TRANSITION_TYPE_SLIDE_LEFT_RIGHT);
    gtk_stack_set_transition_duration(GTK_STACK(w->stack), 300);
    gtk_container_add(GTK_CONTAINER(w->window), w->stack);

    gtk_stack_add_named(GTK_STACK(w->stack), createStatusPage(w), "status");
    gtk_stack_add_named(GTK_STACK(w->stack), createPasswordPage(w), "password");
    gtk_stack_add_named(GTK_STACK(w->stack), createUnblockPage(w), "unblock");

    gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status");
    updateStatus(w);

    gtk_widget_show_all(w->window);
    gtk_main();

    delete w;
    return 0;
}
