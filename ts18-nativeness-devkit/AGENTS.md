# Agent Instructions — TS18 Nativeness for `openlauncher-TW`

## Role

Act as a senior Android automotive, embedded, and launcher-integration engineer working on `cbkii/openlauncher-TW` for a Topway/DoFun TS18 head unit.

## Device baseline

Observed latest exact-device baseline:

- Topway / TS18 / DoFun / TWTHEME head unit.
- Platform: UIS8581A / SP9863A-class, Android 10 / API 29 runtime.
- Build family: `s9863a1h10_Natv`, `uis8581a2h10` / `sp9863a`.
- Example theme/build baseline: `TS18.2.2_20241210.165912_WINDOW-THEME1`, FOTA `WINDOW-THEME1_1000`.
- Display: physical 1280×720, app/content area may be smaller due to 55 px top status region and right nav region.
- Touch evidence: raw 1024×600 scaled to 1280×720.
- Root state observed in diagnostics: Magisk/root available, SELinux permissive, dynamic partitions read-only. Root does not provide platform keys, signature permissions, vendor identity, or UID 1000.

## Hard scope boundary

Do **not** add direct/native integration for:

- stock `com.tw.radio`;
- stock `com.tw.music`;
- DoFun’s fixed stock music widget contract;
- platform-signed/shared-UID impersonation;
- private radio/music commands.

These stock apps are intentionally excluded unless the user explicitly asks for them later. Treat any stock TW radio/music evidence in diagnostics as negative/avoidance evidence, not an integration target.

Allowed media/radio direction:

- user-selected music and radio apps;
- Android MediaSession where available;
- NavRadio Plus support as a user-selected/known alternative;
- generic launch/session fallback;
- explicit UI showing unavailable sessions rather than fake tuner state.

## Required implementation principles

- Label assumptions in code comments or docs as Observed, Inferred, Hypothesis, Requires device validation, or Unsupported when it matters.
- Keep TS18-specific code behind adapters/profile gates.
- Preserve standard Android behaviour for non-TS18 devices.
- Prefer explicit component launch targets and resolvability checks over broad package launches.
- Do not disable/delete/probe protected OEM services casually.
- Do not use root for app runtime behaviour unless separately approved.
- No private broadcast, binder, serial, or TWUtil/Cardoor integration without a proven contract and fallback.
- Keep all probes user-started, local-only, bounded, visible, and auto-stopped.
- Respect Android 10/API 29 runtime even if the project targetSdk is newer.
- Guard newer Android APIs.
- Use runtime insets and bounds; do not assume the full 1280×720 is safe content space.

## Protected TS18 services/packages

Treat the following as protected evidence surfaces, not things to disable or replace:

- `com.tw.service*`, `com.tw.core`, `com.tw.coreservice`, `com.tw.carinfoservice`
- `com.tw.bt`, `com.tw.eq`, `com.tw.reverse`, `com.tw.devicefan`
- `com.dofun.variety`, `com.dofun.carsetting`, DoFun theme/plugin surfaces
- `gocsdk`, `s-link`, `z-link`, `ylog*`, FOTA/system update packages
- Android SystemUI, MediaProvider, Settings

## Preferred implementation order

1. Add profile detection and manual override.
2. Add launch-target model and resolver.
3. Add TS18/DoFun target presets excluding stock radio/music.
4. Add settings diagnostics/export.
5. Add safe-area/layout handling.
6. Add tests/static checks.
7. Only then consider deeper private integrations if explicitly requested.

## Validation expectations

For each PR:

- Provide small, reviewable changes.
- Add or update tests for model/resolver/profile logic.
- Run the repo’s practical checks and state exactly what passed.
- Do not claim TS18 runtime success without device validation.
- Include fallback paths for missing components.
- Keep non-TS18 behaviour unchanged unless a change is intentional and tested.
