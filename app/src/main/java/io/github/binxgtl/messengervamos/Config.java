package io.github.binxgtl.messengervamos;

final class Config {
    static final String TARGET_PACKAGE = "com.facebook.orca";
    static final String TARGET_VERSION = "581.0.0.49.91";
    static final String PREF_GROUP = "messengervamos";

    static final String KEY_NO_SEEN = "no_seen";
    static final String KEY_NO_TYPING = "no_typing";
    static final String KEY_STRICT_VERSION = "strict_version";
    static final String KEY_DIAGNOSTICS = "diagnostics";
    static final String KEY_MESSAGE_OBSERVER = "message_observer";
    static final String KEY_CACHE_MESSAGE_TEXT = "cache_message_text";
    static final String KEY_SEEN_API_ID = "seen_api_id";

    static final boolean DEF_NO_SEEN = true;
    static final boolean DEF_NO_TYPING = true;
    static final boolean DEF_STRICT_VERSION = true;
    static final boolean DEF_DIAGNOSTICS = true;
    static final boolean DEF_MESSAGE_OBSERVER = false;
    static final boolean DEF_CACHE_MESSAGE_TEXT = false;
    static final int DEF_SEEN_API_ID = -1;

    private Config() {}
}
