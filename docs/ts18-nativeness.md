# TS18 Nativeness Support

This OpenLauncher-TW repository has been modified to detect the TS18/Topway/DoFun head unit profile safely and integrate correctly with native car apps, *without* replacing or assuming system/root privileges.

- **Exact-device baseline**: TS18, Topway, DoFun environment (UIS8581A / Android 10).
- **Profile Detection**: Done via user-level PackageManager checks.
- **Excluded apps**: We explicitly exclude `com.tw.radio` and `com.tw.music` as default integration targets based on agent directives. Preferred path is generic MediaSession or NavRadio+ for radio.
- **Safe-area**: The display area has been updated to dynamically respect WindowInsets for the OEM navigation and status bars if configured via `respectSafeArea`.
