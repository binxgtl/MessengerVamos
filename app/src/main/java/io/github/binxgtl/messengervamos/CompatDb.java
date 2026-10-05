package io.github.binxgtl.messengervamos;

final class CompatDb {
    private CompatDb() {}

    static CompatProfile forVersion(String version) {
        boolean exact = Config.TARGET_VERSION.equals(version);
        return new CompatProfile(
                version,
                exact,
                "com.facebook.sdk.mca.MailboxSDKJNI",
                "com.facebook.core.mca.MailboxCoreJNI",
                "com.facebook.typingindicator.mca.MailboxTypingIndicatorJNI",
                "com.facebook.presence.mca.MailboxPresenceJNI",
                "com.facebook.messenger.notification.engine.MSGOpenPathRenderedNotification",
                new String[]{"dispatchVOOOOO", "dispatchVJOOOO"}
        );
    }
}
