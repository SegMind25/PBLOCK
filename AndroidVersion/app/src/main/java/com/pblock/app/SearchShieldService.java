package com.pblock.app;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Search Shield: watches what is typed into browsers / the Google app. When an adult
 * search term appears (in the search box or in the URL, e.g. google.com/search?q=...),
 * it backs out of the page and shows {@link StopActivity}.
 *
 * <p>Kept light on battery: the system only delivers events from the browser packages
 * listed in res/xml/search_shield_config.xml, and window scans are throttled.
 */
public class SearchShieldService extends AccessibilityService {

    private static final long SCAN_THROTTLE_MS = 400;
    private static final long TRIGGER_COOLDOWN_MS = 3000;
    private static final int MAX_NODES = 150;

    // Whole-word matches only, so words like "Essex" or "sextant" are not caught.
    private static final Pattern BAD = Pattern.compile(
        "(?<![\\p{L}\\d])("
            + "porn\\w*|p0rn\\w*|pr0n|xxx+|nsfw|hentai|rule ?34|onlyfans|fansly"
            + "|xvideos?|xnxx|xhamster|pornhub|redtube|youporn|brazzers|chaturbate|stripchat"
            + "|nudes?|naked|nudity|boobs?|tits|titties|pussy|blowjob|handjob|milfs?|camgirls?"
            + "|erotic\\w*|sexting|sex ?(video|videos|movie|movies|tape|cam|chat|clip|clips|pics?)"
            + "|(hot|free|live|real) ?sex|anal ?sex|lesbian ?sex|strip ?tease"
            + "|سكس|نيك|افلام ?سكس|بورن"
            + ")(?![\\p{L}\\d])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private long lastScan;
    private long lastTrigger;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastTrigger < TRIGGER_COOLDOWN_MS) return;

        // 1. Cheap check: text carried by the event itself (typing in a search box).
        List<CharSequence> texts = event.getText();
        if (texts != null) {
            for (CharSequence t : texts) {
                if (isBad(t)) { trigger(now); return; }
            }
        }

        // 2. Throttled scan of editable fields / URL bar in the current window.
        if (now - lastScan < SCAN_THROTTLE_MS) return;
        lastScan = now;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        if (scan(root, new int[]{0})) {
            trigger(now);
        }
    }

    private boolean scan(AccessibilityNodeInfo node, int[] count) {
        if (node == null || count[0]++ > MAX_NODES) return false;
        CharSequence cls = node.getClassName();
        String id = node.getViewIdResourceName();
        boolean field = node.isEditable()
            || (cls != null && cls.toString().endsWith("EditText"))
            || (id != null && (id.contains("url") || id.contains("search")
                || id.contains("location_bar") || id.contains("omnibox")));
        if (field && isBad(node.getText())) {
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && scan(child, count)) return true;
        }
        return false;
    }

    static boolean isBad(CharSequence raw) {
        if (TextUtils.isEmpty(raw)) return false;
        String s = raw.toString();
        if (s.length() > 2000) s = s.substring(0, 2000);
        try {
            // URLs like ...search?q=free+porn  or  %70orn
            if (s.contains("%") || s.contains("+")) {
                s = s + " " + URLDecoder.decode(s.replace("+", " "), "UTF-8");
            }
        } catch (Exception ignored) {
        }
        s = s.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        return BAD.matcher(s).find();
    }

    private void trigger(long now) {
        lastTrigger = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
        Intent stop = new Intent(this, StopActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startActivity(stop);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {
    }

    /** True if the user has switched Search Shield on in Accessibility settings. */
    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(context, SearchShieldService.class);
        for (String s : enabled.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(s);
            if (me.equals(c)) return true;
        }
        return false;
    }
}
