# Validation Plan

## Local/unit validation

Add tests for:

- profile detector confidence/evidence aggregation;
- launch target resolver order;
- component unavailable fallback;
- migration from package-only shortcuts to launch targets;
- exclusion of stock `com.tw.radio` and `com.tw.music` from default TS18 presets;
- safe-area clamping logic;
- Android 10/API 29 guarded code paths.

## Static checks

Add a test or script that fails if default TS18 presets include:

```text
com.tw.radio
com.tw.music
com.tw.media/com.tw.music.MusicActivity
com.tw.music/com.tw.music.MusicActivity
```

These may appear in docs as excluded examples only, but not as default integration targets.

## Device validation

On TS18, validate:

- first boot/start as HOME;
- default launcher flow;
- DoFun/TW profile detection;
- EQ/DSP launch;
- BT/phone launch;
- car settings launch;
- AUX and file-manager launch if present;
- safe-area layout with OEM status/nav bars;
- user-selected music/radio app session control;
- reboot and ACC sleep/wake restore;
- no vendor services disabled or crashed.

## What not to claim

Do not claim TS18 compatibility, native radio control, native music control, or ACC correctness unless it was tested on the device.
