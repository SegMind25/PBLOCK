#include <jni.h>
#include <string>
#include <vector>
#include <sstream>

// Comprehensive list of adult domains to block (redirected to 127.0.0.1)
static const std::vector<std::string> BLOCKED_DOMAINS = {
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
static const char* GOOGLE_SAFESEARCH_IP = "216.239.38.120";
// Bing strict mode IP - forces strict SafeSearch on Bing
static const char* BING_STRICT_IP = "204.79.197.220";

// Google domains to redirect to SafeSearch VIP
static const std::vector<std::string> GOOGLE_DOMAINS = {
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
static const std::vector<std::string> BING_DOMAINS = {
    "bing.com", "www.bing.com",
    "bing.co.uk", "www.bing.co.uk",
    "bing.de", "www.bing.de",
    "bing.fr", "www.bing.fr"
};

// Search engines that don't support forced SafeSearch via DNS - block entirely
static const std::vector<std::string> BLOCKED_SEARCH_ENGINES = {
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

static const char* BLOCK_START_MARKER = "# CONTENT_BLOCKER_START";
static const char* BLOCK_END_MARKER = "# CONTENT_BLOCKER_END";

/**
 * Returns the list of blocked domains as a newline-separated string.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_pblock_app_MainActivity_getBlockedDomainsNative(JNIEnv *env, jobject /* this */) {
    std::ostringstream oss;
    for (size_t i = 0; i < BLOCKED_DOMAINS.size(); i++) {
        oss << BLOCKED_DOMAINS[i];
        if (i + 1 < BLOCKED_DOMAINS.size()) {
            oss << "\n";
        }
    }
    return env->NewStringUTF(oss.str().c_str());
}

/**
 * Returns the total number of blocked/enforced entries.
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_pblock_app_MainActivity_getBlockedDomainCountNative(JNIEnv *env, jobject /* this */) {
    return static_cast<jint>(BLOCKED_DOMAINS.size() + GOOGLE_DOMAINS.size() +
                             BING_DOMAINS.size() + BLOCKED_SEARCH_ENGINES.size());
}

/**
 * Generates hosts file block entries for all blocked domains,
 * SafeSearch enforcement for Google/Bing, and blocks other search engines.
 * Returns the text to append to the hosts file.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_pblock_app_MainActivity_generateBlockEntriesNative(JNIEnv *env, jobject /* this */) {
    std::ostringstream oss;
    oss << "\n" << BLOCK_START_MARKER << " - Do not edit this section\n";

    // Block adult domains (redirect to 127.0.0.1)
    for (const auto& domain : BLOCKED_DOMAINS) {
        oss << "127.0.0.1 " << domain << "\n";
        oss << "127.0.0.1 www." << domain << "\n";
        oss << "::1 " << domain << "\n";
        oss << "::1 www." << domain << "\n";
    }

    // Force Google SafeSearch (redirect to SafeSearch VIP)
    oss << "# Google SafeSearch enforcement\n";
    for (const auto& domain : GOOGLE_DOMAINS) {
        oss << GOOGLE_SAFESEARCH_IP << " " << domain << "\n";
    }

    // Force Bing strict SafeSearch
    oss << "# Bing strict SafeSearch enforcement\n";
    for (const auto& domain : BING_DOMAINS) {
        oss << BING_STRICT_IP << " " << domain << "\n";
    }

    // Block search engines without SafeSearch support
    oss << "# Block other search engines (no SafeSearch support)\n";
    for (const auto& domain : BLOCKED_SEARCH_ENGINES) {
        oss << "127.0.0.1 " << domain << "\n";
        oss << "::1 " << domain << "\n";
    }

    oss << BLOCK_END_MARKER << "\n";
    return env->NewStringUTF(oss.str().c_str());
}

/**
 * Returns the start marker string used in hosts file.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_pblock_app_MainActivity_getStartMarkerNative(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF(BLOCK_START_MARKER);
}

/**
 * Returns the end marker string used in hosts file.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_pblock_app_MainActivity_getEndMarkerNative(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF(BLOCK_END_MARKER);
}

/**
 * Returns app version info string.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_pblock_app_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    std::ostringstream oss;
    oss << "PBLOCK Content Blocker v2.0\n";
    oss << "Adult domains blocked: " << BLOCKED_DOMAINS.size() << "\n";
    oss << "Google SafeSearch enforced: " << GOOGLE_DOMAINS.size() << " domains\n";
    oss << "Bing strict mode: " << BING_DOMAINS.size() << " domains\n";
    oss << "Other search engines blocked: " << BLOCKED_SEARCH_ENGINES.size();
    return env->NewStringUTF(oss.str().c_str());
}
