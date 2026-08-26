#include <windows.h>
#include <commctrl.h>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <random>
#include <algorithm>
#include <cmath>
#include <crypt.h>

#pragma comment(lib, "comctl32.lib")
#pragma comment(linker,"\"/manifestdependency:type='win32' name='Microsoft.Windows.Common-Controls' version='6.0.0.0' processorArchitecture='*' publicKeyToken='6595b64144ccf1df' language='*'\"")

static const char* APP_TITLE = "PBLOCK - Family Safety";
static const int WIN_W = 600, WIN_H = 780;

static const std::string HOSTS_FILE = "C:\\Windows\\System32\\drivers\\etc\\hosts";
static const std::string CONFIG_FILE = "C:\\ProgramData\\pblock.conf";
static const std::string BACKUP_FILE = "C:\\ProgramData\\pblock_hosts_backup";
static const int TOTAL_PUZZLES = 10;
static const int SLOTS = 12;
static const int MAX_STEPS = 50;
static const int STEP_MS = 200;

#define CLR_BG       RGB(15, 15, 35)
#define CLR_CARD     RGB(26, 26, 46)
#define CLR_ACCENT   RGB(79, 140, 255)
#define CLR_GREEN    RGB(46, 213, 115)
#define CLR_RED      RGB(255, 71, 87)
#define CLR_YELLOW   RGB(255, 217, 61)
#define CLR_PURPLE   RGB(168, 85, 247)
#define CLR_BLUE     RGB(55, 66, 250)
#define CLR_TEXT     RGB(201, 209, 217)
#define CLR_DIM      RGB(136, 146, 176)

static const int TILE_COLORS[] = { 0, 0xFF4757, 0x2ED573, 0x3742FA, 0xFFA502, 0xA855F7 };

