package io.github.binxgtl.messengervamos;

final class CompatProfile {
    final String version;
    final boolean exact;
    final String sdkJni;
    final String coreJni;
    final String typingJni;
    final String presenceJni;
    final String unsentNotificationClass;
    final String[] seenMethods;

    CompatProfile(String version, boolean exact, String sdkJni, String coreJni,
                  String typingJni, String presenceJni, String unsentNotificationClass,
                  String[] seenMethods) {
        this.version = version;
        this.exact = exact;
        this.sdkJni = sdkJni;
        this.coreJni = coreJni;
        this.typingJni = typingJni;
        this.presenceJni = presenceJni;
        this.unsentNotificationClass = unsentNotificationClass;
        this.seenMethods = seenMethods;
    }
}
