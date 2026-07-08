# Repository Agent Instructions — OpenLauncher-TW TS18 Work

When working on TS18 nativeness, first read `ts18-nativeness-devkit/AGENTS.md` and the docs in `ts18-nativeness-devkit/docs/`.

Hard rule: do not implement stock `com.tw.radio` or stock `com.tw.music` native integration unless the user explicitly requests it later.

Keep TS18 work profile-gated, Android 10 compatible, reversible, and tested. Do not use root/platform-signing/private-service assumptions.

## Update: TS18 Implementation Details
* Added `HeadUnitProfile` and `LaunchTargetResolver`.
* Created `TopwayTs18LaunchTargets` excluding stock radio/music.
* Implemented OEM Safe Area respect functionality.
* Created a lightweight `DiagnosticsScreen`.
* Refactored radio behavior via `LauncherViewModel` and `RadioBackend` abstract approaches to never run Szchoiceway logic on TS18.
