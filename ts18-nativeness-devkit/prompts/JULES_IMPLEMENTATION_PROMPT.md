# Jules prompt — TS18 nativeness foundation for OpenLauncher-TW

You are working in `cbkii/openlauncher-TW`. Inspect the repository first, then propose a plan before implementing. Keep changes small, reviewable, and PR-ready.

## Context

A reference package exists at `ts18-nativeness-devkit/`. Read:

- `ts18-nativeness-devkit/AGENTS.md`
- `ts18-nativeness-devkit/docs/TS18_NATIVENESS_BRIEF.md`
- `ts18-nativeness-devkit/docs/TS18_LAUNCH_TARGETS_NO_STOCK_RADIO_MUSIC.md`
- `ts18-nativeness-devkit/docs/STOCK_RADIO_MUSIC_EXCLUSION.md`
- `ts18-nativeness-devkit/docs/VALIDATION_PLAN.md`

## User requirement

Improve TS18 / Topway / DoFun nativeness for OpenLauncher-TW, but **exclude stock `com.tw.radio` and stock `com.tw.music` native integration unless the user explicitly requests it later**.

## Initial implementation scope

Implement the foundation only:

1. Add a `HeadUnitProfileDetector` or equivalent with a `TopwayTs18Dofun` profile.
2. Add a first-class launch-target model supporting package, explicit component, action, and URI targets.
3. Add TS18/DoFun launch target presets for EQ/DSP, Bluetooth/phone, car settings, AUX, file manager, keypad/steering-wheel settings, and DoFun theme/settings URI targets.
4. Explicitly exclude stock `com.tw.radio`, `com.tw.music`, and `com.tw.media/com.tw.music.MusicActivity` from default presets.
5. Add UI/settings diagnostics showing detected profile, evidence, and resolved/unavailable launch targets.
6. Preserve existing user settings via migration from package-only shortcuts.
7. Add tests or static checks for resolver/profile logic and stock radio/music exclusion.

## Do not implement yet

- Direct TW radio control.
- Direct TW music control.
- Private Topway/Cardoor/TWUtil APIs.
- Root/Magisk helpers.
- Service disabling or system/vendor modifications.
- Broad layout refactor unless required for diagnostics.

## Validation

Run practical repo checks. Do not claim TS18 runtime validation unless the device was actually tested. Mention any checks that could not run locally.
