# TS18 Safe Area, Display, and Input Notes

## Observed baseline

- Physical panel: 1280×720.
- Runtime app/content area may be smaller due to TS18 OEM status/navigation regions.
- Evidence from earlier diagnostics: stable content roughly `[0,55]-[1225,720]`, around 1225×665.
- Raw touch can be 1024×600 scaled to 1280×720.

## Implementation requirements

- Use runtime `WindowInsets`, `WindowMetrics`, Compose insets, and measured bounds.
- Do not assume the whole 1280×720 surface is safe for controls.
- Clamp saved widget/sidebar positions to current content bounds.
- Add a TS18 setting to respect OEM safe areas.
- Add a diagnostic overlay/page showing physical/display/content/inset numbers.
- Validate cold start, launcher restart, reboot, and ACC sleep/wake on device.

## Avoid

- Hard-coded phone status bar assumptions.
- Unconditional fullscreen hiding when the user wants OEM bars visible.
- Saving widget coordinates that cannot be restored after density/bounds changes.