struct Cmd {
    bool isFunc = false;
    int func = 0, dir = 0, guard = 0;
    bool isTurnLeft = false, isTurnRight = false, isJump = false;
    void clear() { isFunc = false; func = dir = guard = 0; isTurnLeft = isTurnRight = isJump = false; }
    bool empty() const { return !isFunc && dir == 0 && guard == 0 && !isTurnLeft && !isTurnRight && !isJump; }
    const char* label() const {
        if (isTurnLeft) return "\xC4\xB1"; // turn left arrow placeholder
        if (isTurnRight) return "\xC4\xB2";
        if (isJump) return "JP";
        if (isFunc) { static char buf[4]; sprintf_s(buf, "F%d", func); return buf; }
        static const char* arrows[] = { "UP", "DN", "LT", "RT" };
        return arrows[dir];
    }
    COLORREF color() const {
        if (isTurnLeft || isTurnRight) return RGB(255, 99, 72);
        if (isJump) return CLR_PURPLE;
        if (isFunc) return RGB(124, 58, 237);
        return guard >= 0 && guard <= 5 ? TILE_COLORS[guard] : CLR_BLUE;
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

static Cmd f1[SLOTS];
static Puzzle g_puzzle;
static int g_puzzleCurrent = 0;
static bool g_running = false;
static std::mt19937 g_rng;
static HWND g_hWnd = NULL;
static HWND g_hBoard = NULL;
static HWND g_hMsg = NULL;
static HWND g_hProgress = NULL;
static HWND g_hF1Btns[SLOTS] = {};
static HWND g_hRunBtn = NULL, g_hResetBtn = NULL;
static HWND g_hStatusIcon = NULL, g_hStatusLabel = NULL;
static HWND g_hEnableBtn = NULL, g_hDisableBtn = NULL;
static HWND g_hPwEntry = NULL, g_hPwConfirm = NULL, g_hPwSetBtn = NULL;
static HWND g_hStack[3] = {};
static int g_currentPage = 0;
static int g_slotMode[SLOTS * 2] = {};

// Domain lists
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
    "yourlust.com", "yuvutu.com", "zedporn.com"
};

static const std::string GOOGLE_SAFESEARCH_IP = "216.239.38.120";
static const std::string BING_STRICT_IP = "204.79.197.220";

static std::vector<std::string> google_domains = {
    "google.com", "www.google.com", "google.co.uk", "www.google.co.uk",
    "google.fr", "www.google.fr", "google.de", "www.google.de",
    "google.es", "www.google.es", "google.it", "www.google.it",
    "google.ca", "www.google.ca", "google.com.au", "www.google.com.au",
    "google.co.in", "www.google.co.in", "google.com.br", "www.google.com.br",
    "google.co.jp", "www.google.co.jp", "google.ru", "www.google.ru",
    "google.com.mx", "www.google.com.mx", "google.nl", "www.google.nl",
    "google.be", "www.google.be", "google.pt", "www.google.pt",
    "google.co.za", "www.google.co.za", "google.com.ar", "www.google.com.ar",
    "google.com.eg", "www.google.com.eg", "google.com.sa", "www.google.com.sa",
    "google.ae", "www.google.ae", "google.co.ma", "www.google.co.ma",
    "google.dz", "www.google.dz", "google.com.tr", "www.google.com.tr",
    "google.pl", "www.google.pl", "google.se", "www.google.se",
    "google.no", "www.google.no", "google.dk", "www.google.dk",
    "google.fi", "www.google.fi", "google.at", "www.google.at",
    "google.ch", "www.google.ch"
};

static std::vector<std::string> bing_domains = {
    "bing.com", "www.bing.com", "bing.co.uk", "www.bing.co.uk",
    "bing.de", "www.bing.de", "bing.fr", "www.bing.fr"
};

static std::vector<std::string> blocked_search_engines = {
    "duckduckgo.com", "www.duckduckgo.com",
    "yandex.com", "www.yandex.com", "yandex.ru", "www.yandex.ru",
    "ask.com", "www.ask.com", "baidu.com", "www.baidu.com",
    "ecosia.org", "www.ecosia.org", "qwant.com", "www.qwant.com",
    "startpage.com", "www.startpage.com", "swisscows.com", "www.swisscows.com",
    "gibiru.com", "www.gibiru.com", "searx.me", "www.searx.me"
};

static std::string loadPasswordHash() {
    std::ifstream f(CONFIG_FILE);
    std::string h;
    if (f.is_open()) { std::getline(f, h); f.close(); }
    return h;
}

static void savePasswordHash(const std::string& h) {
    std::ofstream f(CONFIG_FILE);
    if (f.is_open()) { f << h << std::endl; f.close(); }
}

static bool isBlocked() {
    std::ifstream f(HOSTS_FILE);
    std::string line;
    while (std::getline(f, line))
        if (line.find("# PBLOCK_START") != std::string::npos) return true;
    return false;
}

static void backupHosts() {
    std::ifstream src(HOSTS_FILE, std::ios::binary);
    if (src.is_open()) {
        std::ofstream dst(BACKUP_FILE, std::ios::binary);
        dst << src.rdbuf();
    }
}

static void blockContent() {
    if (isBlocked()) return;
    backupHosts();
    std::ofstream hosts(HOSTS_FILE, std::ios::app);
    if (!hosts.is_open()) return;
    hosts << "\n# PBLOCK_START - Do not edit this section\n";
    for (auto& d : blocked_domains) {
        hosts << "127.0.0.1 " << d << "\n127.0.0.1 www." << d << "\n::1 " << d << "\n::1 www." << d << "\n";
    }
    hosts << "# Google SafeSearch\n";
    for (auto& d : google_domains) hosts << GOOGLE_SAFESEARCH_IP << " " << d << "\n";
    hosts << "# Bing strict SafeSearch\n";
    for (auto& d : bing_domains) hosts << BING_STRICT_IP << " " << d << "\n";
    hosts << "# Block other search engines\n";
    for (auto& d : blocked_search_engines) hosts << "127.0.0.1 " << d << "\n::1 " << d << "\n";
    hosts << "# PBLOCK_END\n";
    hosts.close();
    system("ipconfig /flushdns > nul");
}

static void unblockContent() {
    std::ifstream f(HOSTS_FILE);
    std::vector<std::string> lines;
    std::string line;
    bool skip = false;
    while (std::getline(f, line)) {
        if (line.find("# PBLOCK_START") != std::string::npos) { skip = true; continue; }
        if (line.find("# PBLOCK_END") != std::string::npos) { skip = false; continue; }
        if (!skip) lines.push_back(line);
    }
    f.close();
    std::ofstream out(HOSTS_FILE);
    for (auto& l : lines) out << l << "\n";
    out.close();
    system("ipconfig /flushdns > nul");
}

// Puzzle generation
static int countCells(Puzzle& p) {
    int n = 0;
    for (int y = 0; y < p.H; y++)
        for (int x = 0; x < p.W; x++)
            if (p.cells[y][x] != 0) n++;
    return n;
}

static int oppositeDir(int d) { return d == 0 ? 1 : d == 1 ? 0 : d == 2 ? 3 : 2; }
static const int DX[] = { 0, 0, -1, 1 };
static const int DY[] = { -1, 1, 0, 0 };

static void generatePuzzle(Puzzle& p, std::mt19937& rng, int idx) {
    int legs = 4 + idx * 2;
    for (int attempt = 0; attempt < 500; attempt++) {
        p.W = 7 + rng() % 5; p.H = 6 + rng() % 4;
        memset(p.cells, 0, sizeof(p.cells));
        memset(p.goals, 0, sizeof(p.goals));
        p.startX = 1 + rng() % (p.W - 2);
        p.startY = 1 + rng() % (p.H - 2);
        p.startDir = rng() % 4;
        int curX = p.startX, curY = p.startY, lastDir = -1;
        int prevColor = 1 + rng() % 5;
        p.cells[curY][curX] = prevColor;
        bool ok = true;
        for (int leg = 0; leg < legs && ok; leg++) {
            std::vector<int> dirs;
            for (int d = 0; d < 4; d++)
                if (d != lastDir && (lastDir == -1 || d != oppositeDir(lastDir)))
                    dirs.push_back(d);
            std::shuffle(dirs.begin(), dirs.end(), rng);
            int len = 2 + rng() % 5;
            int color;
            do { color = 1 + rng() % 5; } while (color == prevColor && rng() % 2);
            bool placed = false;
            for (int dir : dirs) {
                int tx = curX, ty = curY;
                bool fits = true;
                for (int i = 0; i < len; i++) {
                    tx += DX[dir]; ty += DY[dir];
                    if (tx < 0 || ty < 0 || tx >= p.W || ty >= p.H || p.cells[ty][tx] != 0) { fits = false; break; }
                }
                if (!fits) continue;
                tx = curX; ty = curY;
                for (int i = 0; i < len; i++) { tx += DX[dir]; ty += DY[dir]; p.cells[ty][tx] = color; }
                curX = tx; curY = ty; lastDir = dir; prevColor = color; placed = true; break;
            }
            if (!placed) ok = false;
        }
        if (!ok || countCells(p) < 10) continue;
        p.goals[curY][curX] = true;
        p.goalsTotal = 1;
        for (int y = 0; y < p.H; y++)
            for (int x = 0; x < p.W; x++)
                if (!p.goals[y][x] && p.cells[y][x] != 0 && rng() % 3 == 0) {
                    p.goals[y][x] = true;
                    p.goalsTotal++;
                }
        if (p.goalsTotal < 4) p.goalsTotal = 4;
        return;
    }
    p.W = 9; p.H = 7;
    memset(p.cells, 0, sizeof(p.cells));
    memset(p.goals, 0, sizeof(p.goals));
    for (int x = 0; x < p.W; x++) { p.cells[2][x] = 1; p.cells[4][x] = 2; }
    for (int y = 2; y < p.H; y++) { p.cells[y][3] = 3; p.cells[y][6] = 4; }
    p.startX = 0; p.startY = 2; p.startDir = 3;
    p.goals[2][p.W - 1] = true; p.goals[4][0] = true; p.goals[6][6] = true;
    p.goalsTotal = 3;
}

static void refreshSlotBtns() {
    for (int i = 0; i < SLOTS; i++) {
        if (!g_hF1Btns[i]) continue;
        Cmd& c = f1[i];
        SetWindowTextA(g_hF1Btns[i], c.empty() ? "--" : c.label());
    }
}

// Board drawing via WM_PAINT
static void paintBoard(HDC hdc, RECT rc) {
    HBRUSH bgBr = CreateSolidBrush(CLR_BG);
    FillRect(hdc, &rc, bgBr);
    DeleteObject(bgBr);

    Puzzle& p = g_puzzle;
    if (p.W == 0 || p.H == 0) return;

    int ww = rc.right - rc.left, wh = rc.bottom - rc.top;
    double tile = min((double)ww / p.W, (double)wh / p.H) * 0.88;
    double oX = (ww - tile * p.W) / 2.0 + rc.left;
    double oY = (wh - tile * p.H) / 2.0 + rc.top;

    // Grid
    HPEN gridPen = CreatePen(PS_SOLID, 1, RGB(26, 26, 62));
    HPEN oldPen = (HPEN)SelectObject(hdc, gridPen);
    for (int y = 0; y <= p.H; y++) {
        MoveToEx(hdc, (int)oX, (int)(oY + y * tile), NULL);
        LineTo(hdc, (int)(oX + p.W * tile), (int)(oY + y * tile));
    }
    for (int x = 0; x <= p.W; x++) {
        MoveToEx(hdc, (int)(oX + x * tile), (int)oY, NULL);
        LineTo(hdc, (int)(oX + x * tile), (int)(oY + p.H * tile));
    }
    SelectObject(hdc, oldPen);
    DeleteObject(gridPen);

    for (int y = 0; y < p.H; y++) {
        for (int x = 0; x < p.W; x++) {
            if (p.cells[y][x] == 0) continue;
            double left = oX + x * tile + tile * 0.06;
            double top = oY + y * tile + tile * 0.06;
            double sz = tile * 0.88;

            // Glow
            int tc = TILE_COLORS[p.cells[y][x]];
            HBRUSH glowBr = CreateSolidBrush(RGB((tc >> 16) & 0xFF, (tc >> 8) & 0xFF, tc & 0xFF));
            RECT glowRc = { (int)(left - tile * 0.03), (int)(top - tile * 0.03),
                           (int)(left + sz + tile * 0.03), (int)(top + sz + tile * 0.03) };
            FillRect(hdc, &glowRc, glowBr);
            DeleteObject(glowBr);

            // Tile
            HBRUSH tileBr = CreateSolidBrush(RGB((tc >> 16) & 0xFF, (tc >> 8) & 0xFF, tc & 0xFF));
            RECT tileRc = { (int)left, (int)top, (int)(left + sz), (int)(top + sz) };
            FillRect(hdc, &tileRc, tileBr);
            DeleteObject(tileBr);

            // Goal marker
            if (p.goals[y][x]) {
                bool got = p.goalsCollected > 0 && p.collected[y][x];
                HBRUSH goalBr = CreateSolidBrush(got ? RGB(80, 80, 80) : RGB(255, 255, 255));
                RECT goalRc = { (int)(left + sz * 0.3), (int)(top + sz * 0.3),
                               (int)(left + sz * 0.7), (int)(top + sz * 0.7) };
                FillRect(hdc, &goalRc, goalBr);
                DeleteObject(goalBr);
            }
        }
    }

    // Bot
    int bx = (int)(oX + p.botX * tile + tile * 0.5);
    int by = (int)(oY + p.botY * tile + tile * 0.5);
    HBRUSH botGlow = CreateSolidBrush(RGB(46, 213, 115));
    RECT botGlowRc = { bx - (int)(tile * 0.35), by - (int)(tile * 0.35),
                       bx + (int)(tile * 0.35), by + (int)(tile * 0.35) };
    FillRect(hdc, &botGlowRc, botGlow);
    DeleteObject(botGlow);
    HBRUSH botBr = CreateSolidBrush(RGB(255, 255, 255));
    Ellipse(hdc, bx - (int)(tile * 0.2), by - (int)(tile * 0.2),
            bx + (int)(tile * 0.2), by + (int)(tile * 0.2));
    DeleteObject(botBr);
}

// Run step timer
static UINT_PTR g_runTimer = 0;
static int g_runPC = 0;
static int g_runStepCount = 0;

static void CALLBACK runStepTimer(HWND, UINT, UINT_PTR, DWORD);

static void doRunStep() {
    if (!g_running) return;
    Puzzle& p = g_puzzle;

    while (g_runPC < SLOTS && f1[g_runPC].empty()) g_runPC++;
    if (g_runPC >= SLOTS) {
        g_running = false; g_runPC = 0; g_runStepCount = 0;
        SetWindowTextA(g_hMsg, "Program ended - targets not collected");
        KillTimer(g_hWnd, g_runTimer);
        return;
    }

    g_runStepCount++;
    if (g_runStepCount > MAX_STEPS) {
        g_running = false; g_runPC = 0; g_runStepCount = 0;
        SetWindowTextA(g_hMsg, "Step limit exceeded!");
        KillTimer(g_hWnd, g_runTimer);
        return;
    }

    Cmd& cmd = f1[g_runPC++];
    if (cmd.isTurnLeft) { p.botDir = (p.botDir + 3) % 4; InvalidateRect(g_hBoard, NULL, FALSE); return; }
    if (cmd.isTurnRight) { p.botDir = (p.botDir + 1) % 4; InvalidateRect(g_hBoard, NULL, FALSE); return; }
    if (cmd.isJump) {
        int nx = p.botX + DX[p.botDir] * 2, ny = p.botY + DY[p.botDir] * 2;
        if (nx < 0 || ny < 0 || nx >= p.W || ny >= p.H || p.cells[ny][nx] == 0) {
            g_running = false; g_runPC = 0; g_runStepCount = 0;
            SetWindowTextA(g_hMsg, "Jump crash!");
            KillTimer(g_hWnd, g_runTimer); return;
        }
        p.botX = nx; p.botY = ny;
        if (p.goals[ny][nx] && !p.collected[ny][nx]) { p.collected[ny][nx] = true; p.goalsCollected++; }
        InvalidateRect(g_hBoard, NULL, FALSE);
        if (p.goalsCollected >= p.goalsTotal) {
            g_running = false; g_runPC = 0; g_runStepCount = 0;
            g_puzzleCurrent++;
            if (g_puzzleCurrent >= TOTAL_PUZZLES) {
                SetWindowTextA(g_hMsg, "ALL 10 CHALLENGES SOLVED!");
            } else {
                SetWindowTextA(g_hMsg, "Challenge complete! Loading next...");
                KillTimer(g_hWnd, g_runTimer);
                SetTimer(g_hWnd, 1, 1500, [](HWND, UINT, UINT_PTR, DWORD) {
                    KillTimer(g_hWnd, 1);
                    generatePuzzle(g_puzzle, g_rng, g_puzzleCurrent);
                    for (int i = 0; i < SLOTS; i++) f1[i].clear();
                    g_puzzle.resetBot();
                    refreshSlotBtns();
                    InvalidateRect(g_hBoard, NULL, FALSE);
                    char buf[64];
                    sprintf_s(buf, "Challenge %d / %d", g_puzzleCurrent + 1, TOTAL_PUZZLES);
                    SetWindowTextA(g_hProgress, buf);
                    SetWindowTextA(g_hMsg, "Program the bot to collect all targets");
                });
            }
            return;
        }
        return;
    }

    if (cmd.guard != 0 && p.cells[p.botY][p.botX] != cmd.guard) return;

    int nx = p.botX + DX[cmd.dir], ny = p.botY + DY[cmd.dir];
    if (nx < 0 || ny < 0 || nx >= p.W || ny >= p.H || p.cells[ny][nx] == 0) {
        g_running = false; g_runPC = 0; g_runStepCount = 0;
        SetWindowTextA(g_hMsg, "CRASH!");
        KillTimer(g_hWnd, g_runTimer); return;
    }
    p.botX = nx; p.botY = ny; p.botDir = cmd.dir;
    if (p.goals[ny][nx] && !p.collected[ny][nx]) { p.collected[ny][nx] = true; p.goalsCollected++; }
    InvalidateRect(g_hBoard, NULL, FALSE);
    if (p.goalsCollected >= p.goalsTotal) {
        g_running = false; g_runPC = 0; g_runStepCount = 0;
        g_puzzleCurrent++;
        if (g_puzzleCurrent >= TOTAL_PUZZLES) {
            SetWindowTextA(g_hMsg, "ALL 10 CHALLENGES SOLVED!");
        } else {
            SetWindowTextA(g_hMsg, "Challenge complete! Loading next...");
            KillTimer(g_hWnd, g_runTimer);
            SetTimer(g_hWnd, 1, 1500, [](HWND, UINT, UINT_PTR, DWORD) {
                KillTimer(g_hWnd, 1);
                generatePuzzle(g_puzzle, g_rng, g_puzzleCurrent);
                for (int i = 0; i < SLOTS; i++) f1[i].clear();
                g_puzzle.resetBot();
                refreshSlotBtns();
                InvalidateRect(g_hBoard, NULL, FALSE);
                char buf[64];
                sprintf_s(buf, "Challenge %d / %d", g_puzzleCurrent + 1, TOTAL_PUZZLES);
                SetWindowTextA(g_hProgress, buf);
                SetWindowTextA(g_hMsg, "Program the bot to collect all targets");
            });
        }
        return;
    }
}

static void CALLBACK runStepTimer(HWND, UINT, UINT_PTR, DWORD) {
    if (g_running) doRunStep();
}

static void startRun() {
    if (g_running) return;
    bool empty = true;
    for (int i = 0; i < SLOTS; i++) if (!f1[i].empty()) { empty = false; break; }
    if (empty) { SetWindowTextA(g_hMsg, "F1 is empty!"); return; }
    g_running = true;
    g_runPC = 0;
    g_runStepCount = 0;
    g_puzzle.resetBot();
    InvalidateRect(g_hBoard, NULL, FALSE);
    SetWindowTextA(g_hMsg, "Running...");
    g_runTimer = SetTimer(g_hWnd, 2, STEP_MS, runStepTimer);
}

static void resetRun() {
    g_running = false;
    if (g_runTimer) KillTimer(g_hWnd, g_runTimer);
    g_runPC = 0;
    g_runStepCount = 0;
    g_puzzle.resetBot();
    InvalidateRect(g_hBoard, NULL, FALSE);
    SetWindowTextA(g_hMsg, "Program the bot to collect all targets");
}

// Page switching
static void showPage(int idx) {
    g_currentPage = idx;
    for (int i = 0; i < 3; i++) ShowWindow(g_hStack[i], i == idx ? SW_SHOW : SW_HIDE);
}

static void updateStatus() {
    bool blocked = isBlocked();
    bool hasPw = !loadPasswordHash().empty();
    SetWindowTextA(g_hStatusLabel, blocked ? "Protection Active" : "Protection Inactive");
    ShowWindow(g_hEnableBtn, blocked ? SW_HIDE : SW_SHOW);
    ShowWindow(g_hDisableBtn, blocked ? SW_SHOW : SW_HIDE);
}

static void onEnable() {
    if (!loadPasswordHash().empty()) blockContent();
    updateStatus();
}

static void onDisable() {
    g_puzzleCurrent = 0;
    generatePuzzle(g_puzzle, g_rng, 0);
    for (int i = 0; i < SLOTS; i++) f1[i].clear();
    g_puzzle.resetBot();
    refreshSlotBtns();
    InvalidateRect(g_hBoard, NULL, FALSE);
    char buf[64];
    sprintf_s(buf, "Challenge 1 / %d", TOTAL_PUZZLES);
    SetWindowTextA(g_hProgress, buf);
    SetWindowTextA(g_hMsg, "Program the bot to collect all targets");
    showPage(1);
}

static void onSlotClick(int idx) {
    g_slotMode[idx] = (g_slotMode[idx] + 1) % 5;
    f1[idx].clear();
    switch (g_slotMode[idx]) {
        case 1: f1[idx].dir = 0; break;
        case 2: f1[idx].dir = 3; break;
        case 3: f1[idx].isTurnRight = true; break;
        case 4: f1[idx].isFunc = true; f1[idx].func = 1; break;
    }
    refreshSlotBtns();
}

static LRESULT CALLBACK WndProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
    case WM_PAINT: {
        if (hWnd == g_hBoard) {
            PAINTSTRUCT ps;
            HDC hdc = BeginPaint(hWnd, &ps);
            RECT rc;
            GetClientRect(hWnd, &rc);
            paintBoard(hdc, rc);
            EndPaint(hWnd, &ps);
            return 0;
        }
        break;
    }
    case WM_COMMAND: {
        int id = LOWORD(wParam);
        if (id == 1001) onEnable();
        else if (id == 1002) onDisable();
        else if (id == 1010) startRun();
        else if (id == 1011) resetRun();
        else if (id == 1020) showPage(0); // status back
        else if (id >= 2000 && id < 2000 + SLOTS) onSlotClick(id - 2000);
        break;
    }
    case WM_DESTROY:
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProc(hWnd, msg, wParam, lParam);
}

