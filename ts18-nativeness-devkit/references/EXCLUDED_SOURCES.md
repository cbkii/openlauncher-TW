# Deliberately excluded sources

These files were present in the uploaded/project source pool but are **not included** in this package:

- `com.tw.music_TW_THEME.20240715.apk`
- `com.tw.music_ac.apk`
- `deapk.zip` as a whole archive, because it contains `com.tw.music_ac_src/**`

The filtered decompile archive excludes stock TW music/radio paths but keeps DoFun launcher/theme, TW EQ/DSP, and theme-plugin material.

## Rule for agents

Do not add stock `com.tw.radio` or stock `com.tw.music` integration unless the user explicitly asks for it in a later task. Do not infer approval from the presence of runtime diagnostics that mention those packages.
