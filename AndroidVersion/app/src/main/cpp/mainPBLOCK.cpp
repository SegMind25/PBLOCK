#include <jni.h>
#include <string>
#include <vector>
#include <sstream>

// List of domains to block
static const std::vector<std::string> BLOCKED_DOMAINS = {
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
 * Returns the number of blocked domains.
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_pblock_app_MainActivity_getBlockedDomainCountNative(JNIEnv *env, jobject /* this */) {
    return static_cast<jint>(BLOCKED_DOMAINS.size());
}

/**
 * Generates hosts file block entries for all blocked domains.
 * Returns the text to append to the hosts file.
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_pblock_app_MainActivity_generateBlockEntriesNative(JNIEnv *env, jobject /* this */) {
    std::ostringstream oss;
    oss << "\n" << BLOCK_START_MARKER << " - Do not edit this section\n";
    for (const auto& domain : BLOCKED_DOMAINS) {
        oss << "127.0.0.1 " << domain << "\n";
        oss << "127.0.0.1 www." << domain << "\n";
        oss << "::1 " << domain << "\n";
        oss << "::1 www." << domain << "\n";
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
    oss << "PBLOCK Content Blocker v1.0\n";
    oss << "Domains in blocklist: " << BLOCKED_DOMAINS.size();
    return env->NewStringUTF(oss.str().c_str());
}
