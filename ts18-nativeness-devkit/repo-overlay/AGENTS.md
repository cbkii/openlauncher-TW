# Repository Agent Instructions — OpenLauncher-TW TS18 Work

When working on TS18 nativeness, first read `ts18-nativeness-devkit/AGENTS.md` and the docs in `ts18-nativeness-devkit/docs/`.

Hard rule: do not implement stock `com.tw.radio` or stock `com.tw.music` native integration unless the user explicitly requests it later.

Keep TS18 work profile-gated, Android 10 compatible, reversible, and tested. Do not use root/platform-signing/private-service assumptions.

## Update: TS18 Implementation Details
* Added evidence-based `HeadUnitProfile` detection with an effective-profile override flow.
* Created `TopwayTs18LaunchTargets` excluding stock radio/music and resolving every target before use.
* Added runtime-inset safe-area handling that remains user-configurable.
* Added a user-visible diagnostics screen with local JSON sharing and target availability.
* Profile-gated the existing Szchoiceway observer so it is never treated as a TS18 backend.
