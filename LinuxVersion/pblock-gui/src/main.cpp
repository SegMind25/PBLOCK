#include <gtk/gtk.h>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <crypt.h>
#include <cstring>
#include <algorithm>
#include <random>
#include <cmath>
#include <sstream>
#include <sys/stat.h>
#include <cstdlib>

static const std::string HOSTS_FILE = "/etc/hosts";
static const std::string CONFIG_FILE = "/etc/pblock.conf";
static const std::string BACKUP_HOSTS = "/etc/pblock_hosts_backup";
static const int TOTAL_PUZZLES = 10;
static const int SLOTS = 12;
static const int MAX_STEPS = 50;
static const int STEP_MS = 180;

static const int VOID = 0, RED = 1, GREEN = 2, BLUE = 3, YELLOW_C = 4, PURPLE = 5;
static const int DX[] = {0, 0, -1, 1};
static const int DY[] = {-1, 1, 0, 0};
static const char* ARROW[] = {"\u2191", "\u2193", "\u2190", "\u2192"};

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
    "hentaihaven.xxx", "hanime.tv", "nhentai.net",
    "rule34.xxx", "e-hentai.org", "hentai2read.com",
    "chaturbate.com", "bongacams.com", "stripchat.com",
    "cam4.com", "myfreecams.com", "livejasmin.com",
    "camsoda.com", "flirt4free.com", "streamate.com",
    "onlyfans.com", "fansly.com", "manyvids.com",
    "imagefap.com", "sex.com", "literotica.com",
    "eroshare.com", "alohatube.com", "xxxbunker.com",
    "pornmd.com", "nudevista.com", "tubegalore.com",
    "porn300.com", "lobstertube.com", "bellesa.co",
    "daftsex.com", "sxyprn.com", "ok.xxx", "megatube.xxx",
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
    "yourlust.com", "yuvutu.com", "zedporn.com"
};

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
    "google.ro", "www.google.ro",
    "google.com.pk", "www.google.com.pk",
    "google.com.ph", "www.google.com.ph",
    "google.com.ng", "www.google.com.ng",
    "google.co.th", "www.google.co.th",
    "google.co.kr", "www.google.co.kr"
};

static std::vector<std::string> bing_domains = {
    "bing.com", "www.bing.com",
    "bing.co.uk", "www.bing.co.uk",
    "bing.fr", "www.bing.fr",
    "bing.de", "www.bing.de",
    "bing.es", "www.bing.es",
    "bing.it", "www.bing.it",
    "bing.ca", "www.bing.ca",
    "bing.com.au", "www.bing.com.au"
};

static std::vector<std::string> blocked_search_engines = {
    "duckduckgo.com", "www.duckduckgo.com",
    "startpage.com", "www.startpage.com",
    "qwant.com", "www.qwant.com",
    "search.brave.com", "brave.com",
    "yandex.com", "www.yandex.com",
    "baidu.com", "www.baidu.com",
    "sogou.com", "www.sogou.com",
    "ecosia.org", "www.ecosia.org",
    "mojeek.com", "www.mojeek.com",
    "searx.be", "search.naver.com"
};

static const unsigned int TILE_COLORS[] = {0, 0xFF4757FFu, 0x2ED573FFu, 0x3742FAFFu, 0xFFA502FFu, 0xA855F7FFu};

struct Cmd {
    bool isFunc = false;
    int func = 0, dir = 0, guard = 0;
    bool isTurnLeft = false, isTurnRight = false, isJump = false;
    std::string label() const {
        if (isTurnLeft) return "\u21BA";
        if (isTurnRight) return "\u21BB";
        if (isJump) return "\u2B62";
        std::string s = isFunc ? ("F" + std::to_string(func)) : ARROW[dir];
        if (guard != 0) return "\u25CF" + s;
        return s;
    }
    unsigned int cmdColor() const {
        if (isTurnLeft || isTurnRight) return 0xFF6348FF;
        if (isJump) return 0xA855F7FF;
        if (isFunc) return 0x7C3AEDFF;
        return guard >= 0 && guard <= 5 ? TILE_COLORS[guard] : 0x3742FAFF;
    }
};

struct Puzzle {
    int cells[20][20] = {};
    bool goals[20][20] = {};
    bool collected[20][20] = {};
    int W = 7, H = 6;
    int startX = 0, startY = 2, startDir = 3;
    int botX = 0, botY = 0, botDir = 0;
    int goalsTotal = 0, goalsCollected = 0;
    void resetBot() {
        botX = startX; botY = startY; botDir = startDir;
        memset(collected, 0, sizeof(collected));
        goalsCollected = 0;
        if (goals[startY][startX]) { collected[startY][startX] = true; goalsCollected = 1; }
    }
};

struct AppWidgets {
    GtkWidget *window;
    GtkWidget *stack;

