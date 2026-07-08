# Bash Script Reliability and “Just Run” Engineering Standard

## 1. Objective

A reliable Bash script must:

- run from beginning to end under normal and reasonably degraded conditions;
- distinguish genuine fatal failures from warnings, expected negative results and unavailable optional features;
- never stop merely because a harmless command returned non-zero;
- never hide a failure that invalidates the script’s core result;
- never wait forever for input, a network response, a device, a lock or a child process;
- leave temporary files, mounts, locks and child processes in a known state;
- produce enough progress and summary information to explain what happened;
- be safe to run again after success, interruption or partial failure.

“Just run” does not mean ignoring errors. It means every foreseeable result has an intentional, deterministic policy.

---

## 2. Define the execution contract first

Before implementing commands, define:

1. The script’s essential outcome.
2. Which operations are required for that outcome.
3. Which operations are optional enhancements.
4. Which failures are recoverable.
5. Which operations may block or take an unpredictable amount of time.
6. What constitutes overall success.

Every external command must belong to one of these classes:

### A. Expected condition

A non-zero status is part of normal control flow.

Examples:

- `grep` finds no match;
- a file does not yet exist;
- a process is not running;
- an optional property is unavailable.

Handle it with `if`, `case`, `while`, `until`, `&&` or `||`. Do not report it as an error.

### B. Optional operation

Failure does not invalidate the core result.

Policy:

- record a warning;
- use a fallback where available;
- continue;
- include the warning in the final summary.

### C. Retryable operation

Failure may be temporary.

Examples:

- network requests;
- temporarily busy files;
- delayed device availability.

Policy:

- retry only a bounded number of times;
- use a delay or backoff;
- log each attempt;
- preserve the last useful error;
- continue or fail according to the operation’s importance.

### D. Required operation

Failure makes the requested result incomplete, incorrect or unsafe.

Policy:

- print a precise error;
- perform cleanup;
- stop at a controlled boundary;
- return a documented non-zero status.

### E. Unsafe-to-continue condition

Continuing could modify the wrong target, destroy data or produce a misleading result.

Policy:

- print `STOP:` and the exact reason;
- do not attempt speculative recovery;
- clean up and exit non-zero.

Do not allow the shell’s global options to make these decisions implicitly.

---

## 3. Use the correct interpreter explicitly

Use Bash syntax only with Bash.

For a controlled Linux platform:

    #!/bin/bash

For environments where Bash has a variable installation path, such as Termux:

    #!/usr/bin/env bash

Do not:

- use Bash arrays, `[[ ]]`, process substitution or `mapfile` under `#!/bin/sh`;
- place several flags in the shebang and assume every operating system parses them identically;
- depend on the user launching the script from a particular directory;
- assume the user’s interactive aliases or shell startup files will be loaded.

Determine the script directory when resources are stored beside the script:

    SCRIPT_DIR=$(
        cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 &&
        pwd -P
    ) || {
        printf 'ERROR: Cannot determine script directory.\n' >&2
        exit 1
    }

Use absolute or script-relative paths after that point.

---

## 4. Do not apply “strict mode” blindly

The common line:

    set -euo pipefail

is not a complete reliability strategy. It can introduce sudden exits, context-dependent behaviour and false failures.

### 4.1 `set -e` / `errexit`

Do not enable `set -e` by default in collection, diagnostic, recovery, migration or best-effort scripts.

`set -e` has exceptions involving:

- `if` and `while` conditions;
- `&&` and `||` lists;
- negated commands;
- pipelines;
- functions called from conditional contexts;
- command substitutions and subshells.

The same command can therefore be fatal in one location and ignored in another.

It also treats any non-zero command status as a possible reason to terminate, even where non-zero is meaningful rather than exceptional.

Examples of surprising failures include:

    ((count++))

The arithmetic command returns status 1 when its expression evaluates to zero. With `set -e`, the first increment may terminate the script.

Safer:

    ((count += 1))

or:

    count=$((count + 1))

Use explicit handling:

    command_output=$(some_command)
    rc=$?

    if ((rc != 0)); then
        warn "some_command failed with status ${rc}; continuing with fallback"
        command_output='fallback'
    fi

