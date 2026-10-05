package io.github.binxgtl.messengervamos;

import android.content.Context;
import java.lang.reflect.Method;

final class AppContext {
    private AppContext() {}

    static Context currentApplication() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Method m = at.getDeclaredMethod("currentApplication");
            m.setAccessible(true);
            Object app = m.invoke(null);
            return app instanceof Context ? (Context) app : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
