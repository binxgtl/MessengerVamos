package io.github.binxgtl.messengervamos;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

final class HookInstaller {
    @FunctionalInterface
    private interface HookBody {
        Object invoke(XposedInterface.Chain chain) throws Throwable;
    }

    private final ModuleEntry module;
    private final ClassLoader classLoader;
    private final Prefs prefs;
    private final CompatProfile profile;
    private final String version;
    private final MessageCache messageCache = new MessageCache();
    private final Set<String> seenDiagnostics = new HashSet<>();
    private final Set<String> typingDiagnostics = new HashSet<>();
    private final Set<String> observerDiagnostics = new HashSet<>();
    private final Set<String> unsentDiagnostics = new HashSet<>();

    HookInstaller(ModuleEntry module, ClassLoader classLoader, Prefs prefs, CompatProfile profile, String version) {
        this.module = module;
        this.classLoader = classLoader;
        this.prefs = prefs;
        this.profile = profile;
        this.version = version;
    }

    void install() {
        boolean mutationAllowed = profile.exact || !prefs.strictVersion();
        FileLogger.i("M2 compatibility version=" + version + " exact=" + profile.exact + " strict=" + prefs.strictVersion());
        if (mutationAllowed) {
            installNoSeen();
            installNoTyping();
        } else {
            FileLogger.w("M2 mutation hooks skipped: version is not in compatibility database");
        }

        if (prefs.messageObserver()) installMessageObserver();
        else FileLogger.i("M3 message observer disabled by preference; enable it then restart Messenger to install");

        installUnsentSemanticProbe();
        FileLogger.i("M4 hook installation pass complete");
    }

    private void installNoSeen() {
        Class<?> cls = findClass(profile.sdkJni);
        if (cls == null) {
            FileLogger.w("M2 no-seen: class missing " + profile.sdkJni);
            return;
        }
        int installed = 0;
        for (Method method : cls.getDeclaredMethods()) {
            if (!contains(profile.seenMethods, method.getName())) continue;
            if (!isDispatchShape(method, 5, 12)) continue;
            String id = "no-seen/" + method.getName() + "/" + method.getParameterCount();
            if (tryHook(id, method, chain -> {
                List<Object> args = chain.getArgs();
                if (prefs.diagnostics()) logSeenCandidate(method, args);
                if (!prefs.noSeen()) return chain.proceed();
                if (matchesSeen(args)) {
                    int code = firstInt(args, -1);
                    FileLogger.runtimeMarker("BLOCK no-seen method=" + method.getName() + " api=" + code);
                    FileLogger.i("M2 blocked seen receipt api=" + code + " method=" + method.getName());
                    return null;
                }
                return chain.proceed();
            })) installed++;
        }
        FileLogger.i("M2 no-seen hooks installed=" + installed);
    }

    private void installNoTyping() {
        Class<?> cls = findClass(profile.typingJni);
        if (cls == null) {
            FileLogger.w("M2 no-typing: class missing " + profile.typingJni);
            return;
        }
        int installed = 0;
        for (Method method : cls.getDeclaredMethods()) {
            if (!method.getName().startsWith("dispatchV")) continue;
            if (!isDispatchShape(method, 1, 12)) continue;
            String id = "no-typing/" + method.getName() + "/" + method.getParameterCount();
            if (tryHook(id, method, chain -> {
                List<Object> args = chain.getArgs();
                if (prefs.diagnostics()) logTypingCandidate(method, args);
                if (!prefs.noTyping()) return chain.proceed();
                int code = firstInt(args, -1);
                FileLogger.runtimeMarker("BLOCK no-typing method=" + method.getName() + " api=" + code);
                FileLogger.i("M2 blocked typing indicator api=" + code + " method=" + method.getName());
                return null;
            })) installed++;
        }
        FileLogger.i("M2 no-typing hooks installed=" + installed);
    }

    private void installMessageObserver() {
        Class<?> cls = findClass(profile.coreJni);
        if (cls == null) {
            FileLogger.w("M3 message observer: class missing " + profile.coreJni);
            return;
        }
        int installed = 0;
        for (Method method : cls.getDeclaredMethods()) {
            if (!method.getName().startsWith("dispatchVO")) continue;
            if (!isDispatchShape(method, 9, 16)) continue;
            if (installed >= 8) {
                FileLogger.w("M3 message observer capped at 8 Core dispatch hooks");
                break;
            }
            String id = "message-observer/" + method.getName() + "/" + method.getParameterCount();
            if (tryHook(id, method, chain -> {
                List<Object> args = chain.getArgs();
                if (prefs.diagnostics()) logObserverCandidate(method, args);
                try { messageCache.observeCoreDispatch(args, prefs.cacheMessageText()); }
                catch (Throwable t) { FileLogger.e("M3 message observer callback failed", t); }
                return chain.proceed();
            })) installed++;
        }
        FileLogger.i("M3 message observer hooks installed=" + installed);
    }