For a required command:

    if ! required_command; then
        fatal "required_command failed; the requested result cannot be completed"
    fi

Do not use `set -e` as a substitute for reviewing command exit statuses.

### 4.2 `set -u` / `nounset`

`set -u` causes an unset-variable expansion to terminate a non-interactive shell.

This can be useful in small, fully controlled scripts, but it is hazardous where:

- environment variables are optional;
- arrays may be empty;
- cleanup runs after partial initialisation;
- configuration is loaded conditionally;
- positional parameters may be absent.

Prefer explicit defaults and validation:

    output_dir=${OUTPUT_DIR:-"$HOME/output"}
    optional_value=${OPTIONAL_VALUE:-}

    if [[ -z ${required_value:-} ]]; then
        fatal "required_value is not configured"
    fi

Cleanup code must always tolerate variables that were never initialised:

    [[ -n ${tmp_dir:-} ]] && rm -rf -- "$tmp_dir"

Enable `set -u` only after auditing every expansion and every interruption path.

### 4.3 `set -o pipefail`

Without `pipefail`, a pipeline normally reports the status of its final command. With `pipefail`, a non-zero status from an earlier command can fail the pipeline.

This is useful only when failure of any pipeline component invalidates the result.

It can also create false failures where a consumer intentionally stops early, for example:

    producer | head -n 1
    producer | grep -q pattern

The producer may receive a broken-pipe signal after the consumer has already obtained the required result.

Rules:

- enable `pipefail` only after auditing every pipeline;
- avoid pipelines whose individual statuses need different policies;
- capture `PIPESTATUS` immediately when individual statuses matter;
- do not run another command before copying `PIPESTATUS`.

Example:

    producer | transformer | consumer
    pipeline_status=("${PIPESTATUS[@]}")

    if ((pipeline_status[0] != 0)); then
        warn "producer failed with status ${pipeline_status[0]}"
    fi

    if ((pipeline_status[1] != 0)); then
        warn "transformer failed with status ${pipeline_status[1]}"
    fi

    if ((pipeline_status[2] != 0)); then
        fatal "consumer failed with status ${pipeline_status[2]}"
    fi

### 4.4 Recommended default

For reliability-oriented scripts, begin without global automatic-exit behaviour:

    #!/usr/bin/env bash

Then handle failures explicitly.

Options may be enabled in a small, audited scope:

    (
        set -o pipefail
        required_producer | required_consumer
    )
    rc=$?

    if ((rc != 0)); then
        fatal "Required pipeline failed with status ${rc}"
    fi

---

## 5. Check outcomes, not assumptions

Use the command’s exit status as the primary result.

Preferred:

    if mkdir -p -- "$output_dir"; then
        log "Output directory is ready: $output_dir"
    else
        fatal "Cannot create or access output directory: $output_dir"
    fi

Avoid redundant check-then-act patterns:

    if [[ ! -d $output_dir ]]; then
        mkdir "$output_dir"
    fi

The state can change between the check and the action, and `mkdir -p` already expresses the intended idempotent operation.

Check prerequisites once at an appropriate boundary. Do not repeatedly check the same fact before every command unless it can legitimately change during execution.

Good preflight checks include:

- required arguments;
- required commands;
- target identity before destructive work;
- required permissions;
- sufficient essential configuration;
- whether a non-interactive run would otherwise prompt.

Do not:

- require optional commands when a fallback exists;
- reject a system merely because its version string differs;
- perform dozens of speculative checks that do not change behaviour;
- parse human-readable command output when an exit status or machine-readable mode exists.

Prefer capability detection:

    if command -v timeout >/dev/null 2>&1; then
        have_timeout=1
    else
        have_timeout=0
    fi

---

## 6. Capture return statuses immediately

`$?` is overwritten by the next command, including `printf`, `[`, `[[` in some constructions, declarations and logging helpers.

Correct:

    result=$(external_command)
    rc=$?

    if ((rc != 0)); then
        warn "external_command returned ${rc}"
    fi

Do not insert another command between the operation and `rc=$?`.

When assigning command output to a local variable, declare and assign separately:

    local result
    result=$(external_command)
    rc=$?

