# Android CI

Pull requests targeting `main`, pushes to `main`, and manual dispatches run `.github/workflows/android-ci.yml`.

The required job is:

```text
Android CI / Build, test, lint and TS18 contracts
```

It performs:

- JVM unit tests;
- Android lint for the debug variant;
- debug and unsigned release APK assembly;
- source-level TS18 launcher contract validation;
- built-APK package, SDK, launchable-activity, archive and ARM-ABI validation;
- SHA-256 checksum generation;
- upload of test/lint reports and successfully validated APKs.

The release APK produced by CI is intentionally unsigned. Production signing and exact-device runtime validation remain release-owner responsibilities.

The TS18 checks prove static compatibility with the Android 10/API 29 launcher baseline; they do not claim physical head-unit, MCU, CAN, vendor-service or platform-signing validation.
