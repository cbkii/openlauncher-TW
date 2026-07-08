# Copilot context — TS18 nativeness

OpenLauncher-TW is being adapted for a Topway/DoFun TS18 Android 10 head unit.

Important rules:

- Keep TS18 code behind profile/adapters.
- Runtime baseline is Android 10/API 29.
- Use explicit launch targets and resolver diagnostics.
- Respect safe content bounds/insets.
- Do not implement stock `com.tw.radio` or `com.tw.music` native integration.
- Do not use platform-signed/shared-UID/root assumptions.
- Prefer user-selected media/radio apps and Android MediaSession.
- Add tests/static checks for any behaviour changes.