Do not:

    local result=$(external_command)

The status usually reflects the `local` builtin rather than clearly preserving the command substitution’s result.

Be careful with negation:

    if ! external_command; then
        rc=$?    # This is the status after logical negation, not the original failure.
    fi

Use:

    if external_command; then
        :
    else
        rc=$?
        warn "external_command failed with status ${rc}"
    fi

---

## 7. Never suppress a failure without documenting the policy

Avoid unexplained suppression:

    command || true
    command 2>/dev/null || :
    command || exit 0

These patterns erase information and make failures impossible to diagnose.

Acceptable suppression states why the result is harmless:

    if ! rm -f -- "$optional_cache_file"; then
        warn "Could not remove optional cache file: $optional_cache_file"
    fi

Inside cleanup, where failure must not replace the original status:

    rm -f -- "${lock_file:-}" 2>/dev/null || :

Comment why suppression is safe:

    # Best-effort cleanup only. Preserve the script's original exit status.
    rm -rf -- "$tmp_dir" 2>/dev/null || :

---

## 8. Prevent hangs by design

Any operation that can wait indefinitely must have a stopping condition.

Potential blocking points include:

- network requests;
- device discovery;
- `adb wait-for-device`;
- package managers;
- DNS resolution;
- lock acquisition;
- `read`;
- `wait`;
- FIFOs and pipes;
- mounts;
- commands that unexpectedly request confirmation;
- child processes that ignore termination.

### 8.1 Prefer native timeout controls

Use the program’s own controls where possible.

For example, network clients may provide separate:

- connection timeout;
- read timeout;
- total operation timeout;
- retry count.

Native controls usually report more precise errors than an external watchdog.

### 8.2 Add an outer timeout to genuinely risky commands

Where GNU `timeout` is available:

    timeout --signal=TERM --kill-after=5s 60s risky_command
    rc=$?

    case $rc in
        0)
            log "risky_command completed"
            ;;
        124)
            warn "risky_command exceeded 60 seconds"
            ;;
        137)
            warn "risky_command did not stop after TERM and was killed"
            ;;
        *)
            warn "risky_command failed with status ${rc}"
            ;;
    esac

Do not silently run an operation unbounded when the timeout facility is missing. Either:

- use a tested platform-specific watchdog;
- use the application’s native timeout;
- skip the optional operation with a warning;
- stop before a required but potentially unbounded operation.

### 8.3 Bound polling loops

Bad:

    while ! device_is_ready; do
        sleep 1
    done

Good:

    max_attempts=30
    ready=0

    for ((attempt = 1; attempt <= max_attempts; attempt += 1)); do
        if device_is_ready; then
            ready=1
            break
        fi

        log "Device not ready; attempt ${attempt}/${max_attempts}"
        sleep 1
    done

    if ((ready == 0)); then
        warn "Device did not become ready after ${max_attempts} attempts"
    fi

Every loop must have at least one of:

- finite input;
- a maximum attempt count;
- a deadline;
- a guaranteed progress invariant;
- an externally handled cancellation path.

### 8.4 Prevent hidden prompts

For non-interactive scripts:

- use documented non-interactive options;
- provide required input explicitly;
- redirect stdin from `/dev/null` for commands that must not consume script input;
- detect whether a terminal is available before prompting.

Example:

    if [[ -t 0 && -t 1 ]]; then
        read -r -p "Continue? [y/N] " reply
    else
        reply='N'
        warn "No interactive terminal is available; using the safe default"
    fi

Do not add automatic `-y` confirmation to destructive commands unless the target has already been validated.

### 8.5 Manage background processes explicitly

When starting a background process:

    worker_command &
    worker_pid=$!

Record its PID immediately.

Later:

    if wait "$worker_pid"; then
        log "Worker completed"
    else
        rc=$?
        warn "Worker exited with status ${rc}"
    fi

Do not call bare `wait` unless waiting for every current child is intentional.

Cleanup must terminate owned background processes:

    if [[ -n ${worker_pid:-} ]] && kill -0 "$worker_pid" 2>/dev/null; then
        kill -TERM "$worker_pid" 2>/dev/null || :
        wait "$worker_pid" 2>/dev/null || :
    fi

