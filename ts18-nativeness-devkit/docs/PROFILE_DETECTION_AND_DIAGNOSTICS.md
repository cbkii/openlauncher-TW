# Profile Detection and Diagnostics

## Goal

Detect the TS18/Topway/DoFun environment without relying on root, private services, or unsafe probes.

## Suggested evidence checks

Use PackageManager and Android public APIs only:

- installed package names:
  - `com.dofun.variety`
  - `com.dofun.carsetting`
  - `com.tw.service`
  - `com.tw.core`
  - `com.tw.coreservice`
  - `com.tw.carinfoservice`
  - `com.tw.bt`
  - `com.tw.eq`
  - `com.tw.auxin`
  - `com.tw.twfileexplore`
- resolvable components from the launch target map;
- build properties only if readable through normal APIs or user-exported diagnostics;
- screen/content bounds from runtime WindowMetrics/insets;
- selected HOME role/default launcher state.

## Confidence levels

Use a simple confidence model:

- `NONE`: no Topway/DoFun evidence.
- `LOW`: one or two generic `com.tw.*` packages.
- `MEDIUM`: DoFun plus several TW packages.
- `HIGH`: DoFun/TWTHEME plus `com.tw.service/core/coreservice/carinfoservice` and multiple exact launch targets.

## Diagnostics UI

Add a user-visible page that shows:

- detected profile and confidence;
- evidence packages/components found/missing;
- active HOME/default launcher status;
- notification listener status;
- overlay permission status;
- safe-area/insets snapshot;
- active media-session packages;
- selected music/radio apps;
- exported JSON button.

Do not enable Ylog or vendor debug modes automatically.
