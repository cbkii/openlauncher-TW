# Decompiled stock app filter notes

Source: `/mnt/data/deapk.zip`.

This package includes a filtered decompile archive at:

`references/decompiled-stock-apps-filtered/deapk-filtered-no-stock-radio-music.zip`

Included from the decompile source:

- `TS-themeImport-com.dofun.variety_V7.12.16.184.240530_src/**`
- `com.tw.eq-1_src/**`
- `launcher.variety.theme.plugin.sfp_t5_src/**`

Excluded by request and by scope boundary:

- `com.tw.music_ac_src/**`
- any direct `com.tw.radio/**` or `com.tw.music/**` paths if present

Excluded entry count: 4188

Reason: stock `com.tw.radio` and `com.tw.music` native integration is out of scope unless the user explicitly requests it. Agents may use remaining DoFun/Topway material to learn package discovery, component-launch conventions, EQ/DSP launch targets, settings launch targets, safe-area assumptions, and TS18 diagnostics. They must not implement stock TW radio/music targeting from this package.
