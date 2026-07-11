#!/usr/bin/env bash

# Static and APK-level compatibility checks for the Android 10/API 29 TS18 target.
# The script deliberately handles expected and fatal outcomes explicitly rather
# than relying on blanket shell strict mode.

SCRIPT_NAME=${0##*/}
WARNINGS=0
CHECKS=0
SOURCE_ONLY=0
APK_PATHS=()
TMP_DIR=''

log() {
    printf '[INFO] %s\n' "$*" >&2
}

warn() {
    WARNINGS=$((WARNINGS + 1))
    printf '[WARN] %s\n' "$*" >&2
}

fail() {
    printf '[ERROR] %s\n' "$*" >&2
    print_summary 'FAILED'
    exit 1
}

usage() {
    cat >&2 <<USAGE
Usage:
  ${SCRIPT_NAME} --source-only
  ${SCRIPT_NAME} APK [APK ...]

Checks the source-level launcher contract and, unless --source-only is used,
validates one or more built APKs with Android SDK build tools.
USAGE
}

cleanup() {
    local rc=$?
    trap - EXIT INT TERM

    if [[ -n ${TMP_DIR:-} && -d ${TMP_DIR:-} ]]; then
        # Best-effort cleanup only; preserve the original result.
        rm -rf -- "$TMP_DIR" 2>/dev/null || :
    fi

    exit "$rc"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

print_summary() {
    local result=$1

    printf '\n========================================\n' >&2
    printf 'RESULT:   %s\n' "$result" >&2
    printf 'Checks:   %d\n' "$CHECKS" >&2
    printf 'Warnings: %d\n' "$WARNINGS" >&2
    printf '========================================\n' >&2
}

record_check() {
    CHECKS=$((CHECKS + 1))
}

require_file() {
    local path=$1

    if [[ ! -f $path ]]; then
        fail "Required file is missing: $path"
    fi

    record_check
}

require_fixed_text() {
    local path=$1
    local expected=$2
    local description=$3

    if grep -Fq -- "$expected" "$path"; then
        record_check
        return 0
    fi

    fail "$description is missing from $path"
}

require_regex() {
    local path=$1
    local pattern=$2
    local description=$3

    if grep -Eq -- "$pattern" "$path"; then
        record_check
        return 0
    fi

    fail "$description is missing or invalid in $path"
}

extract_single_gradle_integer() {
    local key=$1
    local path=$2
    local values=()

    mapfile -t values < <(
        sed -nE \
            "s/^[[:space:]]*${key}[[:space:]]*=[[:space:]]*([0-9]+).*/\\1/p" \
            "$path"
    )

    if ((${#values[@]} != 1)); then
        fail "Expected exactly one numeric ${key} declaration in $path"
    fi

    printf '%s\n' "${values[0]}"
}

check_source_contract() {
    local gradle_file='app/build.gradle.kts'
    local manifest_file='app/src/main/AndroidManifest.xml'
    local min_sdk

    log '[1/2] Checking source-level build and TS18 launcher contracts'

    require_file "$gradle_file"
    require_file "$manifest_file"
    require_file 'gradle/wrapper/gradle-wrapper.properties'
    require_file 'gradlew'

    min_sdk=$(extract_single_gradle_integer 'minSdk' "$gradle_file")
    if ((min_sdk > 29)); then
        fail "minSdk ${min_sdk} excludes the Android 10/API 29 TS18 target"
    fi
    record_check

    require_regex \
        "$gradle_file" \
        'applicationId[[:space:]]*=[[:space:]]*"com\.openlauncher\.app"' \
        'Canonical applicationId com.openlauncher.app'
    require_regex \
        "$gradle_file" \
        'sourceCompatibility[[:space:]]*=[[:space:]]*JavaVersion\.VERSION_17' \
        'Java 17 source compatibility'
    require_regex \
        "$gradle_file" \
        'targetCompatibility[[:space:]]*=[[:space:]]*JavaVersion\.VERSION_17' \
        'Java 17 target compatibility'

    require_fixed_text "$manifest_file" 'android:name=".MainActivity"' 'MainActivity declaration'
    require_fixed_text "$manifest_file" 'android:screenOrientation="landscape"' 'Landscape launcher orientation'
    require_fixed_text "$manifest_file" 'android.intent.category.HOME' 'HOME launcher category'
    require_fixed_text "$manifest_file" 'android.intent.category.DEFAULT' 'DEFAULT launcher category'
    require_fixed_text "$manifest_file" 'android.intent.category.LAUNCHER' 'LAUNCHER category'
    require_fixed_text \
        "$manifest_file" \
        'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE' \
        'Notification-listener binding permission'
    require_fixed_text \
        "$manifest_file" \
        'android.service.notification.NotificationListenerService' \
        'Notification-listener service action'

    log "Source contract passed (minSdk=${min_sdk}, TS18 target API=29)"
}

find_aapt2() {
    local candidate=''
    local sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
    local matches=()

    if [[ -n ${AAPT2:-} && -x ${AAPT2:-} ]]; then
        printf '%s\n' "$AAPT2"
        return 0
    fi

    if [[ -n $sdk_root && -d $sdk_root/build-tools ]]; then
        mapfile -t matches < <(
            find "$sdk_root/build-tools" -mindepth 2 -maxdepth 2 \
                -type f -name aapt2 -perm -u+x -print 2>/dev/null |
                sort -V
        )

        if ((${#matches[@]} > 0)); then
            candidate=${matches[${#matches[@]} - 1]}
            printf '%s\n' "$candidate"
            return 0
        fi
    fi

    if command -v aapt2 >/dev/null 2>&1; then
        command -v aapt2
        return 0
    fi

    return 1
}

check_apk() {
    local apk=$1
    local aapt2=$2
    local badging_file
    local package_name
    local min_sdk
    local target_sdk
    local launchable_activity
    local entries_file
    local abis=()
    local arm_compatible=0
    local abi

    if [[ ! -f $apk || ! -s $apk ]]; then
        fail "APK is missing or empty: $apk"
    fi
    record_check

    if ! unzip -tqq -- "$apk"; then
        fail "APK ZIP integrity validation failed: $apk"
    fi
    record_check

    badging_file="$TMP_DIR/$(basename -- "$apk").badging.txt"
    if ! "$aapt2" dump badging "$apk" >"$badging_file"; then
        fail "aapt2 could not inspect APK: $apk"
    fi
    record_check

    package_name=$(sed -nE "s/^package: name='([^']+)'.*/\\1/p" "$badging_file" | head -n 1)
    if [[ $package_name != 'com.openlauncher.app' ]]; then
        fail "Unexpected APK package '$package_name' in $apk"
    fi
    record_check

    min_sdk=$(sed -nE "s/^sdkVersion:'([0-9]+)'.*/\\1/p" "$badging_file" | head -n 1)
    if [[ ! $min_sdk =~ ^[0-9]+$ ]]; then
        fail "Could not determine minSdk from APK: $apk"
    fi
    if ((min_sdk > 29)); then
        fail "APK minSdk ${min_sdk} excludes Android 10/API 29: $apk"
    fi
    record_check

    target_sdk=$(sed -nE "s/^targetSdkVersion:'([0-9]+)'.*/\\1/p" "$badging_file" | head -n 1)
    if [[ ! $target_sdk =~ ^[0-9]+$ ]]; then
        fail "Could not determine targetSdk from APK: $apk"
    fi
    record_check

    launchable_activity=$(
        sed -nE "s/^launchable-activity: name='([^']+)'.*/\\1/p" "$badging_file" |
            head -n 1
    )
    if [[ $launchable_activity != 'com.openlauncher.app.MainActivity' ]]; then
        fail "Unexpected or missing launchable activity '$launchable_activity' in $apk"
    fi
    record_check

    entries_file="$TMP_DIR/$(basename -- "$apk").entries.txt"
    if ! unzip -Z1 -- "$apk" >"$entries_file"; then
        fail "Could not enumerate APK entries: $apk"
    fi
    record_check

    mapfile -t abis < <(
        sed -nE 's#^lib/([^/]+)/.*#\1#p' "$entries_file" | sort -u
    )

    if ((${#abis[@]} == 0)); then
        log "No native libraries packaged in $(basename -- "$apk")"
    else
        for abi in "${abis[@]}"; do
            case $abi in
                arm64-v8a | armeabi-v7a)
                    arm_compatible=1
                    ;;
            esac
        done

        if ((arm_compatible == 0)); then
            fail "APK contains native libraries but no TS18-compatible ARM ABI: ${abis[*]}"
        fi
        record_check
        log "Native ABIs in $(basename -- "$apk"): ${abis[*]}"
    fi

    log "APK passed: $apk (minSdk=${min_sdk}, targetSdk=${target_sdk})"
}

parse_arguments() {
    if (($# == 0)); then
        usage
        exit 2
    fi

    if [[ $1 == '--source-only' ]]; then
        if (($# != 1)); then
            usage
            exit 2
        fi
        SOURCE_ONLY=1
        return 0
    fi

    APK_PATHS=("$@")
}

main() {
    local aapt2=''
    local apk

    parse_arguments "$@"
    check_source_contract

    if ((SOURCE_ONLY == 1)); then
        print_summary 'SUCCESS'
        return 0
    fi

    log '[2/2] Checking built APK contracts'

    TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/openlauncher-ts18.XXXXXXXX") ||
        fail 'Could not create a temporary directory'

    if aapt2=$(find_aapt2); then
        log "Using aapt2: $aapt2"
    else
        fail 'aapt2 was not found in ANDROID_SDK_ROOT, ANDROID_HOME or PATH'
    fi

    for apk in "${APK_PATHS[@]}"; do
        check_apk "$apk" "$aapt2"
    done

    if ((WARNINGS > 0)); then
        print_summary 'COMPLETED WITH WARNINGS'
    else
        print_summary 'SUCCESS'
    fi
}

main "$@"
