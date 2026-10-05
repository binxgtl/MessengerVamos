# MessengerVamos

Clean-room modern libxposed module for Facebook Messenger. The code in this repository was written independently; older Messenger modules are used only as behavioral references, not as a source-code base.

## Current target

- Package: `com.facebook.orca`
- Verified target profile: Messenger `581.0.0.49.91`
- Xposed API: modern libxposed API 102
- Primary framework target: Vector 2.2+ / other API-102-compatible frameworks

## M0 → M4 status

- **M0 – bootstrap:** API-102 module entry, static Messenger scope, CI build.
- **M1 – discovery:** runtime signature dump for the relevant MCA/JNI classes. No broad `hookAllMethods` exploration layer.
- **M2 – privacy hooks:** No Seen and No Typing with strict version gating and defensive signature checks.
- **M3 – message/unsend diagnostics:** optional message observer plus semantic unsent probe. Message text persistence is OFF by default.
- **M4 – settings & compatibility:** standalone settings UI, Remote Preferences, exact-version compatibility profile and fail-closed behavior on unknown Messenger versions.

## Diagnostics

MessengerVamos intentionally writes diagnostic files when a hook fails or the target crashes. Expected location in the Messenger app sandbox:

`/storage/emulated/0/Android/data/com.facebook.orca/files/MessengerVamos/logs`

Useful files:

- `latest.log` – current session diagnostics.
- `last-hook.txt` – `PENDING`, `OK`, or `FAILED` for the most recent hook installation attempt. A native crash during hook installation can leave this as `PENDING`.
- `last-runtime-event.txt` – last blocked Seen/Typing or unsent checkpoint.
- `last-java-crash.txt` – uncaught Java exception stack, when Java receives the crash.
- `signatures-<version>.txt` – runtime class/method map for porting future Messenger versions.
- `status.txt` – framework/API/version summary.

Native aborts cannot be caught by a Java uncaught-exception handler, so `last-hook.txt` and `last-runtime-event.txt` are deliberately persisted before/around risky operations.

## Safety strategy

The module does not scan and hook every MCA dispatch. Mutating hooks are enabled only for the exact compatibility profile by default. Unknown Messenger versions run discovery diagnostics but skip mutation unless the strict-version guard is explicitly disabled.

## Build

```bash
gradle :app:assembleDebug
```

CI publishes `MessengerVamos-M4-debug` as a GitHub Actions artifact.

## Test order

1. Install the APK and enable scope only for Messenger.
2. Open MessengerVamos once and confirm **Xposed service: connected**.
3. Force-stop Messenger and reopen it 4–5 times; confirm no crash loop.
4. Test Typing from a second account/device.
5. Test Seen from a second account/device.
6. If a feature fails or Messenger crashes, collect the diagnostic files above before changing settings.

## License

GPL-3.0-only.
