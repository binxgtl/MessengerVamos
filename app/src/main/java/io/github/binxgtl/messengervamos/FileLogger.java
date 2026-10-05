package io.github.binxgtl.messengervamos;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Executable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

final class FileLogger {
    static final String TAG = "MessengerVamos";
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final SimpleDateFormat FILE_TS = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
    private static final AtomicBoolean CRASH_HANDLER_INSTALLED = new AtomicBoolean(false);

    private static File logDir;
    private static File sessionLog;
    private static File latestLog;

    private FileLogger() {}

    static void init(Context context) {
        synchronized (LOCK) {
            if (sessionLog != null) return;
            File base = null;
            try {
                if (context != null) {
                    File external = context.getExternalFilesDir(null);
                    if (external != null) base = new File(external, "MessengerVamos");
                    if (base == null) base = new File(context.getFilesDir(), "MessengerVamos");
                }
            } catch (Throwable ignored) {}
            if (base == null) {
                base = new File("/data/user/0/com.facebook.orca/files/MessengerVamos");
            }
            logDir = new File(base, "logs");
            try { logDir.mkdirs(); } catch (Throwable ignored) {}
            sessionLog = new File(logDir, "session-" + FILE_TS.format(new Date()) + ".log");
            latestLog = new File(logDir, "latest.log");
            writeRaw(latestLog, "", false);
            write(Log.INFO, "logger initialized; dir=" + logDir.getAbsolutePath(), null);
        }
    }

    static String logPath() {
        synchronized (LOCK) {
            return logDir == null ? "/storage/emulated/0/Android/data/com.facebook.orca/files/MessengerVamos/logs" : logDir.getAbsolutePath();
        }
    }

    static void i(String msg) { write(Log.INFO, msg, null); }
    static void w(String msg) { write(Log.WARN, msg, null); }
    static void e(String msg, Throwable t) { write(Log.ERROR, msg, t); }

    private static void write(int level, String msg, Throwable t) {
        String line = TS.format(new Date()) + " [" + levelName(level) + "] " + msg;
        if (t != null) line += "\n" + stackTrace(t);
        if (level >= Log.ERROR) Log.e(TAG, msg, t);
        else if (level >= Log.WARN) Log.w(TAG, msg, t);
        else Log.i(TAG, msg);
        synchronized (LOCK) {
            if (sessionLog != null) writeRaw(sessionLog, line + "\n", true);
            if (latestLog != null) writeRaw(latestLog, line + "\n", true);
        }
    }

    static void hookPending(String id, Executable executable) {
        String text = "PENDING\n" + TS.format(new Date()) + "\n" + id + "\n" + executable + "\n";
        writeNamed("last-hook.txt", text, false);
        i("hook pending: " + id + " -> " + executable);
    }

    static void hookOk(String id, Executable executable) {
        String text = "OK\n" + TS.format(new Date()) + "\n" + id + "\n" + executable + "\n";
        writeNamed("last-hook.txt", text, false);
        i("hook installed: " + id + " -> " + executable);
    }

    static void hookFailed(String id, Executable executable, Throwable t) {
        String text = "FAILED\n" + TS.format(new Date()) + "\n" + id + "\n" + executable + "\n" + stackTrace(t) + "\n";
        writeNamed("last-hook.txt", text, false);
        e("hook failed: " + id + " -> " + executable, t);
    }

    static void runtimeMarker(String text) {
        writeNamed("last-runtime-event.txt", TS.format(new Date()) + "\n" + text + "\n", false);
    }

    static void writeNamed(String name, String content, boolean append) {
        synchronized (LOCK) {
            if (logDir == null) return;
            writeRaw(new File(logDir, name), content, append);
        }
    }

    static void installCrashHandler() {
        if (!CRASH_HANDLER_INSTALLED.compareAndSet(false, true)) return;
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                String text = "thread=" + thread.getName() + "\n" + stackTrace(throwable) + "\n";
                writeNamed("last-java-crash.txt", TS.format(new Date()) + "\n" + text, false);
                e("uncaught exception in " + thread.getName(), throwable);
            } catch (Throwable ignored) {}
            if (previous != null) previous.uncaughtException(thread, throwable);
        });
        i("Java crash handler installed");
    }

    private static void writeRaw(File file, String text, boolean append) {
        try (FileWriter fw = new FileWriter(file, append)) {
            fw.write(text);
            fw.flush();
        } catch (Throwable t) {
            Log.e(TAG, "file log write failed: " + file, t);
        }
    }

    private static String stackTrace(Throwable t) {
        if (t == null) return "";
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String levelName(int level) {
        if (level >= Log.ERROR) return "E";
        if (level >= Log.WARN) return "W";
        if (level >= Log.INFO) return "I";
        return "D";
    }
}
