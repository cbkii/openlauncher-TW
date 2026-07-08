# Stock TW Radio/Music Exclusion

The user explicitly requested:

> Exclude com.tw.radio and com.tw.music native integration unless explicitly requested by the user; they're shit apps and will not be used.

## Practical effect

Agents must not:

- implement stock `com.tw.radio` control;
- add stock `com.tw.radio` as a TS18 default target;
- implement stock `com.tw.music` control;
- add stock `com.tw.music` or `com.tw.media/com.tw.music.MusicActivity` as a TS18 default target;
- re-use DoFun stock music widget contracts;
- claim those apps are desired integration targets.

Agents may:

- detect that these packages exist only to avoid/default-away from them;
- document them as excluded legacy/stock targets;
- prefer user-selected music/radio apps;
- support NavRadio Plus as a known radio candidate when installed;
- use Android MediaSession for currently active media apps.

## Test requirement

Add a guard test/static check that the TS18 default preset does not include stock TW radio/music components.
