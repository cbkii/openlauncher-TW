# Root and Privilege Boundary

## Observed root state

Diagnostics showed root/Magisk availability, SELinux permissive, unlocked/orange AVB state, and dynamic read-only system/vendor partitions.

## What root does not provide

Root does **not** provide:

- platform signing keys;
- `android.uid.system` identity;
- signature permissions;
- DoFun/TW vendor app identity;
- permission to impersonate `com.tw.*` protected apps safely.

## Runtime app policy

OpenLauncher-TW should run as a normal Android app/HOME launcher.

Root is allowed only for optional, user-started diagnostics or future separately approved helpers. Do not build core launcher behaviour around root.

## STOP conditions

Stop and ask for explicit approval before:

- modifying system/vendor partitions;
- disabling protected packages;
- installing Magisk modules;
- using private service calls;
- writing settings that alter MCU/CAN/reverse/audio routing;
- claiming platform-signed behaviour.
