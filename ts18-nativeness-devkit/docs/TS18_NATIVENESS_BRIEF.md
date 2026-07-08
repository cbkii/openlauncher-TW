# TS18 Nativeness Brief

## Objective

Make `openlauncher-TW` feel like a native TS18 / Topway / DoFun launcher while remaining a normal Android HOME launcher.

## What “native” means here

- Correctly detects the Topway/DoFun environment.
- Offers TS18-relevant shortcuts and settings by default.
- Launches exact stock components for non-media system functions where safe.
- Uses safe display bounds and works with the TS18 status/navigation regions.
- Uses Android-standard media/session behaviour for user-selected apps.
- Provides diagnostics explaining what was detected and why a shortcut is or is not available.

## What “native” does not mean

- Replacing platform-signed stock packages.
- Sharing UID with `com.tw.*` apps.
- Implementing stock `com.tw.radio` or `com.tw.music` internals.
- Copying private smali or pretending to be a vendor service.
- Flashing, remounting, or modifying system/vendor partitions.

## Exact-device observations to preserve

- Android 10/API 29 runtime behaviour is the compatibility baseline.
- Device has DoFun/TWTHEME and many `com.tw.*` services/apps.
- Root exists for diagnostics but is not an app privilege model.
- `/` and `/vendor` are read-only dynamic partitions in diagnostics.
- USB/OTG, ACC, reverse, audio focus, Bluetooth, MediaSession, and launcher contracts are separate layers.
- Content bounds/insets differ from physical pixels.

## Main implementation shape

```text
OpenLauncher-TW
  ├─ Standard Android launcher path
  ├─ HeadUnitProfileDetector
  │    ├─ StandardAndroid
  │    ├─ TopwayTs18Dofun
  │    └─ Other future profile gates
  ├─ LaunchTargetResolver
  │    ├─ package launch
  │    ├─ explicit component launch
  │    ├─ action/URI launch
  │    └─ unavailable-state diagnostics
  ├─ MediaSessionController path
  └─ TS18 safe-area/layout adapter
```

## Avoid stock TW radio/music

Stock TW radio/music are excluded as targets for this package. The launcher may still show user-selected radio/music apps and MediaSession controls.