    // Status page
    GtkWidget *status_label, *status_icon;
    GtkWidget *enable_btn, *disable_btn, *settings_btn;

    // Password page
    GtkWidget *pw_entry, *pw_confirm, *pw_set_btn, *pw_back_btn;

    // Puzzle page
    GtkWidget *board_drawing;
    GtkWidget *puzzle_label, *puzzle_hint, *puzzle_entry, *puzzle_submit;
    GtkWidget *puzzle_progress_label;
    GtkWidget *f1_btns[SLOTS], *f2_btns[SLOTS];
    GtkWidget *puzzle_back_btn;
    GtkWidget *puzzle_msg;
    GtkWidget *run_btn, *reset_btn;

    Cmd f1[SLOTS], f2[SLOTS];
    Puzzle puzzle;
    bool running = false;
    int puzzle_current = 0;
    int steps_used = 0;
    std::mt19937 rng;
    Cmd* current_slot_arr = nullptr;
    int current_slot_idx = 0;
};

static AppWidgets* g_app = nullptr;

static int countCells(Puzzle& p) {
    int n = 0;
    for (int y = 0; y < p.H; y++)
        for (int x = 0; x < p.W; x++)
            if (p.cells[y][x] != VOID) n++;
    return n;
}

static int opposite(int d) { return d == 0 ? 1 : d == 1 ? 0 : d == 2 ? 3 : 2; }

static void generatePuzzle(Puzzle& p, std::mt19937& rng, int idx) {
    int legs = 4 + idx * 2;
    for (int attempt = 0; attempt < 500; attempt++) {
        p.W = 7 + rng() % 5; p.H = 6 + rng() % 4;
        memset(p.cells, 0, sizeof(p.cells));
        memset(p.goals, 0, sizeof(p.goals));
        p.startX = 1 + rng() % (p.W - 2); p.startY = 1 + rng() % (p.H - 2);
        p.startDir = rng() % 4;
        int curX = p.startX, curY = p.startY, lastDir = -1;
        int prevColor = 1 + rng() % 5;
        p.cells[curY][curX] = prevColor;
        bool ok = true;
        for (int leg = 0; leg < legs && ok; leg++) {
            std::vector<int> dirs;
            for (int d = 0; d < 4; d++)
                if (d != lastDir && (lastDir == -1 || d != opposite(lastDir))) dirs.push_back(d);
            std::shuffle(dirs.begin(), dirs.end(), rng);
            int len = 2 + rng() % 5;
            int color; do { color = 1 + rng() % 5; } while (color == prevColor && rng() % 2);
            bool placed = false;
            for (int dir : dirs) {
                int tx = curX, ty = curY; bool fits = true;
                for (int i = 0; i < len; i++) {
                    tx += DX[dir]; ty += DY[dir];
                    if (tx < 0 || ty < 0 || tx >= p.W || ty >= p.H || p.cells[ty][tx] != VOID) { fits = false; break; }
                }
                if (!fits) continue;
                tx = curX; ty = curY;
                for (int i = 0; i < len; i++) { tx += DX[dir]; ty += DY[dir]; p.cells[ty][tx] = color; }
                curX = tx; curY = ty; lastDir = dir; prevColor = color; placed = true; break;
            }
            if (!placed) ok = false;
        }
        if (!ok || countCells(p) < 10) continue;
        p.goals[curY][curX] = true; p.goalsTotal = 1;
        for (int y = 0; y < p.H; y++)
            for (int x = 0; x < p.W; x++)
                if (!p.goals[y][x] && p.cells[y][x] != VOID && rng() % 3 == 0) { p.goals[y][x] = true; p.goalsTotal++; }
        if (p.goalsTotal < 4) p.goalsTotal = 4;
        return;
    }
    p.W = 9; p.H = 7;
    memset(p.cells, 0, sizeof(p.cells)); memset(p.goals, 0, sizeof(p.goals));
    for (int x = 0; x < p.W; x++) { p.cells[2][x] = RED; p.cells[4][x] = GREEN; }
    for (int y = 2; y < p.H; y++) { p.cells[y][3] = BLUE; p.cells[y][6] = YELLOW_C; }
    p.startX = 0; p.startY = 2; p.startDir = 3;
    p.goals[2][p.W - 1] = true; p.goals[4][0] = true; p.goals[6][6] = true; p.goalsTotal = 3;
}

