package io.github.binxgtl.messengervamos;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class RuntimeDiscovery {
    private RuntimeDiscovery() {}

    static void dump(ClassLoader cl, CompatProfile profile, String version) {
        StringBuilder out = new StringBuilder();
        out.append("MessengerVamos runtime signature dump\n");
        out.append("version=").append(version).append('\n');
        out.append("profileExact=").append(profile.exact).append("\n\n");
        dumpClass(cl, profile.sdkJni, out, true);
        dumpClass(cl, profile.coreJni, out, true);
        dumpClass(cl, profile.typingJni, out, true);
        dumpClass(cl, profile.presenceJni, out, true);
        dumpClass(cl, profile.unsentNotificationClass, out, false);
        String safe = version == null ? "unknown" : version.replaceAll("[^A-Za-z0-9._-]", "_");
        FileLogger.writeNamed("signatures-" + safe + ".txt", out.toString(), false);
        FileLogger.i("M1 runtime discovery complete");
    }

    private static void dumpClass(ClassLoader cl, String name, StringBuilder out, boolean dispatchOnly) {
        out.append("CLASS ").append(name).append('\n');
        try {
            Class<?> c = Class.forName(name, false, cl);
            out.append("  FOUND modifiers=").append(Modifier.toString(c.getModifiers())).append('\n');
            int count = 0;
            for (Method m : c.getDeclaredMethods()) {
                String lower = m.getName().toLowerCase();
                if (dispatchOnly && !m.getName().startsWith("dispatch")) continue;
                if (!dispatchOnly && !(lower.contains("unsent") || lower.contains("delete") || lower.contains("message"))) continue;
                out.append("  M ").append(Modifier.toString(m.getModifiers())).append(' ')
                        .append(m.getReturnType().getTypeName()).append(' ')
                        .append(m.getName()).append('(');
                Class<?>[] p = m.getParameterTypes();
                for (int i = 0; i < p.length; i++) {
                    if (i > 0) out.append(',');
                    out.append(p[i].getTypeName());
                }
                out.append(")\n");
                if (++count >= 250) {
                    out.append("  ... capped at 250 methods\n");
                    break;
                }
            }
            for (Constructor<?> ctor : c.getDeclaredConstructors()) {
                out.append("  C ").append(ctor).append('\n');
            }
        } catch (Throwable t) {
            out.append("  MISSING ").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append('\n');
        }
        out.append('\n');
    }
}
