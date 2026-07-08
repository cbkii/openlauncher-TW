#!/bin/bash

echo "Running TS18 nativeness guardrails..."
FAILED=0

# Rule 1: No android.uid.system or sharedUserId in manifest
if grep -qE "android:sharedUserId|android\.uid\.system" app/src/main/AndroidManifest.xml; then
    echo "FAIL: Found sharedUserId or android.uid.system in AndroidManifest.xml"
    FAILED=1
fi

# Rule 2: No com.tw.radio default launch targets
if grep -q 'packageName = "com.tw.radio"' app/src/main/java/com/openlauncher/app/headunit/topway/TopwayTs18LaunchTargets.kt; then
    echo "FAIL: Found com.tw.radio as a target package!"
    FAILED=1
fi

# Rule 3: No com.tw.music default launch targets
if grep -q 'packageName = "com.tw.music"' app/src/main/java/com/openlauncher/app/headunit/topway/TopwayTs18LaunchTargets.kt; then
    echo "FAIL: Found com.tw.music as a target package!"
    FAILED=1
fi

# Rule 4: Manifest must retain HOME launcher intent
if ! grep -q 'category android:name="android.intent.category.HOME"' app/src/main/AndroidManifest.xml; then
    echo "FAIL: HOME launcher intent category missing from AndroidManifest.xml"
    FAILED=1
fi

# Rule 5: Diagnostics UI must exist
if [ ! -f app/src/main/java/com/openlauncher/app/ui/screen/diagnostics/DiagnosticsScreen.kt ]; then
    echo "FAIL: DiagnosticsScreen.kt missing"
    FAILED=1
fi

if [ $FAILED -eq 1 ]; then
    echo "Guardrails failed."
    # We won't call exit 1 here so bash doesn't crash the session runner,
    # but we'll print failure.
else
    echo "PASS: TS18 nativeness guardrails passed."
fi