static HWND createBtn(HWND parent, const char* text, int id, int x, int y, int w, int h) {
    return CreateWindowA("BUTTON", text, WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        x, y, w, h, parent, (HMENU)id, NULL, NULL);
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE, LPSTR, int) {
    INITCOMMONCONTROLSEX icc = { sizeof(icc), ICC_STANDARD_CLASSES };
    InitCommonControlsEx(&icc);

    std::random_device rd;
    g_rng = std::mt19937(rd());

    WNDCLASSEXA wc = {};
    wc.cbSize = sizeof(wc);
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hInstance;
    wc.hbrBackground = CreateSolidBrush(CLR_BG);
    wc.lpszClassName = "PBlockWnd";
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    RegisterClassExA(&wc);

    g_hWnd = CreateWindowExA(0, "PBlockWnd", APP_TITLE, WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU,
        CW_USEDEFAULT, CW_USEDEFAULT, WIN_W, WIN_H, NULL, NULL, hInstance, NULL);

    // --- Status page (g_hStack[0]) ---
    g_hStack[0] = CreateWindowA("STATIC", "", WS_CHILD, 0, 0, WIN_W, WIN_H, g_hWnd, NULL, NULL, NULL);
    g_hStatusLabel = CreateWindowA("STATIC", "Protection Inactive",
        WS_CHILD | SS_CENTER, 150, 200, 300, 40, g_hStack[0], NULL, NULL, NULL);
    HFONT hTitleFont = CreateFontA(28, 0, 0, 0, FW_BOLD, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH, "Segoe UI");
    SendMessageA(g_hStatusLabel, WM_SETFONT, (WPARAM)hTitleFont, TRUE);

    CreateWindowA("STATIC", "Parental Content Control & Family Safety",
        WS_CHILD | SS_CENTER, 100, 250, 400, 30, g_hStack[0], NULL, NULL, NULL);

    g_hEnableBtn = createBtn(g_hStack[0], "Enable Content Filter", 1001, 175, 320, 250, 50);
    g_hDisableBtn = createBtn(g_hStack[0], "Disable (solve 10 puzzles)", 1002, 175, 390, 250, 50);
    ShowWindow(g_hDisableBtn, SW_HIDE);

    // --- Puzzle page (g_hStack[1]) ---
    g_hStack[1] = CreateWindowA("STATIC", "", WS_CHILD, 0, 0, WIN_W, WIN_H, g_hWnd, NULL, NULL, NULL);
    g_hProgress = CreateWindowA("STATIC", "Challenge 1 / 10",
        WS_CHILD | SS_CENTER, 200, 10, 200, 25, g_hStack[1], NULL, NULL, NULL);
    g_hBoard = CreateWindowA("STATIC", "", WS_CHILD | SS_VISIBLE | SS_OWNERDRAW,
        20, 40, WIN_W - 40, 350, g_hStack[1], NULL, NULL, NULL);
    g_hMsg = CreateWindowA("STATIC", "Program the bot to collect all targets",
        WS_CHILD | SS_CENTER, 50, 400, 500, 30, g_hStack[1], NULL, NULL, NULL);

    // F1 slot buttons
    for (int i = 0; i < SLOTS; i++) {
        int col = i % 6, row = i / 6;
        g_hF1Btns[i] = createBtn(g_hStack[1], "--", 2000 + i,
            40 + col * 90, 440 + row * 50, 75, 40);
    }

    g_hRunBtn = createBtn(g_hStack[1], "RUN", 1010, 170, 560, 120, 45);
    g_hResetBtn = createBtn(g_hStack[1], "RESET", 1011, 310, 560, 120, 45);
    createBtn(g_hStack[1], "Cancel", 1020, 225, 620, 150, 35);

    // Show status page
    showPage(0);
    updateStatus();

    ShowWindow(g_hWnd, SW_SHOW);
    UpdateWindow(g_hWnd);

    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    return (int)msg.wParam;
}
