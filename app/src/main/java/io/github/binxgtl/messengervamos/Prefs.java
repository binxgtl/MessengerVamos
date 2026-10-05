package io.github.binxgtl.messengervamos;

import android.content.SharedPreferences;

final class Prefs {
    private final SharedPreferences prefs;

    Prefs(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    boolean getBoolean(String key, boolean defValue) {
        try {
            return prefs == null ? defValue : prefs.getBoolean(key, defValue);
        } catch (Throwable ignored) {
            return defValue;
        }
    }

    int getInt(String key, int defValue) {
        try {
            return prefs == null ? defValue : prefs.getInt(key, defValue);
        } catch (Throwable ignored) {
            return defValue;
        }
    }

    boolean noSeen() { return getBoolean(Config.KEY_NO_SEEN, Config.DEF_NO_SEEN); }
    boolean noTyping() { return getBoolean(Config.KEY_NO_TYPING, Config.DEF_NO_TYPING); }
    boolean strictVersion() { return getBoolean(Config.KEY_STRICT_VERSION, Config.DEF_STRICT_VERSION); }
    boolean diagnostics() { return getBoolean(Config.KEY_DIAGNOSTICS, Config.DEF_DIAGNOSTICS); }
    boolean messageObserver() { return getBoolean(Config.KEY_MESSAGE_OBSERVER, Config.DEF_MESSAGE_OBSERVER); }
    boolean cacheMessageText() { return getBoolean(Config.KEY_CACHE_MESSAGE_TEXT, Config.DEF_CACHE_MESSAGE_TEXT); }
    int seenApiId() { return getInt(Config.KEY_SEEN_API_ID, Config.DEF_SEEN_API_ID); }
}