    private void installUnsentSemanticProbe() {
        Class<?> cls = findClass(profile.unsentNotificationClass);
        if (cls == null) {
            FileLogger.i("M3 semantic unsent probe unavailable: class missing");
            return;
        }
        int installed = 0;
        for (Method method : cls.getDeclaredMethods()) {
            String n = method.getName().toLowerCase(Locale.ROOT);
            if (!n.contains("unsent")) continue;
            if (method.getParameterCount() != 0) continue;
            if (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class) continue;
            String id = "unsent-probe/" + method.getName();
            if (tryHook(id, method, chain -> {
                Object result = chain.proceed();
                if (prefs.diagnostics()) logUnsentCandidate(method, result);
                if (Boolean.TRUE.equals(result)) {
                    FileLogger.runtimeMarker("UNSENT semantic flag " + method.getName());
                    messageCache.onUnsentFlag(chain.getThisObject(), prefs.cacheMessageText());
                }
                return result;
            })) installed++;
        }
        FileLogger.i("M3 semantic unsent hooks installed=" + installed);
    }

    private boolean tryHook(String id, Method method, HookBody body) {
        try {
            method.setAccessible(true);
            FileLogger.hookPending(id, method);
            module.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> body.invoke(chain));
            FileLogger.hookOk(id, method);
            return true;
        } catch (Throwable t) {
            FileLogger.hookFailed(id, method, t);
            return false;
        }
    }

    private boolean matchesSeen(List<Object> args) {
        if (args == null || args.isEmpty()) return false;
        int api = firstInt(args, -1);
        int configured = prefs.seenApiId();
        if (configured >= 0) return api == configured;
        if (args.size() < 4) return false;
        boolean mailbox = isMailbox(args.get(1)) || isMailbox(args.get(2));
        Object fourth = args.get(3);
        boolean longish = fourth == null || fourth instanceof Long || fourth instanceof Number;
        return mailbox && longish;
    }

    private boolean isMailbox(Object value) {
        if (value == null) return false;
        String name = value.getClass().getName();
        return "com.facebook.msys.mci.Mailbox".equals(name) || name.endsWith(".Mailbox");
    }

    private void logSeenCandidate(Method method, List<Object> args) {
        int api = firstInt(args, -1);
        String key = method.getName() + ":" + api + ":" + typeShape(args);
        synchronized (seenDiagnostics) {
            if (seenDiagnostics.add(key)) FileLogger.i("M2 seen candidate " + key);
        }
    }

    private void logTypingCandidate(Method method, List<Object> args) {
        int api = firstInt(args, -1);
        String key = method.getName() + ":" + api + ":" + typeShape(args);
        synchronized (typingDiagnostics) {
            if (typingDiagnostics.add(key)) FileLogger.i("M2 typing candidate " + key);
        }
    }

    private void logObserverCandidate(Method method, List<Object> args) {
        String key = method.getName() + ":" + typeShape(args);
        synchronized (observerDiagnostics) {
            if (observerDiagnostics.size() < 32 && observerDiagnostics.add(key)) {
                FileLogger.i("M3 observer candidate " + key);
            }
        }
    }

    private void logUnsentCandidate(Method method, Object result) {
        String key = method.getName() + ":" + result;
        synchronized (unsentDiagnostics) {
            if (unsentDiagnostics.add(key)) FileLogger.i("M3 unsent probe candidate " + key);
        }
    }

    private String typeShape(List<Object> args) {
        if (args == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(',');
            Object o = args.get(i);
            sb.append(o == null ? "null" : o.getClass().getSimpleName());
        }
        return sb.toString();
    }

    private int firstInt(List<Object> args, int def) {
        if (args == null || args.isEmpty()) return def;
        Object o = args.get(0);
        return o instanceof Number ? ((Number) o).intValue() : def;
    }

    private boolean isDispatchShape(Method method, int minParams, int maxParams) {
        if (!Modifier.isStatic(method.getModifiers())) return false;
        if (method.getReturnType() != void.class) return false;
        int pc = method.getParameterCount();
        if (pc < minParams || pc > maxParams) return false;
        Class<?>[] pt = method.getParameterTypes();
        return pt.length > 0 && (pt[0] == int.class || pt[0] == Integer.class);
    }

    private Class<?> findClass(String name) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (Throwable t) {
            FileLogger.w("class not found: " + name + " (" + t.getClass().getSimpleName() + ")");
            return null;
        }
    }

    private boolean contains(String[] values, String needle) {
        for (String value : values) if (value.equals(needle)) return true;
        return false;
    }
}