Never kill processes based only on a broad name match when a recorded PID is available.

---

## 9. Make retries bounded and selective

Retries are appropriate only for failures likely to be transient.

Do not retry:

- invalid arguments;
- missing required files;
- authentication rejection unless credentials may actually refresh;
- unsupported commands;
- syntax errors;
- permission denial that cannot change;
- target identity mismatch.

Reference pattern:

    run_with_retry() {
        local max_attempts=$1
        local delay_seconds=$2
        shift 2

        local attempt
        local rc=1

        for ((attempt = 1; attempt <= max_attempts; attempt += 1)); do
            if "$@"; then
                return 0
            else
                rc=$?
            fi

            if ((attempt < max_attempts)); then
                warn "Attempt ${attempt}/${max_attempts} failed with status ${rc}; retrying"
                sleep "$delay_seconds"
            fi
        done

        return "$rc"
    }

The caller must still decide whether final failure is fatal or optional.

Do not create nested retry layers without a total deadline. Three layers of five retries can unexpectedly produce 125 attempts.

---

## 10. Quote expansions and preserve argument boundaries

Quote variable and command expansions unless intentional splitting or pattern expansion is explicitly required:

    printf '%s\n' "$value"
    cp -- "$source" "$destination"
    command "${arguments[@]}"

Do not:

    cp $source $destination
    for item in $items
    command $flags

Unquoted expansions are subject to word splitting and pathname expansion.

Use `--` before user-controlled or variable path arguments where the command supports it:

    rm -- "$file"
    mv -- "$source" "$destination"

This prevents a filename such as `-rf` from being interpreted as an option.