static std::string generateSalt() {
    const char charset[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./";
    std::string salt = "$6$";
    std::ifstream urandom("/dev/urandom", std::ios::binary);
    if (urandom.is_open()) {
        for (int i = 0; i < 16; i++) { unsigned char c; urandom.read(reinterpret_cast<char*>(&c), 1); salt += charset[c % (sizeof(charset) - 1)]; }
        urandom.close();
    }
    salt += "$"; return salt;
}

static std::string hashPassword(const std::string& pw) { return crypt(pw.c_str(), generateSalt().c_str()); }
static void savePasswordHash(const std::string& h) { std::ofstream f(CONFIG_FILE); f << h << std::endl; f.close(); chmod(CONFIG_FILE.c_str(), 0600); }
static std::string loadPasswordHash() { std::ifstream f(CONFIG_FILE); std::string h; if (f.is_open()) { std::getline(f, h); f.close(); } return h; }
static bool isBlocked() {
    std::ifstream f(HOSTS_FILE); std::string line;
    while (std::getline(f, line)) if (line.find("# PBLOCK_START") != std::string::npos) return true;
    return false;
}

static void backupHosts() {
    std::ifstream src(HOSTS_FILE, std::ios::binary);
    if (src.is_open()) { std::ofstream dst(BACKUP_HOSTS, std::ios::binary); dst << src.rdbuf(); }
}

static void on_board_draw(GtkWidget*, cairo_t* cr, gpointer) {
    Puzzle& p = g_app->puzzle;
    int ww = gtk_widget_get_allocated_width(g_app->board_drawing);
    int wh = gtk_widget_get_allocated_height(g_app->board_drawing);

    cairo_set_source_rgba(cr, 0.059, 0.059, 0.137, 1);
    cairo_paint(cr);

    if (p.W == 0 || p.H == 0) return;
    double tile = std::min((double)ww / p.W, (double)wh / p.H) * 0.88;
    double oX = (ww - tile * p.W) / 2.0, oY = (wh - tile * p.H) / 2.0;

    // Grid
    cairo_set_source_rgba(cr, 0.102, 0.102, 0.243, 1);
    cairo_set_line_width(cr, 1);
    for (int y = 0; y <= p.H; y++) { cairo_move_to(cr, oX, oY + y * tile); cairo_line_to(cr, oX + p.W * tile, oY + y * tile); cairo_stroke(cr); }
    for (int x = 0; x <= p.W; x++) { cairo_move_to(cr, oX + x * tile, oY); cairo_line_to(cr, oX + x * tile, oY + p.H * tile); cairo_stroke(cr); }

    for (int y = 0; y < p.H; y++) {
        for (int x = 0; x < p.W; x++) {
            if (p.cells[y][x] == VOID) continue;
            double left = oX + x * tile + tile * 0.06;
            double top = oY + y * tile + tile * 0.06;
            double sz = tile * 0.88;
            double r = tile * 0.12;
            unsigned int color = TILE_COLORS[p.cells[y][x]];

            // Glow
            double gr = (color >> 24) / 255.0, gg = ((color >> 16) & 0xFF) / 255.0, gb = ((color >> 8) & 0xFF) / 255.0;
            cairo_set_source_rgba(cr, gr, gg, gb, 0.2);
            cairo_rectangle(cr, left - tile * 0.03, top - tile * 0.03, sz + tile * 0.06, sz + tile * 0.06);
            cairo_fill(cr);

            // Tile
            cairo_set_source_rgba(cr, gr, gg, gb, 1);
            double x0 = left, y0 = top, rr = r;
            cairo_new_sub_path(cr); cairo_arc(cr, x0 + rr, y0 + rr, rr, M_PI, 1.5 * M_PI);
            cairo_arc(cr, x0 + sz - rr, y0 + rr, rr, 1.5 * M_PI, 2 * M_PI);
            cairo_arc(cr, x0 + sz - rr, y0 + sz - rr, rr, 0, 0.5 * M_PI);
            cairo_arc(cr, x0 + rr, y0 + sz - rr, rr, 0.5 * M_PI, M_PI);
            cairo_close_path(cr); cairo_fill(cr);

            // Inner highlight
            cairo_set_source_rgba(cr, 1, 1, 1, 0.1);
            cairo_rectangle(cr, left + tile * 0.1, top + tile * 0.1, sz * 0.4, sz * 0.4);
            cairo_fill(cr);

            // Goal
            if (p.goals[y][x]) {
                bool got = p.goalsCollected > 0 && p.collected[y][x];
                cairo_set_source_rgba(cr, 1, 1, 1, got ? 0.27 : 1);
                cairo_arc(cr, left + sz / 2, top + sz / 2, tile * 0.15, 0, 2 * M_PI);
                cairo_fill(cr);
                if (!got) { cairo_set_source_rgba(cr, 1, 1, 1, 0.4); cairo_arc(cr, left + sz / 2, top + sz / 2, tile * 0.22, 0, 2 * M_PI); cairo_fill(cr); }
            }
        }
    }

    // Bot
    double bx = oX + p.botX * tile + tile * 0.06 + tile * 0.44;
    double by = oY + p.botY * tile + tile * 0.06 + tile * 0.44;
    cairo_set_source_rgba(cr, 0.18, 0.835, 0.45, 0.27);
    cairo_arc(cr, bx, by, tile * 0.38, 0, 2 * M_PI); cairo_fill(cr);
    cairo_set_source_rgba(cr, 0.18, 0.835, 0.45, 1);
    cairo_arc(cr, bx, by, tile * 0.28, 0, 2 * M_PI); cairo_fill(cr);
    cairo_set_line_width(cr, tile * 0.04);
    cairo_set_source_rgba(cr, 1, 1, 1, 1);
    cairo_arc(cr, bx, by, tile * 0.28, 0, 2 * M_PI); cairo_stroke(cr);
    double ddx = DX[p.botDir] * tile * 0.18, ddy = DY[p.botDir] * tile * 0.18;
    cairo_set_source_rgba(cr, 1, 1, 1, 1);
    cairo_arc(cr, bx + ddx, by + ddy, tile * 0.05, 0, 2 * M_PI); cairo_fill(cr);
}

static void refreshSlotButtons(AppWidgets* w) {
    for (int i = 0; i < SLOTS; i++) {
        for (int f = 0; f < 2; f++) {
            GtkWidget* btn = f == 0 ? w->f1_btns[i] : w->f2_btns[i];
            Cmd& c = f == 0 ? w->f1[i] : w->f2[i];
            if (c.isFunc || c.dir || c.guard || c.isTurnLeft || c.isTurnRight || c.isJump) {
                gtk_button_set_label(GTK_BUTTON(btn), c.label().c_str());
                unsigned int clr = c.cmdColor();
                char css[96];
                snprintf(css, sizeof(css), "* { background-color: #%06X; color: white; font-weight: bold; }", (unsigned int)((clr >> 8) & 0xFFFFFF));
                GtkCssProvider* prov = gtk_css_provider_new();
                gtk_css_provider_load_from_data(prov, css, -1, NULL);
                GtkStyleContext* ctx = gtk_widget_get_style_context(btn);
                gtk_style_context_add_provider(ctx, GTK_STYLE_PROVIDER(prov), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
                g_object_unref(prov);
            } else {
                gtk_button_set_label(GTK_BUTTON(btn), "\u2014");
                GtkCssProvider* prov = gtk_css_provider_new();
                gtk_css_provider_load_from_data(prov, "* { background-color: #1A1A2E; color: white; }", -1, NULL);
                GtkStyleContext* ctx = gtk_widget_get_style_context(btn);
                gtk_style_context_add_provider(ctx, GTK_STYLE_PROVIDER(prov), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
                g_object_unref(prov);
            }
        }
    }
}

static void resetBot(AppWidgets* w) {
    w->puzzle.resetBot();
    gtk_widget_queue_draw(w->board_drawing);
}

static void runStep(AppWidgets* w);

static gboolean runStepTimer(gpointer) {
    if (g_app && g_app->running) runStep(g_app);
    return FALSE;
}

static void runStep(AppWidgets* w) {
    if (!w->running) return;
    Puzzle& p = w->puzzle;

    // Simple interpreter: execute f1 sequentially with basic loop (F1 in F1)
    static int pc = 0;
    static int stepCount = 0;

    // Find next non-null command
    while (pc < SLOTS && w->f1[pc].isFunc == false && !w->f1[pc].isTurnLeft && !w->f1[pc].isTurnRight && !w->f1[pc].isJump && w->f1[pc].dir == 0 && w->f1[pc].guard == 0) pc++;
    if (pc >= SLOTS) { w->running = false; gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FF4757'>Program ended - targets not collected</span>"); return; }

    stepCount++;
    if (stepCount > MAX_STEPS) { w->running = false; pc = 0; stepCount = 0; gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FF4757'>Step limit exceeded! Optimize.</span>"); return; }

    Cmd& cmd = w->f1[pc]; pc++;

    if (cmd.isTurnLeft) { p.botDir = (p.botDir + 3) % 4; gtk_widget_queue_draw(w->board_drawing); g_timeout_add(STEP_MS, runStepTimer, w); return; }
    if (cmd.isTurnRight) { p.botDir = (p.botDir + 1) % 4; gtk_widget_queue_draw(w->board_drawing); g_timeout_add(STEP_MS, runStepTimer, w); return; }
    if (cmd.isJump) {
        int nx = p.botX + DX[p.botDir] * 2, ny = p.botY + DY[p.botDir] * 2;
        if (nx < 0 || ny < 0 || nx >= p.W || ny >= p.H || p.cells[ny][nx] == VOID) {
            w->running = false; pc = 0; stepCount = 0;
            gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FF4757'>Jump crash!</span>"); return;
        }
        p.botX = nx; p.botY = ny;
        if (p.goals[ny][nx] && !p.collected[ny][nx]) { p.collected[ny][nx] = true; p.goalsCollected++; }
        gtk_widget_queue_draw(w->board_drawing);
        if (p.goalsCollected >= p.goalsTotal) {
            w->running = false; pc = 0; stepCount = 0;
            gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#2ED573' weight='bold'>CHALLENGE COMPLETE!</span>");
            w->puzzle_current++;
            if (w->puzzle_current >= TOTAL_PUZZLES) {
                gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#2ED573' weight='bold'>ALL 10 CHALLENGES SOLVED!</span>");
            } else {
                g_timeout_add(1500, [](gpointer) -> gboolean {
                    generatePuzzle(g_app->puzzle, g_app->rng, g_app->puzzle_current);
                    for (int i = 0; i < SLOTS; i++) g_app->f1[i] = Cmd();
                    for (int i = 0; i < SLOTS; i++) g_app->f2[i] = Cmd();
                    resetBot(g_app); refreshSlotButtons(g_app);
                    char buf[128]; snprintf(buf, sizeof(buf), "Challenge %d / %d", g_app->puzzle_current + 1, TOTAL_PUZZLES);
                    gtk_label_set_text(GTK_LABEL(g_app->puzzle_progress_label), buf);
                    gtk_label_set_markup(GTK_LABEL(g_app->puzzle_msg), "<span foreground='#FFD93D'>Program the bot to collect all targets</span>");
                    gtk_widget_queue_draw(g_app->board_drawing);
                    return FALSE;
                }, NULL);
            }
            return;
        }
        g_timeout_add(STEP_MS, runStepTimer, w); return;
    }

    if (cmd.guard != 0 && p.cells[p.botY][p.botX] != cmd.guard) { g_timeout_add(STEP_MS, runStepTimer, w); return; }

    int nx = p.botX + DX[cmd.dir], ny = p.botY + DY[cmd.dir];
    if (nx < 0 || ny < 0 || nx >= p.W || ny >= p.H || p.cells[ny][nx] == VOID) {
        w->running = false; pc = 0; stepCount = 0;
        gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FF4757'>CRASH!</span>"); return;
    }
    p.botX = nx; p.botY = ny; p.botDir = cmd.dir;
    if (p.goals[ny][nx] && !p.collected[ny][nx]) { p.collected[ny][nx] = true; p.goalsCollected++; }
    gtk_widget_queue_draw(w->board_drawing);
    if (p.goalsCollected >= p.goalsTotal) {
        w->running = false; pc = 0; stepCount = 0;
        gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#2ED573' weight='bold'>CHALLENGE COMPLETE!</span>");
        w->puzzle_current++;
        if (w->puzzle_current >= TOTAL_PUZZLES) {
            gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#2ED573' weight='bold'>ALL 10 CHALLENGES SOLVED!</span>");
        } else {
            g_timeout_add(1500, [](gpointer) -> gboolean {
                generatePuzzle(g_app->puzzle, g_app->rng, g_app->puzzle_current);
                for (int i = 0; i < SLOTS; i++) g_app->f1[i] = Cmd();
                for (int i = 0; i < SLOTS; i++) g_app->f2[i] = Cmd();
                resetBot(g_app); refreshSlotButtons(g_app);
                char buf[128]; snprintf(buf, sizeof(buf), "Challenge %d / %d", g_app->puzzle_current + 1, TOTAL_PUZZLES);
                gtk_label_set_text(GTK_LABEL(g_app->puzzle_progress_label), buf);
                gtk_label_set_markup(GTK_LABEL(g_app->puzzle_msg), "<span foreground='#FFD93D'>Program the bot to collect all targets</span>");
                gtk_widget_queue_draw(g_app->board_drawing);
                return FALSE;
            }, NULL);
        }
        return;
    }
    g_timeout_add(STEP_MS, runStepTimer, w);
}

static void on_run_clicked(GtkWidget*, AppWidgets* w) {
    if (w->running) return;
    bool empty = true;
    for (int i = 0; i < SLOTS; i++) if (w->f1[i].isFunc || w->f1[i].dir || w->f1[i].guard || w->f1[i].isTurnLeft || w->f1[i].isTurnRight || w->f1[i].isJump) { empty = false; break; }
    if (empty) { gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FF4757'>F1 is empty!</span>"); return; }
    w->running = true; w->steps_used = 0; w->puzzle.resetBot();
    gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FFD93D'>Running...</span>");
    gtk_widget_queue_draw(w->board_drawing);
    g_timeout_add(STEP_MS, runStepTimer, w);
}

static void on_reset_clicked(GtkWidget*, AppWidgets* w) {
    w->running = false;
    w->puzzle.resetBot();
    gtk_widget_queue_draw(w->board_drawing);
    gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FFD93D'>Program the bot to collect all targets</span>");
}

// Hosts blocking
static void blockContent(AppWidgets* w) {
    if (isBlocked()) return;
    backupHosts();
    std::ofstream hosts(HOSTS_FILE, std::ios::app);
    if (!hosts.is_open()) return;
    hosts << "\n# PBLOCK_START - Do not edit this section\n";
    for (const auto& d : blocked_domains) { hosts << "127.0.0.1 " << d << "\n127.0.0.1 www." << d << "\n::1 " << d << "\n::1 www." << d << "\n"; }
    for (const auto& d : google_domains) hosts << "216.239.38.120 " << d << "\n";
    for (const auto& d : bing_domains) hosts << "204.79.197.220 " << d << "\n";
    for (const auto& d : blocked_search_engines) { hosts << "127.0.0.1 " << d << "\n::1 " << d << "\n"; }
    hosts << "# PBLOCK_END\n"; hosts.close();
    system("systemd-resolve --flush-caches 2>/dev/null || nscd -i hosts 2>/dev/null || true");
}

static void unblockContent(AppWidgets* w) {
    std::ifstream f(HOSTS_FILE); std::vector<std::string> lines; std::string line; bool skip = false;
    while (std::getline(f, line)) {
        if (line.find("# PBLOCK_START") != std::string::npos) { skip = true; continue; }
        if (line.find("# PBLOCK_END") != std::string::npos) { skip = false; continue; }
        if (!skip) lines.push_back(line);
    }
    f.close();
    std::ofstream out(HOSTS_FILE); for (const auto& l : lines) out << l << "\n"; out.close();
    system("systemd-resolve --flush-caches 2>/dev/null || nscd -i hosts 2>/dev/null || true");
}

static void updateStatus(AppWidgets* w) {
    bool blocked = isBlocked(); bool hasPw = !loadPasswordHash().empty();
    if (blocked) {
        gtk_label_set_markup(GTK_LABEL(w->status_label), "<span size='xx-large' weight='bold'>Protection Active</span>");
        gtk_widget_set_visible(w->enable_btn, FALSE);
        gtk_widget_set_visible(w->disable_btn, TRUE);
        gtk_widget_set_visible(w->settings_btn, FALSE);
    } else {
        gtk_label_set_markup(GTK_LABEL(w->status_label), "<span size='xx-large' weight='bold'>Protection Inactive</span>");
        gtk_widget_set_visible(w->enable_btn, TRUE);
        gtk_widget_set_visible(w->disable_btn, FALSE);
        gtk_widget_set_visible(w->settings_btn, hasPw);
    }
}

static void onEnable(GtkWidget*, AppWidgets* w) { if (!loadPasswordHash().empty()) blockContent(w); updateStatus(w); }
static void onDisable(GtkWidget*, AppWidgets* w) {
    w->puzzle_current = 0;
    generatePuzzle(w->puzzle, w->rng, 0);
    for (int i = 0; i < SLOTS; i++) w->f1[i] = Cmd();
    for (int i = 0; i < SLOTS; i++) w->f2[i] = Cmd();
    resetBot(w); refreshSlotButtons(w);
    char buf[128]; snprintf(buf, sizeof(buf), "Challenge 1 / %d", TOTAL_PUZZLES);
    gtk_label_set_text(GTK_LABEL(w->puzzle_progress_label), buf);
    gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "puzzle");
}

static void onPuzzleBack(GtkWidget*, AppWidgets* w) { gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status"); updateStatus(w); }
static void onSettings(GtkWidget*, AppWidgets* w) { gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "password"); }
static void onPwBack(GtkWidget*, AppWidgets* w) { gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status"); }

static void onPwSet(GtkWidget*, AppWidgets* w) {
    const char* pw = gtk_entry_get_text(GTK_ENTRY(w->pw_entry));
    const char* cf = gtk_entry_get_text(GTK_ENTRY(w->pw_confirm));
    if (strlen(pw) < 8 || strcmp(pw, cf) != 0) return;
    savePasswordHash(hashPassword(pw));
    gtk_stack_set_visible_child_name(GTK_STACK(w->stack), "status");
    updateStatus(w);
}

static void onSlotClicked(GtkWidget* btn, gpointer data) {
    int idx = GPOINTER_TO_INT(data) & 0xFF;
    bool isF1 = (GPOINTER_TO_INT(data) >> 8) & 1;
    Cmd* arr = isF1 ? g_app->f1 : g_app->f2;

    // Simple cycle: none -> forward -> turn right -> turn left -> F1 -> clear
    static int stateMap[SLOTS * 2] = {};
    int key = isF1 ? idx : idx + SLOTS;
    stateMap[key] = (stateMap[key] + 1) % 5;

    arr[idx] = Cmd();
    switch (stateMap[key]) {
        case 1: arr[idx].dir = 0; break; // up
        case 2: arr[idx].dir = 3; break; // right
        case 3: arr[idx].isTurnRight = true; break;
        case 4: arr[idx].isFunc = true; arr[idx].func = 1; break;
        default: break; // clear
    }
    refreshSlotButtons(g_app);
}

static GtkWidget* createStatusPage(AppWidgets* w) {
    GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
    gtk_widget_set_halign(box, GTK_ALIGN_CENTER); gtk_widget_set_valign(box, GTK_ALIGN_CENTER);
    gtk_container_set_border_width(GTK_CONTAINER(box), 40);
    w->status_icon = gtk_image_new_from_icon_name("dialog-information", GTK_ICON_SIZE_DIALOG);
    gtk_box_pack_start(GTK_BOX(box), w->status_icon, FALSE, FALSE, 0);
    w->status_label = gtk_label_new("");
    gtk_box_pack_start(GTK_BOX(box), w->status_label, FALSE, FALSE, 0);
    GtkWidget* sub = gtk_label_new("Parental Content Control & Family Safety");
    gtk_widget_set_opacity(sub, 0.6); gtk_box_pack_start(GTK_BOX(box), sub, FALSE, FALSE, 0);
    gtk_box_pack_start(GTK_BOX(box), gtk_separator_new(GTK_ORIENTATION_HORIZONTAL), FALSE, FALSE, 10);
    w->enable_btn = gtk_button_new_with_label("Enable Content Filter");
    gtk_widget_set_size_request(w->enable_btn, 250, 50);
    g_signal_connect(w->enable_btn, "clicked", G_CALLBACK(onEnable), w);
    gtk_box_pack_start(GTK_BOX(box), w->enable_btn, FALSE, FALSE, 0);
    w->disable_btn = gtk_button_new_with_label("Disable (solve 10 puzzles)");
    gtk_widget_set_size_request(w->disable_btn, 250, 50);
    g_signal_connect(w->disable_btn, "clicked", G_CALLBACK(onDisable), w);
    gtk_box_pack_start(GTK_BOX(box), w->disable_btn, FALSE, FALSE, 0);
    w->settings_btn = gtk_button_new_with_label("Change Password");
    gtk_widget_set_size_request(w->settings_btn, 250, 50);
    g_signal_connect(w->settings_btn, "clicked", G_CALLBACK(onSettings), w);
    gtk_box_pack_start(GTK_BOX(box), w->settings_btn, FALSE, FALSE, 0);
    return box;
}

static GtkWidget* createPasswordPage(AppWidgets* w) {
    GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
    gtk_widget_set_halign(box, GTK_ALIGN_CENTER); gtk_widget_set_valign(box, GTK_ALIGN_CENTER);
    gtk_container_set_border_width(GTK_CONTAINER(box), 40);
    GtkWidget* title = gtk_label_new(NULL);
    gtk_label_set_markup(GTK_LABEL(title), "<span size='x-large' weight='bold'>Set Password</span>");
    gtk_box_pack_start(GTK_BOX(box), title, FALSE, FALSE, 0);
    GtkWidget* desc = gtk_label_new("Min 8 characters. Write it down safely.");
    gtk_box_pack_start(GTK_BOX(box), desc, FALSE, FALSE, 5);
    w->pw_entry = gtk_entry_new(); gtk_entry_set_visibility(GTK_ENTRY(w->pw_entry), FALSE);
    gtk_widget_set_size_request(w->pw_entry, 300, -1); gtk_box_pack_start(GTK_BOX(box), w->pw_entry, FALSE, FALSE, 0);
    w->pw_confirm = gtk_entry_new(); gtk_entry_set_visibility(GTK_ENTRY(w->pw_confirm), FALSE);
    gtk_widget_set_size_request(w->pw_confirm, 300, -1); gtk_box_pack_start(GTK_BOX(box), w->pw_confirm, FALSE, FALSE, 0);
    w->pw_set_btn = gtk_button_new_with_label("Set Password");
    g_signal_connect(w->pw_set_btn, "clicked", G_CALLBACK(onPwSet), w);
    gtk_box_pack_start(GTK_BOX(box), w->pw_set_btn, FALSE, FALSE, 10);
    w->pw_back_btn = gtk_button_new_with_label("Back");
    g_signal_connect(w->pw_back_btn, "clicked", G_CALLBACK(onPwBack), w);
    gtk_box_pack_start(GTK_BOX(box), w->pw_back_btn, FALSE, FALSE, 0);
    return box;
}

static GtkWidget* createPuzzlePage(AppWidgets* w) {
    GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 8);
    gtk_container_set_border_width(GTK_CONTAINER(box), 12);
    w->puzzle_progress_label = gtk_label_new("Challenge 1 / 10");
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_progress_label, FALSE, FALSE, 0);
    w->board_drawing = gtk_drawing_area_new();
    gtk_widget_set_size_request(w->board_drawing, -1, 300);
    g_signal_connect(w->board_drawing, "draw", G_CALLBACK(on_board_draw), NULL);
    gtk_box_pack_start(GTK_BOX(box), w->board_drawing, TRUE, TRUE, 0);
    w->puzzle_msg = gtk_label_new(NULL);
    gtk_label_set_markup(GTK_LABEL(w->puzzle_msg), "<span foreground='#FFD93D'>Program the bot to collect all targets</span>");
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_msg, FALSE, FALSE, 0);

    // F1 slots
    GtkWidget* f1_label = gtk_label_new("F1 (main)");
    gtk_box_pack_start(GTK_BOX(box), f1_label, FALSE, FALSE, 0);
    GtkWidget* f1_row = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 2);
    gtk_widget_set_halign(f1_row, GTK_ALIGN_CENTER);
    for (int i = 0; i < SLOTS; i++) {
        w->f1_btns[i] = gtk_button_new_with_label("\u2014");
        gtk_widget_set_size_request(w->f1_btns[i], 40, 40);
        g_signal_connect(w->f1_btns[i], "clicked", G_CALLBACK(onSlotClicked), GINT_TO_POINTER(i));
        gtk_box_pack_start(GTK_BOX(f1_row), w->f1_btns[i], FALSE, FALSE, 2);
    }
    gtk_box_pack_start(GTK_BOX(box), f1_row, FALSE, FALSE, 0);

    // Control buttons
    GtkWidget* ctrl = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_widget_set_halign(ctrl, GTK_ALIGN_CENTER);
    w->run_btn = gtk_button_new_with_label("  RUN \u25B6  ");
    g_signal_connect(w->run_btn, "clicked", G_CALLBACK(on_run_clicked), w);
    gtk_box_pack_start(GTK_BOX(ctrl), w->run_btn, FALSE, FALSE, 0);
    w->reset_btn = gtk_button_new_with_label("  RESET  ");
    g_signal_connect(w->reset_btn, "clicked", G_CALLBACK(on_reset_clicked), w);
    gtk_box_pack_start(GTK_BOX(ctrl), w->reset_btn, FALSE, FALSE, 0);
    gtk_box_pack_start(GTK_BOX(box), ctrl, FALSE, FALSE, 8);

    w->puzzle_back_btn = gtk_button_new_with_label("Cancel");
    g_signal_connect(w->puzzle_back_btn, "clicked", G_CALLBACK(onPuzzleBack), w);
    gtk_box_pack_start(GTK_BOX(box), w->puzzle_back_btn, FALSE, FALSE, 0);
    return box;
}

static const char* CSS = "window { background-color: #0F0F23; }"
    "* { font-family: sans-serif; }"
    "label { color: #C9D1D9; }"
    "button { padding: 8px 16px; border-radius: 6px; background-color: #21262D; color: white; font-weight: bold; }"
    "button:hover { background-color: #30363D; }"
    "entry { padding: 8px; border-radius: 6px; background-color: #161B22; color: white; }";

int main(int argc, char* argv[]) {
    if (geteuid() != 0) { fprintf(stderr, "Must be run as root: sudo pblock\n"); return 1; }

    gtk_init(&argc, &argv);
    GtkCssProvider* css = gtk_css_provider_new();
    gtk_css_provider_load_from_data(css, CSS, -1, NULL);
    gtk_style_context_add_provider_for_screen(gdk_screen_get_default(),
        GTK_STYLE_PROVIDER(css), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);

    g_app = new AppWidgets();
    std::random_device rd; g_app->rng = std::mt19937(rd());

    g_app->window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(g_app->window), "PBLOCK - Family Safety");
    gtk_window_set_default_size(GTK_WINDOW(g_app->window), 480, 650);
    gtk_window_set_position(GTK_WINDOW(g_app->window), GTK_WIN_POS_CENTER);
    g_signal_connect(g_app->window, "destroy", G_CALLBACK(gtk_main_quit), NULL);

    g_app->stack = gtk_stack_new();
    gtk_stack_set_transition_type(GTK_STACK(g_app->stack), GTK_STACK_TRANSITION_TYPE_SLIDE_LEFT_RIGHT);
    gtk_stack_set_transition_duration(GTK_STACK(g_app->stack), 300);
    gtk_container_add(GTK_CONTAINER(g_app->window), g_app->stack);

    gtk_stack_add_named(GTK_STACK(g_app->stack), createStatusPage(g_app), "status");
    gtk_stack_add_named(GTK_STACK(g_app->stack), createPasswordPage(g_app), "password");
    gtk_stack_add_named(GTK_STACK(g_app->stack), createPuzzlePage(g_app), "puzzle");

    gtk_stack_set_visible_child_name(GTK_STACK(g_app->stack), "status");
    updateStatus(g_app);

    gtk_widget_show_all(g_app->window);
    gtk_main();
    delete g_app;
    return 0;
}
