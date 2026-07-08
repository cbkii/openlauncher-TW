# TS18 Nativeness Devkit for `cbkii/openlauncher-TW`

This directory is a self-contained reference and agent-instruction pack for improving TS18 / Topway / DoFun integration in `cbkii/openlauncher-TW`.

It is designed to be unpacked as a new top-level directory in the repository, for example:

```bash
unzip ts18-nativeness-devkit.zip -d /path/to/openlauncher-TW
cd /path/to/openlauncher-TW/ts18-nativeness-devkit
```

## Scope

Build OpenLauncher-TW into a TS18-aware HOME launcher that feels native on the user’s exact TS18 device by improving:

- Topway/DoFun device-profile detection;
- explicit component launching for stock settings, EQ/DSP, Bluetooth/phone, AUX, file manager, keypad/steering-wheel settings, DoFun theme/settings surfaces, and user-selected media apps;
- safe-area/layout handling for TS18 1280×720 panels with OEM status/navigation regions;
- robust package/component resolution and diagnostics;
- conservative standards-based media handling via Android MediaSession where appropriate;
- development documentation, tests, and validation hooks.

## Explicit exclusion

Do **not** implement direct/native integration for stock `com.tw.radio` or stock `com.tw.music` unless the user explicitly requests it in a later instruction. Those stock apps are intentionally excluded from this package as integration targets.

Allowed radio/music direction for this workload:

- user-selected music app targets;
- standards-based MediaSession controls;
- NavRadio Plus or other user-selected radio app launching/session handling;
- explicit exclusion/disablement of stock TW radio/music defaults from presets.

## Contents

- `AGENTS.md` — root agent instructions for this workload.
- `docs/` — TS18 integration brief, launch-target map, validation plan, and safety boundaries.
- `prompts/` — ready-to-send prompts for Jules, Codex, and Copilot-assisted development.
- `repo-overlay/` — optional files to copy into repo root if you want Codex/Copilot/Jules instructions discovered automatically.
- `references/raw/` — original source/reference artifacts, excluding stock TW music APKs.
- `references/decompiled-stock-apps-filtered/` — filtered decompile archive with stock radio/music removed.
- `references/SHA256SUMS.json` — checksums and sizes for packaged references.
- `scripts/` — helper scripts for validating the reference package and listing contents.

## First agent task recommendation

Start with a small PR that adds:

1. `HeadUnitProfileDetector` with `TopwayTs18Dofun` detection.
2. A launch-target model that supports package, component, action, and URI targets.
3. A TS18 launch-target preset excluding stock `com.tw.radio` and `com.tw.music`.
4. A settings diagnostics page showing detection evidence and resolved launch targets.

Do not begin with private broadcast/control protocols. Inspect and launch first.