Use explicit paths for globs:

    for file in ./*; do
        ...
    done

rather than:

    for file in *; do
        ...
    done

Handle unmatched globs intentionally. In Bash, scope `nullglob` where appropriate:

    (
        shopt -s nullglob
        files=("$directory"/*.log)
        process_files "${files[@]}"
    )

Do not globally change glob behaviour unless every use has been audited.

---

## 11. Use arrays for command arguments

Never build a command as one quoted string.

Bad:

    options='--output "/path with spaces" --verbose'
    tool $options

Bad:

    command_string="tool --output '$path'"
    eval "$command_string"

Good:

    options=(
        --output "$path"
        --verbose
    )

    tool "${options[@]}"

Arrays preserve each argument exactly, including spaces, wildcard characters and empty strings.

Avoid `eval`. It introduces a second parsing pass, makes quoting difficult to reason about and can convert data into executable shell syntax.

---

## 12. Handle filenames as arbitrary data

Filenames may contain:

- spaces;
- tabs;
- wildcard characters;
- leading hyphens;
- quotes;
- backslashes;
- newlines.

Do not parse `ls`.

Bad:

    for file in $(ls "$directory"); do
        ...
    done

For globs:

    for file in "$directory"/*; do
        [[ -e $file ]] || continue
        process_file "$file"
    done

For recursive traversal, use NUL delimiters:

    while IFS= read -r -d '' file; do
        process_file "$file"
    done < <(find "$directory" -type f -print0)

Use:

    while IFS= read -r line; do
        ...
    done < "$input_file"

`read -r` prevents backslashes from being consumed as escape characters.

Do not use command substitution for binary data. Command substitution also removes trailing newline characters.

---

## 13. Avoid pipeline subshell surprises

A loop placed on the right side of a pipe commonly runs in a subshell. Variable changes may disappear when the pipeline ends.

Bad:

    found=0

    producer | while IFS= read -r line; do
        found=1
    done

    printf '%s\n' "$found"    # May still print 0.

Use process substitution:

    found=0

    while IFS= read -r line; do
        found=1
    done < <(producer)

Or use `mapfile`/`readarray` when retaining all records is appropriate:

    mapfile -t lines < <(producer)

Do not enable obscure shell options merely to change pipeline execution semantics unless the whole script depends on and documents them.

---

## 14. Keep functions predictable

Each function should:

- perform one coherent task;
- accept inputs through arguments;
- use local variables;
- return only an exit status;
- send returned data to stdout only when that is the function’s documented interface;
- send logs and diagnostics to stderr;
- avoid unexpectedly changing the caller’s directory, shell options, traps or global variables.

Pattern:

    inspect_target() {
        local target=$1
        local result
        local rc

        result=$(inspection_command -- "$target")
        rc=$?

        if ((rc != 0)); then
            error "Inspection failed for: $target"
            return "$rc"
        fi

        printf '%s\n' "$result"
        return 0
    }

Use a subshell for temporary directory changes:

    (
        cd -- "$work_dir" || exit 1
        perform_work
    )
    rc=$?

This prevents an unsuccessful or forgotten `cd` reversal from corrupting later paths.

Use a `main` function for non-trivial scripts:

    main() {
        parse_arguments "$@" || return
        preflight || return
        perform_work || return
        print_summary
    }

    main "$@"
    exit $?

Do not place hidden executable statements between function definitions.

---

## 15. Separate data output from diagnostics

Use stdout for requested machine-readable data.

Use stderr for:

- progress;
- warnings;
- errors;
- debug traces;
- human-readable summaries when stdout is an export stream.

Logging helpers:

    log() {
        printf '[INFO] %s\n' "$*" >&2
    }

    warn() {
        printf '[WARN] %s\n' "$*" >&2
        ((warning_count += 1))
    }

    error() {
        printf '[ERROR] %s\n' "$*" >&2
        ((error_count += 1))
    }

    fatal() {
        printf '[FATAL] %s\n' "$*" >&2
        exit 1
    }

Do not place variables directly in a `printf` format string:

    printf "$message\n"

Use:

    printf '%s\n' "$message"

A variable containing `%s`, `%n` or backslash sequences must remain data, not formatting instructions.

---

## 16. Provide visible progress without noise

Long-running scripts should announce meaningful stage boundaries:

    [1/6] Validating environment
    [2/6] Collecting system state
    [3/6] Inspecting files
    [4/6] Running optional diagnostics
    [5/6] Writing results
    [6/6] Finalising

For operations of unpredictable duration, print periodic progress based on:

- files processed;
- bytes transferred;
- attempts completed;
- elapsed time;
- current stage.

Avoid printing every successful low-level command. Excessive output hides the useful failure.

A script should never appear frozen merely because it is waiting normally.

---

## 17. Use secure temporary files and deterministic cleanup

Use `mktemp` or an equivalent secure facility. Do not create predictable temporary names using only `$$`, timestamps or usernames.

Bad:

    tmp_file="/tmp/my-script.$$"

Good, using syntax verified for the target platform:

    tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/my-script.XXXXXXXX") || {
        fatal "Cannot create temporary directory"
    }

Never use `mktemp -u` followed by separate file creation. That reintroduces a race between selecting the name and creating it.

Register cleanup immediately after acquiring the resource.

Reference pattern:

    tmp_dir=''
    worker_pid=''

    cleanup() {
        local rc=$?

        trap - EXIT INT TERM

        if [[ -n ${worker_pid:-} ]] &&
           kill -0 "$worker_pid" 2>/dev/null; then
            kill -TERM "$worker_pid" 2>/dev/null || :
            wait "$worker_pid" 2>/dev/null || :
        fi

        if [[ -n ${tmp_dir:-} && -d ${tmp_dir:-} ]]; then
            rm -rf -- "$tmp_dir" 2>/dev/null || :
        fi

        exit "$rc"
    }

    trap cleanup EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM

Cleanup rules:

- preserve the original exit status;
- tolerate partial initialisation;
- never assume a variable was set;
- never make cleanup failure replace the primary failure;
- remove only resources created or owned by this invocation;
- avoid broad wildcard deletion;
- disable traps before explicitly exiting from the cleanup handler.

Do not use an `ERR` trap as the primary control-flow system. Its triggering rules largely mirror `set -e` and contain the same contextual exceptions.

---

## 18. Make file updates atomic where practical

Do not partially overwrite an important output or configuration file.

Preferred sequence:

1. Create a secure temporary file in the destination filesystem.
2. Write the complete new content.
3. Validate the temporary content.
4. Apply required permissions.
5. Rename it over the destination.
6. Remove it during cleanup if any earlier step fails.

Conceptual pattern:

    temp_output=$(mktemp "${destination}.tmp.XXXXXXXX") || {
        fatal "Cannot create temporary output beside $destination"
    }

    if generate_content > "$temp_output" &&
       validate_content "$temp_output" &&
       chmod --reference="$destination" "$temp_output" 2>/dev/null; then
        mv -f -- "$temp_output" "$destination" || {
            fatal "Cannot replace destination: $destination"
        }
        temp_output=''
    else
        fatal "New content could not be generated or validated"
    fi

The exact `mktemp` and permission commands must be tested on the target platform.

For append-only logs, write complete records with one `printf` where possible to reduce interleaving.

---

## 19. Design for interruption and reruns

A script may stop because of:

- Ctrl+C;
- termination by a service manager;
- lost terminal session;
- reboot;
- low storage;
- network loss;
- process kill;
- dependency crash.

The next run must be able to distinguish:

- completed state;
- incomplete temporary state;
- stale lock;
- valid checkpoint;
- unsafe ambiguous state.

Prefer idempotent operations:

    mkdir -p
    rm -f
    ln -sfn
    install -D
    update only when content differs

Do not append duplicate configuration blindly.

Use checkpoints only where repeating prior work is expensive or unsafe. A checkpoint must be written only after its associated stage is fully complete.

Do not mistake the existence of an output file for proof that it is complete. Validate size, format, checksum or completion metadata as appropriate.

Locks must contain enough information to identify their owner. Handle stale locks conservatively rather than deleting every existing lock automatically.

---

## 20. Validate destructive targets immediately before use

Destructive operations require stronger guards than read-only collection.

Before `rm -rf`, formatting, flashing, overwriting or recursive permission changes:

- require a non-empty target;
- canonicalise it where possible;
- reject `/`, `.`, `..` and known parent directories;
- verify expected target identity;
- use `--`;
- print the resolved target;
- ensure the operation is within the allowed root.

Example:

    delete_tree_safely() {
        local target=$1
        local allowed_root=$2

        [[ -n $target ]] || {
            error "Refusing deletion: target is empty"
            return 1
        }

        [[ $target != / && $target != . && $target != .. ]] || {
            error "Refusing deletion of unsafe target: $target"
            return 1
        }

        case $target in
            "$allowed_root"/*)
                ;;
            *)
                error "Refusing deletion outside allowed root: $target"
                return 1
                ;;
        esac

        rm -rf -- "$target"
    }

Do not weaken destructive safety guards merely to make a script continue.

---

## 21. Control environment-dependent behaviour

Do not depend unnecessarily on:

- the caller’s current directory;
- aliases;
- locale-specific output;
- interactive shell settings;
- inherited `IFS`;
- an arbitrary `PATH`;
- terminal width;
- colour support;
- user-specific command configuration.

Set `IFS` locally when reading:

    while IFS= read -r line; do
        ...
    done

When parsing output that is unavoidably locale-dependent, scope the locale:

    result=$(LC_ALL=C external_command)

Do not globally force a locale unless every operation has been tested under it.

Use a conservative `umask` when creating sensitive files:

    umask 077

Do not overwrite `PATH` with a narrow fixed value unless the deployment environment is fully controlled. Add required paths deliberately or resolve dependencies during preflight.

---

## 22. Use exit statuses as a documented API

Recommended top-level meanings:

- `0`: the script’s essential outcome was achieved;
- `1`: an operational failure prevented the essential outcome;
- `2`: invalid invocation or arguments.

Warnings about optional operations may still produce status `0` when the promised result is valid.

Do not return non-zero merely because:

- an optional tool was absent;
- an expected file was not present;
- a best-effort cleanup failed;
- an informational probe was denied;
- one fallback failed but another succeeded.

Do not repurpose conventional shell statuses casually:

- `126`: command found but not executable;
- `127`: command not found;
- values greater than 128 commonly indicate signal termination;
- GNU `timeout` commonly uses `124` for expiry and `137` after forced kill.

Functions return status codes, not arbitrary data. Status values are limited. Return structured data through stdout, files or named variables.

---

## 23. Always print a final summary

A collection or multi-stage script should end with a concise summary even when some optional stages failed.

Example:

    ==================================================
    RESULT: COMPLETED WITH WARNINGS
    Required stages completed: 7
    Optional stages skipped:   2
    Warnings:                  3
    Required failures:         0
    Output directory:          /path/to/results
    Log file:                  /path/to/run.log
    ==================================================

Use one of:

- `SUCCESS`;
- `COMPLETED WITH WARNINGS`;
- `FAILED`;
- `STOPPED FOR SAFETY`;
- `INTERRUPTED`.

Identify:

- what completed;
- what did not;
- whether the core result is usable;
- where outputs and logs were written;
- any exact manual action still required.

Do not finish silently after a long operation.

---

## 24. Provide opt-in debugging

Normal runs should be readable. Detailed tracing should be optional.

Example:

    if [[ ${DEBUG:-0} == 1 ]]; then
        PS4='+ ${BASH_SOURCE##*/}:${LINENO}:${FUNCNAME[0]:-main}: '
        set -x
    fi

