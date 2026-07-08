# TS18 Launch Target Map — stock radio/music excluded

This map is derived from DoFun/TW launcher and package evidence, but intentionally excludes native stock `com.tw.radio` and `com.tw.music` integration.

Each target must be resolved before display/use. Missing components should appear as unavailable with diagnostic evidence, not crash or silently fail.

## Safe/allowed TS18 targets

### EQ / DSP

Observed package family: `com.tw.eq`.

Candidate components:

```text
com.tw.eq/com.tw.eq.EQActivity
com.tw.eq/com.tw.eq.DSPActivity
```

Purpose: open stock EQ/DSP controls. This is in scope.

### Bluetooth / phone

Candidate package:

```text
com.tw.bt
```

Purpose: open stock BT/phone UI. Do not assume Android Bluetooth pairing/session semantics match Topway BT internals.

### Car settings

Candidate components:

```text
com.dofun.carsetting/com.dofun.carsetting.ui.MainActivity
com.android.settings/com.android.settings.Settings
```

Purpose: prefer DoFun car settings when present; fall back to Android Settings.

### AUX input

Candidate component:

```text
com.tw.auxin/com.tw.auxin.AuxInActivity
```

Purpose: launch AUX video/audio input UI if installed.

### File manager

Candidate package:

```text
com.tw.twfileexplore
```

Purpose: open stock file manager. DocumentsUI may be absent; do not assume SAF works.

### Steering wheel / keypad settings

Candidate components:

```text
com.android.settings.keypad2.KeyPad2Activity
com.android.settings.KeypadActivity
com.tw.keypad/com.tw.keypad.SteeringWheelActivity
```

Purpose: open settings surfaces only. Do not write key mappings directly.

### DoFun theme/settings URIs

Candidate URIs/actions from DoFun evidence:

```text
launcher://variety/setting
launcher://variety/theme/category/one
launcher://variety/theme/category/two
```

Resolve before use. These are launcher/deep-link surfaces; they may be version/theme dependent.

## Explicitly excluded targets

Do not add default shortcuts or native integration for:

```text
com.tw.radio
com.tw.music
com.tw.media/com.tw.music.MusicActivity
com.tw.music/com.tw.music.MusicActivity
```

If agents see those in DoFun mappings, keep them documented as excluded. For radio/music, use user-selected apps, NavRadio Plus, Auxio, or generic MediaSession behaviour only.

## Suggested model

```kotlin
data class LaunchTarget(
    val id: String,
    val label: String,
    val packageName: String? = null,
    val className: String? = null,
    val action: String? = null,
    val uri: String? = null,
    val iconHint: DefaultShortcutIcon = DefaultShortcutIcon.NONE,
    val requiresProfile: HeadUnitProfile? = null,
    val excludedReason: String? = null
)
```

## Resolver order

1. explicit component;
2. action/URI intent if resolvable;
3. package launch intent;
4. first safe `ACTION_MAIN` activity fallback;
5. unavailable diagnostic state.
