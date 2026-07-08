# `openlauncher-TW` current-state summary

This file summarises the current upstream/fork state as inspected during package creation.

## Repository state

- Fork checked: `cbkii/openlauncher-TW`.
- Upstream fallback checked: `dw2lam/openlauncher`.
- Key files inspected from both were effectively identical at the time of review.

## Current strengths

- Android app with HOME/default launcher activity.
- Fullscreen landscape launcher activity.
- Notification listener service for MediaSession/Now Playing.
- App library uses installed application enumeration, which can surface head-unit apps without normal launcher icons.
- Settings screen already includes default-launcher, notification access, overlay, and location permission flows.
- Home screen has widget grid, sidebar, weather, telemetry, Now Playing, radio-widget placeholder/backends, and customisation infrastructure.

## Current gaps for TS18

- Radio backend is szchoiceway-oriented and not Topway/DoFun-safe as a native control path.
- Default shortcuts are labels/icons, not component-level TS18 launch targets.
- Stock TW radio/music targets must be excluded for this workload.
- Safe-area/inset handling needs TS18-specific measured bounds support.
- Profile detection and diagnostics are missing.
- Component/URI/action launch targets need a first-class model.

## Suggested initial PR

Implement profile detection + LaunchTarget model + TS18 preset excluding stock radio/music + diagnostics page. Keep it small before touching layout/radio/session internals.