Never enable `set -x` unconditionally in a script that handles:

- passwords;
- tokens;
- cookies;
- private keys;
- signed URLs;
- sensitive environment variables.

Disable tracing around sensitive commands if debugging may be enabled:

    { set +x; } 2>/dev/null
    sensitive_command "$secret"
    if [[ ${DEBUG:-0} == 1 ]]; then
        set -x
    fi

---

## 25. Required validation before delivery

Every non-trivial script must pass:

    bash -n script.sh
    shellcheck script.sh

A ShellCheck warning may be suppressed only when:

- the author understands the warning;
- the behaviour is intentional;
- the suppression is narrowly scoped;
- a comment explains why.

Test on the actual minimum supported Bash version and target operating system.

Required test cases should include:

- normal successful run;
- second run over existing state;
- path containing spaces;
- path beginning with `-`;
- empty input;
- empty glob;
- missing optional command;
- missing required command;
- permission-denied output;
- read-only destination;
- failed network operation;
- timeout;
- interrupted run;
- no interactive terminal;
- stdin redirected from a file or pipe;
- partial output from a prior run;
- cleanup after failure;
- cleanup before all variables are initialised;
- child-process failure;
- unexpected non-zero status in every pipeline component.

For scripts processing filenames, also test tabs, wildcard characters, quotes, backslashes and newlines.

