# Codex prompt — audit and harden TS18 nativeness work

Audit the latest `cbkii/openlauncher-TW` branch for TS18 / Topway / DoFun nativeness work. Use `ts18-nativeness-devkit/` as the reference context.

Focus on:

- profile detection correctness;
- launch-target resolver robustness;
- explicit component/action/URI handling;
- Android 10/API 29 compatibility;
- safe fallback when packages/components are missing;
- no stock `com.tw.radio` or stock `com.tw.music` native integration in defaults;
- no platform-signing/shared-UID/root assumptions;
- no private Topway APIs without explicit user approval;
- no regressions to generic/non-TS18 launcher behaviour;
- tests and static checks covering the exclusion and resolver behaviour.

Produce a PR-ready patch only for demonstrated issues. Do not broaden into speculative private integrations.
