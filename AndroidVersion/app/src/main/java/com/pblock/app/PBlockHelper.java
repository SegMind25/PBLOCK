package com.pblock.app;

import java.util.Arrays;
import java.util.List;

public class PBlockHelper {

    private static final List<String> BLOCKED_DOMAINS = Arrays.asList(
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
        "sxyprn.com", "ok.xxx",
        "pornzog.com", "upornia.com", "hdporncomics.com",
        "vqporn.com", "vrporn.com", "sexlikereal.com",
        "pornkai.com", "fapcat.com", "ashemaletube.com",
        "shemale.com", "zbporn.com",
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
    );

    private static final String GOOGLE_SAFESEARCH_IP = "216.239.38.120";
    private static final String BING_STRICT_IP = "204.79.197.220";

    private static final List<String> GOOGLE_DOMAINS = Arrays.asList(
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
    );

    private static final List<String> BING_DOMAINS = Arrays.asList(
        "bing.com", "www.bing.com",
        "bing.co.uk", "www.bing.co.uk",
        "bing.de", "www.bing.de",
        "bing.fr", "www.bing.fr"
    );

    private static final List<String> BLOCKED_SEARCH_ENGINES = Arrays.asList(
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
    );

    public static final String BLOCK_START_MARKER = "# CONTENT_BLOCKER_START";
    public static final String BLOCK_END_MARKER = "# CONTENT_BLOCKER_END";

    public static int getBlockedDomainCount() {
        return BLOCKED_DOMAINS.size() + GOOGLE_DOMAINS.size()
            + BING_DOMAINS.size() + BLOCKED_SEARCH_ENGINES.size();
    }

    public static String generateBlockEntries() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(BLOCK_START_MARKER).append(" - Do not edit this section\n");

        for (String domain : BLOCKED_DOMAINS) {
            sb.append("127.0.0.1 ").append(domain).append("\n");
            sb.append("127.0.0.1 www.").append(domain).append("\n");
            sb.append("::1 ").append(domain).append("\n");
            sb.append("::1 www.").append(domain).append("\n");
        }

        sb.append("# Google SafeSearch enforcement\n");
        for (String domain : GOOGLE_DOMAINS) {
            sb.append(GOOGLE_SAFESEARCH_IP).append(" ").append(domain).append("\n");
        }

        sb.append("# Bing strict SafeSearch enforcement\n");
        for (String domain : BING_DOMAINS) {
            sb.append(BING_STRICT_IP).append(" ").append(domain).append("\n");
        }

        sb.append("# Block other search engines (no SafeSearch support)\n");
        for (String domain : BLOCKED_SEARCH_ENGINES) {
            sb.append("127.0.0.1 ").append(domain).append("\n");
            sb.append("::1 ").append(domain).append("\n");
        }

        sb.append(BLOCK_END_MARKER).append("\n");
        return sb.toString();
    }
}
