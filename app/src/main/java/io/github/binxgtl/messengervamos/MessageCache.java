package io.github.binxgtl.messengervamos;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MessageCache {
    private static final int MAX = 250;

    static final class Entry {
        final String id;
        final String text;
        final String sender;
        final String thread;
        final long time;

        Entry(String id, String text, String sender, String thread) {
            this.id = id;
            this.text = text;
            this.sender = sender;
            this.thread = thread;
            this.time = System.currentTimeMillis();
        }
    }

    private final LinkedHashMap<String, Entry> map = new LinkedHashMap<String, Entry>(MAX + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            return size() > MAX;
        }
    };

    synchronized void observeCoreDispatch(List<Object> args, boolean persistText) {
        if (args == null || args.size() < 9) return;
        Object idObj = args.get(6);
        Object textObj = args.get(8);
        if (!(idObj instanceof String) || !(textObj instanceof String)) return;
        String id = (String) idObj;
        String text = (String) textObj;
        if (id.isEmpty() || text.isEmpty()) return;
        String sender = args.get(3) instanceof String ? (String) args.get(3) : "";
        String thread = String.valueOf(args.get(2));
        map.put(id, new Entry(id, text, sender, thread));
        FileLogger.i("M3 message observed id=" + shortId(id) + " textLen=" + text.length());
        if (persistText) {
            FileLogger.writeNamed("message-cache.tsv", sanitize(id) + "\t" + sanitize(thread) + "\t" + sanitize(sender) + "\t" + sanitize(text) + "\n", true);
        }
        if (containsUnsentMarker(args)) {
            recordRecovered(id, persistText, "dispatch-marker");
        }
    }

    synchronized void onUnsentFlag(Object owner, boolean persistText) {
        if (owner == null) {
            FileLogger.i("M3 unsent flag observed; owner=null");
            return;
        }
        String matchedId = null;
        for (Class<?> c = owner.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(owner);
                    if (v instanceof String && map.containsKey(v)) {
                        matchedId = (String) v;
                        break;
                    }
                } catch (Throwable ignored) {}
            }
            if (matchedId != null) break;
        }
        if (matchedId != null) recordRecovered(matchedId, persistText, "semantic-unsent-flag");
        else FileLogger.i("M3 unsent flag observed; no cached message id matched");
    }

    private void recordRecovered(String id, boolean persistText, String source) {
        Entry e = map.get(id);
        if (e == null) return;
        FileLogger.i("M3 UNSENT recovered source=" + source + " id=" + shortId(id) + " textLen=" + e.text.length());
        if (persistText) {
            FileLogger.writeNamed("unsent-recovered.tsv", sanitize(e.id) + "\t" + sanitize(e.thread) + "\t" + sanitize(e.sender) + "\t" + sanitize(e.text) + "\n", true);
        }
    }

    private static boolean containsUnsentMarker(List<Object> args) {
        for (Object arg : args) {
            if (!(arg instanceof String)) continue;
            String s = ((String) arg).toLowerCase(Locale.ROOT);
            if (s.contains("unsent") || s.contains("message_unsent") || s.contains("delete_message")) return true;
        }
        return false;
    }

    private static String shortId(String id) {
        if (id == null) return "null";
        return id.length() <= 10 ? id : id.substring(0, 6) + "..." + id.substring(id.length() - 3);
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace('\\', '/').replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
