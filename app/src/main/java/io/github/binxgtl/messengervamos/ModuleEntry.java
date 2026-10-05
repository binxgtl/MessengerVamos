package io.github.binxgtl.messengervamos;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class ModuleEntry extends XposedModule {
    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(Log.INFO, FileLogger.TAG, "M0 module loaded process=" + param.getProcessName() + " api=" + getApiVersion());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!Config.TARGET_PACKAGE.equals(param.getPackageName())) return;

        Context context = AppContext.currentApplication();
        FileLogger.init(context);
        FileLogger.installCrashHandler();
        FileLogger.i("M0 package ready package=" + param.getPackageName() + " framework=" + getFrameworkName() + " " + getFrameworkVersion());

        SharedPreferences remote = null;
        try {
            remote = getRemotePreferences(Config.PREF_GROUP);
            FileLogger.i("M4 remote preferences connected");
        } catch (Throwable t) {
            FileLogger.e("M4 remote preferences unavailable; built-in defaults will be used", t);
        }
        Prefs prefs = new Prefs(remote);

        String version = resolveVersion(context);
        CompatProfile profile = CompatDb.forVersion(version);
        FileLogger.i("target Messenger version=" + version + " expected=" + Config.TARGET_VERSION + " exact=" + profile.exact);

        RuntimeDiscovery.dump(param.getClassLoader(), profile, version);

        HookInstaller installer = new HookInstaller(this, param.getClassLoader(), prefs, profile, version);
        installer.install();

        FileLogger.writeNamed("status.txt",
                "MessengerVamos 0.4.0-m4\n" +
                        "package=" + param.getPackageName() + "\n" +
                        "version=" + version + "\n" +
                        "exactProfile=" + profile.exact + "\n" +
                        "framework=" + getFrameworkName() + " " + getFrameworkVersion() + "\n" +
                        "api=" + getApiVersion() + "\n" +
                        "logs=" + FileLogger.logPath() + "\n", false);
        FileLogger.i("M4 initialization complete");
    }

    private String resolveVersion(Context context) {
        if (context == null) return "unknown";
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(Config.TARGET_PACKAGE, 0);
            return pi.versionName == null ? "unknown" : pi.versionName;
        } catch (Throwable t) {
            FileLogger.e("failed to resolve Messenger version", t);
            return "unknown";
        }
    }
}
