package io.github.binxgtl.messengervamos;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class VamosApp extends Application implements XposedServiceHelper.OnServiceListener {
    private static volatile XposedService service;
    private static volatile SharedPreferences remotePrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
        ensureLocalDefaults(this);
    }

    @Override
    public void onServiceBind(XposedService bound) {
        service = bound;
        try {
            remotePrefs = bound.getRemotePreferences(Config.PREF_GROUP);
            syncLocalToRemote(this, remotePrefs);
        } catch (Throwable ignored) {
            remotePrefs = null;
        }
    }

    @Override
    public void onServiceDied(XposedService dead) {
        if (service == dead) {
            service = null;
            remotePrefs = null;
        }
    }

    static boolean isServiceReady() {
        return remotePrefs != null;
    }

    static SharedPreferences readPrefs(Context context) {
        SharedPreferences remote = remotePrefs;
        return remote != null ? remote : local(context);
    }

    static void putBoolean(Context context, String key, boolean value) {
        local(context).edit().putBoolean(key, value).apply();
        SharedPreferences remote = remotePrefs;
        if (remote != null) remote.edit().putBoolean(key, value).apply();
    }

    static void putInt(Context context, String key, int value) {
        local(context).edit().putInt(key, value).apply();
        SharedPreferences remote = remotePrefs;
        if (remote != null) remote.edit().putInt(key, value).apply();
    }

    static void resetDefaults(Context context) {
        SharedPreferences local = local(context);
        local.edit().clear().apply();
        ensureLocalDefaults(context);
        SharedPreferences remote = remotePrefs;
        if (remote != null) {
            remote.edit().clear().apply();
            syncLocalToRemote(context, remote);
        }
    }

    private static SharedPreferences local(Context context) {
        return context.getSharedPreferences(Config.PREF_GROUP, Context.MODE_PRIVATE);
    }

    private static void ensureLocalDefaults(Context context) {
        SharedPreferences p = local(context);
        SharedPreferences.Editor e = p.edit();
        if (!p.contains(Config.KEY_NO_SEEN)) e.putBoolean(Config.KEY_NO_SEEN, Config.DEF_NO_SEEN);
        if (!p.contains(Config.KEY_NO_TYPING)) e.putBoolean(Config.KEY_NO_TYPING, Config.DEF_NO_TYPING);
        if (!p.contains(Config.KEY_STRICT_VERSION)) e.putBoolean(Config.KEY_STRICT_VERSION, Config.DEF_STRICT_VERSION);
        if (!p.contains(Config.KEY_DIAGNOSTICS)) e.putBoolean(Config.KEY_DIAGNOSTICS, Config.DEF_DIAGNOSTICS);
        if (!p.contains(Config.KEY_MESSAGE_OBSERVER)) e.putBoolean(Config.KEY_MESSAGE_OBSERVER, Config.DEF_MESSAGE_OBSERVER);
        if (!p.contains(Config.KEY_CACHE_MESSAGE_TEXT)) e.putBoolean(Config.KEY_CACHE_MESSAGE_TEXT, Config.DEF_CACHE_MESSAGE_TEXT);
        if (!p.contains(Config.KEY_SEEN_API_ID)) e.putInt(Config.KEY_SEEN_API_ID, Config.DEF_SEEN_API_ID);
        e.apply();
    }

    @SuppressWarnings("unchecked")
    private static void syncLocalToRemote(Context context, SharedPreferences remote) {
        SharedPreferences src = local(context);
        SharedPreferences.Editor out = remote.edit();
        for (Map.Entry<String, ?> entry : src.getAll().entrySet()) {
            Object v = entry.getValue();
            String k = entry.getKey();
            if (v instanceof Boolean) out.putBoolean(k, (Boolean) v);
            else if (v instanceof Integer) out.putInt(k, (Integer) v);
            else if (v instanceof Long) out.putLong(k, (Long) v);
            else if (v instanceof Float) out.putFloat(k, (Float) v);
            else if (v instanceof String) out.putString(k, (String) v);
            else if (v instanceof Set) out.putStringSet(k, (Set<String>) v);
        }
        out.apply();
    }
}
