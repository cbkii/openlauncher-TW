OpenLauncher-TW TS18 development context:

- Read `ts18-nativeness-devkit/AGENTS.md` before suggesting TS18 integration code.
- Runtime baseline: Topway/DoFun TS18, Android 10/API 29.
- Use profile-gated adapters and explicit launch targets.
- Do not implement stock `com.tw.radio` or stock `com.tw.music` native integration.
- Prefer user-selected media/radio apps and Android MediaSession.
- Avoid root, platform signing, shared UID, private services, and unguarded vendor broadcasts.
- Add tests/static checks for resolver/profile/exclusion logic.