---

## 26. Complexity limit

Bash is appropriate when the script primarily coordinates commands and performs limited data manipulation.

Consider a more structured language when the script requires substantial:

- nested state management;
- concurrency;
- structured data processing;
- protocol implementation;
- complex retry graphs;
- transaction management;
- error-object propagation;
- parsing of JSON, XML or binary formats without dedicated tools.

Do not compensate for excessive complexity with more traps, global flags, `eval`, generated shell code or deeply nested conditionals.

A short Bash launcher around a reliable Python or compiled implementation is often more dependable than several hundred lines of stateful shell logic.

---

## 27. Recommended reference structure

    #!/usr/bin/env bash

    # No blanket set -e or set -u.
    # Enable pipefail only for audited pipelines or scoped subshells.

    readonly SCRIPT_NAME=${0##*/}

    warning_count=0
    error_count=0
    tmp_dir=''
    worker_pid=''

    log() {
        printf '[INFO] %s\n' "$*" >&2
    }

    warn() {
        printf '[WARN] %s\n' "$*" >&2
        ((warning_count += 1))
    }

    error() {
        printf '[ERROR] %s\n' "$*" >&2
        ((error_count += 1))
    }

    cleanup() {
        local rc=$?

        trap - EXIT INT TERM

        if [[ -n ${worker_pid:-} ]] &&
           kill -0 "$worker_pid" 2>/dev/null; then
            kill -TERM "$worker_pid" 2>/dev/null || :
            wait "$worker_pid" 2>/dev/null || :
        fi

        if [[ -n ${tmp_dir:-} && -d ${tmp_dir:-} ]]; then
            rm -rf -- "$tmp_dir" 2>/dev/null || :
        fi

        exit "$rc"
    }

    print_summary() {
        local result=$1

        printf '\n' >&2
        printf '========================================\n' >&2
        printf 'RESULT:   %s\n' "$result" >&2
        printf 'WARNINGS: %d\n' "$warning_count" >&2
        printf 'ERRORS:   %d\n' "$error_count" >&2
        printf '========================================\n' >&2
    }

    require_command() {
        local command_name=$1

        if ! command -v "$command_name" >/dev/null 2>&1; then
            error "Required command not found: $command_name"
            return 1
        fi
    }

    preflight() {
        local failed=0

        require_command find || failed=1
        require_command mktemp || failed=1

        if ((failed != 0)); then
            return 1
        fi

        return 0
    }

    run_optional_probe() {
        local output
        local rc

        output=$(optional_probe 2>&1)
        rc=$?

        if ((rc != 0)); then
            warn "Optional probe failed with status ${rc}: ${output}"
            return 0
        fi

        printf '%s\n' "$output"
        return 0
    }

    perform_required_work() {
        if required_command; then
            return 0
        else
            local rc=$?
            error "Required command failed with status ${rc}"
            return "$rc"
        fi
    }

    main() {
        trap cleanup EXIT
        trap 'exit 130' INT
        trap 'exit 143' TERM

        log "[1/4] Validating environment"

        if ! preflight; then
            print_summary "FAILED"
            return 1
        fi

        tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/${SCRIPT_NAME}.XXXXXXXX")
        if [[ -z $tmp_dir || ! -d $tmp_dir ]]; then
            error "Cannot create temporary working directory"
            print_summary "FAILED"
            return 1
        fi

        log "[2/4] Running required work"

        if ! perform_required_work; then
            print_summary "FAILED"
            return 1
        fi

        log "[3/4] Running optional diagnostics"
        run_optional_probe || :

        log "[4/4] Finalising"

        if ((warning_count > 0)); then
            print_summary "COMPLETED WITH WARNINGS"
        else
            print_summary "SUCCESS"
        fi

        return 0
    }

    main "$@"

---

## 28. Agent/developer acceptance checklist

Before declaring a script final, verify all of the following:

- [ ] The interpreter matches the syntax used.
- [ ] The script does not depend on the caller’s current directory.
- [ ] `set -euo pipefail` was not added mechanically.
- [ ] Every non-zero result has an intentional classification.
- [ ] Expected negative checks are not reported as failures.
- [ ] Optional failures warn and continue.
- [ ] Required failures stop through a controlled path.
- [ ] Return statuses are captured immediately.
- [ ] No unexplained `|| true`, `|| :` or broad stderr suppression exists.
- [ ] Every potentially blocking operation is bounded.
- [ ] Every polling or retry loop has a maximum.
- [ ] Background PIDs are recorded and explicitly waited for.
- [ ] Interruptions trigger safe cleanup.
- [ ] Cleanup preserves the original exit status.
- [ ] Cleanup tolerates partial initialisation.
- [ ] All variable expansions used as arguments are quoted.
- [ ] Arrays are used for argument lists.
- [ ] `eval` is absent unless technically unavoidable and independently reviewed.
- [ ] User-controlled paths are preceded by `--` where supported.
- [ ] Recursive filename processing is NUL-delimited.
- [ ] No `ls` output is parsed.
- [ ] Pipeline subshell behaviour has been considered.
- [ ] Temporary files are created securely.
- [ ] Important file replacement is atomic where practical.
- [ ] Destructive targets have explicit safety validation.
- [ ] Operations are idempotent or have recovery checkpoints.
- [ ] Logs identify meaningful stages and failures.
- [ ] A final success/warning/failure summary is always produced.
- [ ] Debug tracing is opt-in and does not expose secrets.
- [ ] `bash -n` passes.
- [ ] ShellCheck passes or every suppression is narrowly justified.
- [ ] The script was tested on the actual target environment.
- [ ] The script was tested with missing tools, failed commands, timeouts and interruption.
- [ ] The script can be rerun safely after a partial run.

The governing principle is:

    Explicit policy beats implicit shell behaviour.

A reliable script does not merely continue at all costs and does not terminate at every anomaly. It knows which result matters, bounds every uncertain wait, preserves evidence, cleans up what it owns and reports exactly what succeeded.
