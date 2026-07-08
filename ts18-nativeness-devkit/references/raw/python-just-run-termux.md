# Python “Just Run” Reliability Standard for Android Termux

## Purpose

Use this document when writing, reviewing, repairing, or finalising Python scripts intended to run in Android Termux.

The goal is predictable execution, not maximum strictness. A reliable script should:

- avoid spurious failures and unnecessary early exits;
- distinguish expected conditions from real failures;
- bound subprocesses, network requests, retries, waits, queues and locks;
- preserve the last valid state until replacements are complete;
- degrade gracefully when optional features fail;
- stop when destructive safety cannot be proven;
- recover safely after interruption or Android process termination;
- report exactly what completed, failed, or was skipped;
- be safe to run again.

The governing principle is:

> **Explicit policy beats broad exception handling.**

A “just run” script does not continue at all costs and does not abort at every anomaly. It handles each foreseeable outcome intentionally.

---

# 1. Priority order

When requirements conflict, use this order:

1. **Safety and target correctness**
2. **Validity of the required result**
3. **Recovery after interruption**
4. **Bounded execution**
5. **Clear diagnostics**
6. **Graceful degradation**
7. **Convenience**
8. **Compactness or stylistic elegance**

Never weaken a destructive safety guard merely to make a script continue.

---

# 2. Classify every operation

Before coding, classify each meaningful operation.

## Expected condition

The result is normal control flow.

Examples:

- optional file absent;
- search returns no matches;
- process not running;
- Android property unavailable;
- optional configuration omitted.

Policy:

- handle the specific result;
- do not print a traceback;
- do not count it as an error;
- continue through the intended branch.

## Optional operation

Failure does not invalidate the core result.

Policy:

- catch only expected exceptions;
- warn once with useful context;
- use a fallback or skip the feature;
- continue;
- include the warning in the final summary.

## Retryable operation

Failure may be temporary.

Policy:

- retry only known transient failures;
- cap attempts and total elapsed time;
- use bounded delay or backoff;
- preserve the final error;
- stop retrying permanent failures immediately.

## Required operation

Failure prevents a correct essential result.

Policy:

- raise a domain-specific operational exception;
- preserve the original cause;
- clean up owned resources where possible;
- stop at a controlled top-level boundary;
- return a documented non-zero status.

## Unsafe-to-continue condition

Continuing could modify the wrong target, delete unrelated data, write invalid state, or produce misleading output.

Policy:

- report `STOP:` and the exact reason;
- do not attempt speculative recovery;
- preserve evidence;
- return a distinct non-zero status.

---

# 3. Termux execution model

Termux is not a conventional desktop Linux distribution.

Assume:

- Python runs natively on Android;
- Android uses bionic rather than glibc;
- Termux uses a non-standard prefix;
- the app runs under Android sandboxing and SELinux;
- commands available in `adb shell` may behave differently from the Termux app UID;
- Android may pause or kill the process without allowing Python cleanup.

Common paths are:

```text
HOME=/data/data/com.termux/files/home
PREFIX=/data/data/com.termux/files/usr
```

Do not hard-code these throughout the script:

```python
from pathlib import Path
import os

HOME = Path.home()
PREFIX = Path(os.environ.get("PREFIX", "/data/data/com.termux/files/usr"))
```

## Private storage versus shared storage

Keep these in Termux private storage:

- scripts;
- virtual environments;
- temporary files;
- locks;
- databases;
- checkpoints;
- authoritative configuration;
- incomplete archives;
- active working directories.

Treat Android shared storage mainly as import/export storage:

```text
/sdcard
/storage/emulated/0
~/storage/shared
```

Shared storage may not reliably support normal POSIX behaviour for permissions, ownership, symlinks, sockets, locks, case sensitivity, exact timestamps, or atomic replacement.

Preferred workflow:

1. Read or copy input from shared storage.
2. Work in private Termux storage.
3. Validate completed output.
4. Export the final result.
5. Keep checkpoint and recovery state private.

Do not place virtual environments or live SQLite databases on shared storage.

---

# 4. Interpreter and entry point

For a Termux-only script:

```python
#!/data/data/com.termux/files/usr/bin/python
```

For a portable script normally invoked explicitly:

```python
#!/usr/bin/env python3
```

Do not assume `/usr/bin/python3` exists in Termux.

Do not rely on direct execution from shared storage. Prefer:

```bash
python script.py
```

Declare the minimum Python version actually required:

```python
import sys

MIN_PYTHON = (3, 11)

if sys.version_info < MIN_PYTHON:
    print(
        f"ERROR: Python {MIN_PYTHON[0]}.{MIN_PYTHON[1]} or newer is required; "
        f"found {sys.version.split()[0]}",
        file=sys.stderr,
    )
    raise SystemExit(1)
```

Use one controlled entry point:

```python
def main(argv: list[str] | None = None) -> int:
    ...
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

Do not perform significant work at import time. Import-time code must not modify files, access the network, run subprocesses, request root, parse arguments, prompt, acquire locks, start threads, or modify Android state.

Helper functions should return results or raise exceptions. They should not call `sys.exit()`.

---

# 5. Arguments and configuration

Use `argparse` for non-trivial command-line interfaces.

Parse and validate arguments before side effects:

```python
import argparse
from pathlib import Path


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Collect and package diagnostics.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path.cwd() / "output",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=60.0,
    )
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--debug", action="store_true")
    return parser
```

Validate semantics explicitly:

```python
if args.timeout <= 0:
    parser.error("--timeout must be greater than zero")
```

Do not use `assert` for user input, configuration, path safety, permissions, target identity, or runtime availability. Assertions may be disabled with `python -O`.

Defaults are appropriate only for omitted optional settings. Do not silently replace malformed required configuration with defaults.

---

# 6. Exception-handling rules

## Catch narrowly

Preferred:

```python
try:
    data = path.read_text(encoding="utf-8")
except FileNotFoundError:
    data = default_data
except PermissionError as exc:
    raise OperationalError(
        f"Cannot read {path}: permission denied"
    ) from exc
```

Avoid:

```python
try:
    data = path.read_text()
except Exception:
    data = default_data
```

Broad handlers can hide programming defects, encoding errors, resource exhaustion, API changes, path mistakes, and failures inside fallback code.

Catch an exception only where the code can recover, classify it, add context, or perform required cleanup.

Preserve causes:

```python
try:
    config = json.loads(text)
except json.JSONDecodeError as exc:
    raise ConfigError(
        f"Invalid JSON in {config_path} at "
        f"line {exc.lineno}, column {exc.colno}"
    ) from exc
```

Do not catch `BaseException` during normal operation. A bare handler also catches `KeyboardInterrupt` and `SystemExit`.

A bare or `BaseException` handler is acceptable only for minimal cleanup followed by immediate re-raise:

```python
try:
    ...
except BaseException:
    cleanup_best_effort()
    raise
```

Never suppress an active exception accidentally:

```python
# Wrong: return in finally can hide the real failure.
try:
    perform_work()
finally:
    return 0
```

Avoid unexplained patterns such as:

```python
except Exception:
    pass
```

or ambiguous fallbacks such as returning `None` for every failure.

---

# 7. Domain-specific exceptions

Use a small hierarchy:

```python
class ScriptError(Exception):
    """Base class for expected script failures."""


class ConfigError(ScriptError):
    """Invalid invocation or configuration."""


class OperationalError(ScriptError):
    """Required work could not be completed."""


class SafetyStop(ScriptError):
    """Continuing would be unsafe."""
```

Top-level policy:

```python
try:
    return run(args)
except SafetyStop as exc:
    logger.critical("STOP: %s", exc)
    return 3
except ConfigError as exc:
    logger.error("%s", exc)
    return 2
except OperationalError as exc:
    logger.error("%s", exc)
    return 1
except Exception:
    logger.exception("Unexpected internal failure")
    return 70
```

Expected operational failures should not emit unnecessary tracebacks. Unexpected `TypeError`, `AttributeError`, `IndexError`, or invariant failures should remain visible as programming defects.

---

# 8. Preflight without over-checking

Perform preflight once for stable facts:

- supported Python version;
- required arguments;
- required modules;
- required external commands;
- writable private work directory;
- required input;
- root availability where genuinely required.

Recheck only facts that may change:

- removable storage;
- device connection;
- network state;
- free space;
- target identity immediately before destructive work.

Do not fail because an optional command is absent when a fallback exists.

Prefer capability detection:

```python
import shutil

if shutil.which("tar") is None:
    logger.warning("tar unavailable; using Python tarfile fallback")
```

For optional modules, isolate the import:

```python
try:
    import optional_module
except ImportError:
    optional_module = None
```

Do not wrap unrelated initialisation in the same `ImportError` handler.

---

# 9. Dependency policy

Operational scripts must not silently modify their environment.

Do not automatically run during normal execution:

```text
pkg upgrade
apt install ...
pip install ...
pip install --upgrade ...
```

Provide a separate explicit bootstrap step.

Use:

```bash
python -m pip
```

rather than bare `pip`.

Keep virtual environments under private Termux storage:

```bash
python -m venv "$HOME/.venvs/project-name"
"$HOME/.venvs/project-name/bin/python" -m pip install ...
```

Pin dependency ranges or exact versions sufficiently for reproducibility.

Do not silently start long source builds because an Android wheel is unavailable.

---

# 10. Environment variables

Treat environment variables as optional, untrusted input.

Prefer:

```python
raw_output = os.environ.get("OUTPUT_DIR")
output = Path(raw_output) if raw_output else default_output
```

Validate values:

```python
raw_timeout = os.environ.get("SCRIPT_TIMEOUT", "60")

try:
    timeout = float(raw_timeout)
except ValueError as exc:
    raise ConfigError(
        f"SCRIPT_TIMEOUT must be numeric, not {raw_timeout!r}"
    ) from exc

if timeout <= 0:
    raise ConfigError("SCRIPT_TIMEOUT must be greater than zero")
```

Normally copy the current environment for subprocesses:

```python
child_env = os.environ.copy()
child_env["LC_ALL"] = "C"
```

A minimal replacement environment can accidentally remove `PATH`, `HOME`, `PREFIX`, loader settings, or credentials intentionally provided by the caller.

For a specific Android command, sanitise only that command’s environment.

---

# 11. Bound every uncertain wait

Every potentially blocking operation must have a timeout, deadline, maximum attempts, finite input, or documented cancellation mechanism.

Potential blockers include:

- subprocesses;
- network and DNS;
- sockets;
- `input()`;
- device discovery;
- locks;
- queues;
- `Thread.join()`;
- `Future.result()`;
- `Process.join()`;
- named pipes;
- stream reads;
- archive extraction;
- filesystem walks over unavailable mounts;
- native extension calls.

No loop may depend on “eventually”.

Use `time.monotonic()` for deadlines:

```python
deadline = time.monotonic() + timeout

while True:
    if condition_is_ready():
        break

    remaining = deadline - time.monotonic()

    if remaining <= 0:
        raise TimeoutError(
            f"Condition was not met within {timeout:.1f} seconds"
        )

    time.sleep(min(0.5, remaining))
```

Do not use wall-clock time for elapsed-time control.

Retries must be selective, finite, visible, and covered by a total deadline.

Do not retry invalid configuration, syntax errors, unsupported operations, deterministic permission denial, malformed data, missing required files, or target identity mismatch.

---

# 12. Interactive input

A script intended for unattended use must not unexpectedly prompt.

Before `input()`:

```python
if not sys.stdin.isatty() or not sys.stdout.isatty():
    raise ConfigError(
        "Confirmation is required, but no interactive terminal is available"
    )
```

Support explicit modes such as:

```text
--yes
--non-interactive
--dry-run
```

Do not default to “yes” for destructive work.

For subprocesses that must not prompt or consume script input:

```python
stdin=subprocess.DEVNULL
```

---

# 13. Subprocess rules

Use argument lists:

```python
subprocess.run(
    ["getprop", "ro.build.version.release"],
    ...
)
```

Do not build shell strings unless shell syntax is genuinely required:

```python
# Avoid.
subprocess.run(
    f"getprop {property_name}",
    shell=True,
)
```

Default to `shell=False`. Avoid `os.system()` and `os.popen()`.

Use `check=True` only when every non-zero status means failure.

For meaningful non-zero statuses:

```python
completed = subprocess.run(
    ["grep", "-q", pattern, filename],
    check=False,
    timeout=10,
)

if completed.returncode == 0:
    found = True
elif completed.returncode == 1:
    found = False
else:
    raise OperationalError(
        f"grep failed with status {completed.returncode}"
    )
```

Do not treat stderr output alone as failure.

Always bound uncertain commands with `timeout=`.

When using `Popen` with pipes, use `communicate(timeout=...)`. Do not call `wait()` while unread stdout or stderr may fill.

Do not capture unbounded output in memory. Redirect large output to a private file and validate it.

For text output, specify:

```python
text=True,
encoding="utf-8",
errors="replace",
```

Use bytes when exact binary output matters.

---

# 14. Terminate subprocess trees

A child may create descendants. Killing only the direct child can leave workers, locks, mounts, listeners, or privileged helpers.

Start the child in its own session:

```python
process = subprocess.Popen(
    args,
    start_new_session=True,
    stdin=subprocess.DEVNULL,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True,
    encoding="utf-8",
    errors="replace",
)
```

On timeout, terminate the process group, wait briefly, then kill if necessary.

Do not call `killpg()` unless the child was intentionally placed in a dedicated process group or session.

Do not kill by broad process-name matching when a PID or process group is known.

---

# 15. Root operations

Do not run the entire Python script as root merely because one operation requires root.

Preferred sequence:

1. Run Python as the normal Termux user.
2. Inspect and validate state.
3. Build one narrow privileged operation.
4. Invoke root only for that operation.
5. Verify the result as the normal user.

`su -c` normally accepts a shell command string. Do not interpolate untrusted data into it.

Avoid:

```python
command = f"rm -rf {user_path}"
subprocess.run(["su", "-c", command])
```

Prefer fixed root helpers, secure temporary files, absolute paths, strict target validation, and minimal privilege scope.

Do not run Termux package management as root.

Consider ownership changes caused by root-created files under `$HOME`.

---

# 16. Termux and Android commands

Termux binaries normally live under `$PREFIX/bin`. Android system binaries normally live under `/system/bin`.

Do not assume a basename resolves to the intended implementation.

Use `shutil.which()` for Termux commands.

Use absolute Android paths when identity matters:

```text
/system/bin/am
/system/bin/pm
/system/bin/settings
/system/bin/getprop
```

Termux loader and PATH variables can interfere with Android commands. Scope any sanitised environment to the specific command:

```python
android_env = os.environ.copy()
android_env["PATH"] = "/system/bin"
android_env.pop("LD_PRELOAD", None)
android_env.pop("LD_LIBRARY_PATH", None)
```

Permission denial under the Termux UID is not automatically retryable.

---

# 17. Path and filename handling

Use `pathlib.Path`.

Do not depend on the caller’s working directory unless the interface explicitly says so.

Distinguish script-relative resources, caller-relative input, Termux-private state, temporary state, and shared-storage exports.

Avoid global `os.chdir()`. Prefer explicit paths and subprocess `cwd=`.

Before destructive work, resolve and contain targets:

```python
resolved_target = target.expanduser().resolve(strict=False)
resolved_root = allowed_root.expanduser().resolve(strict=True)

if resolved_target == resolved_root:
    raise SafetyStop(
        f"Refusing to operate on allowed root itself: {resolved_target}"
    )

if not resolved_target.is_relative_to(resolved_root):
    raise SafetyStop(
        f"Target is outside allowed root: {resolved_target}"
    )
```

Also reject empty paths, `/`, `.`, `..`, `$HOME`, `$PREFIX`, shared-storage roots, and ambiguous unresolved targets unless explicitly intended.

Do not use string-prefix checks as the sole containment test.

Treat filenames as arbitrary strings. Do not parse `ls`. Do not concatenate filenames into shell commands. Use Python APIs or subprocess argument lists.

Do not follow symlinks recursively unless required. Bound traversal by root, depth, filesystem, item count, or time where appropriate.

---

# 18. Text and binary data

Specify encodings for persistent text:

```python
path.read_text(encoding="utf-8")
path.write_text(text, encoding="utf-8", newline="\n")
```

For diagnostics containing arbitrary bytes:

```python
text = path.read_text(
    encoding="utf-8",
    errors="replace",
)
```

Use `errors="replace"` only when exact byte preservation is unnecessary.

Do not use `errors="ignore"` unless data loss is explicitly acceptable and reported.

Use binary mode for exact round-tripping.

Successful parsing does not prove semantic validity. Validate types, required keys, ranges, and schema version.

---

# 19. Atomic file replacement

Do not write directly over important files.

Required pattern:

1. Create a temporary file in the destination directory.
2. Write complete content.
3. Flush Python buffers.
4. `fsync()` when durability matters.
5. Validate the temporary file.
6. Apply permissions where supported.
7. Replace using `os.replace()`.
8. Optionally fsync the parent directory.
9. Remove the temporary file on failure.

Reference:

```python
def atomic_write_bytes(path: Path, data: bytes) -> None:
    path = path.expanduser()
    path.parent.mkdir(parents=True, exist_ok=True)

    fd, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.",
        suffix=".tmp",
        dir=path.parent,
    )
    temporary_path = Path(temporary_name)

    try:
        with os.fdopen(fd, "wb") as file:
            file.write(data)
            file.flush()
            os.fsync(file.fileno())

        os.replace(temporary_path, path)

    except BaseException:
        try:
            temporary_path.unlink(missing_ok=True)
        except OSError:
            pass
        raise
```

The temporary file must be on the same filesystem as the destination.

Do not assume shared storage provides equivalent atomicity or durability.

For archives, create under a temporary name, close fully, reopen and validate, optionally checksum, then replace or export.

---

# 20. Temporary resources and cleanup

Use `tempfile.TemporaryDirectory()`, `NamedTemporaryFile()`, `mkstemp()`, or `mkdtemp()`.

Do not construct predictable temporary names from PID or timestamp alone.

Prefer private Termux storage as the temporary root.

Use context managers for files, temporary directories, databases, locks, sockets, archives, HTTP responses, and subprocess resources.

Do not rely on garbage collection or `__del__()` for important cleanup.

Cleanup must:

- tolerate partial initialisation;
- remove only owned resources;
- be idempotent;
- avoid indefinite waits;
- avoid network dependencies;
- not obscure the primary exception;
- not convert failure to success;
- not convert success to failure for harmless cleanup problems.

Do not rely on `atexit` for essential correctness. It may not run after SIGKILL, Android forced termination, fatal crashes, reboot, or `os._exit()`.

---

# 21. Signals and Android termination

Signal handlers should perform minimal work. Do not do file I/O, logging, lock acquisition, waits, or complex cleanup inside a handler.

Use a simple flag:

```python
stop_requested = False
received_signal: int | None = None


def handle_signal(signum: int, frame: FrameType | None) -> None:
    del frame

    global stop_requested
    global received_signal

    stop_requested = True
    received_signal = signum
```

Long-running loops must check the flag. Blocking operations still need timeouts.

Recommended statuses:

- `130` for Ctrl+C or SIGINT;
- `143` for SIGTERM where represented.

Python cannot catch SIGKILL. Android may kill Termux processes because of memory pressure, battery policy, phantom-process limits, force-stop, reboot, or OEM process management.

Therefore:

- write versioned checkpoints;
- make stages idempotent;
- use temporary names for incomplete output;
- write completion markers only after validation;
- avoid keeping the only valid state in memory;
- make rerun recovery explicit.

A wake lock is not a substitute for checkpoints.

---

# 22. Idempotence, checkpoints, and locks

Running a script twice must not duplicate configuration, database records, exports, or destructive transformations.

For configuration:

1. parse current state;
2. normalise;
3. apply the intended update;
4. validate;
5. serialise once;
6. replace atomically.

Do not infer completion solely from file existence. Validate format, expected members, size, checksum, target identity, or completion metadata.

A checkpoint means all effects represented by it are complete and validated.

Write checkpoints atomically and include a schema or script version.

On restart:

1. parse checkpoint;
2. validate schema;
3. verify referenced output;
4. resume only from a completed boundary;
5. restart incomplete stages;
6. stop for review when state is contradictory.

Keep locks in private storage. Record PID, run ID, start time, and script identity. Do not assume PID alone proves ownership. Remove only demonstrably stale locks; otherwise stop and report ambiguity.

---

# 23. Concurrency

Use sequential execution unless concurrency provides clear value.

Concurrency adds race conditions, hidden exceptions, shutdown complexity, memory pressure, and Android process pressure.

Bound workers conservatively:

```python
max_workers = min(4, max(1, os.cpu_count() or 1))
```

Always consume future results:

```python
with concurrent.futures.ThreadPoolExecutor(
    max_workers=max_workers
) as executor:
    futures = [
        executor.submit(process_item, item)
        for item in items
    ]

    for future in concurrent.futures.as_completed(futures):
        result = future.result()
```

Apply timeouts to futures, joins, queues, locks, and waits.

Python cannot safely force-kill a thread. Use cooperative cancellation. Use a subprocess when hard termination is genuinely required.

Test `multiprocessing` on the actual Termux and Python build before relying on it.

---

# 24. Network operations

Every network operation must have explicit time bounds.

Where supported, define connection timeout, TLS timeout, read timeout, total deadline, retry count, and maximum response size.

Do not use `requests.get(url)` without timeouts.

Example:

```python
requests.get(
    url,
    timeout=(10, 30),
)
```

Retry selectively. Do not repeatedly retry authentication rejection, malformed input, deterministic 4xx failures, or invalid configuration.

Stream large downloads, cap total bytes, write to a temporary file, then validate before replacing an existing valid file.

---

# 25. Memory, storage, and archives

Avoid loading unbounded data into memory:

```python
# Avoid for large inputs.
data = huge_file.read_bytes()
lines = huge_file.read_text().splitlines()
items = list(very_large_generator)
```

Prefer streaming:

```python
with path.open("rb") as file:
    while chunk := file.read(1024 * 1024):
        process(chunk)
```

Apply limits to file size, decompressed size, archive members, logs, subprocess output, JSON, queues, directory entries, and retries.

Writes may fail during write, flush, fsync, close, rename, or archive finalisation. Handle `errno.ENOSPC` explicitly and preserve the previous valid output.

Before extracting archives:

1. reject absolute paths;
2. normalise member paths;
3. ensure members remain under the extraction root;
4. reject or explicitly handle symlinks;
5. cap member count and expanded size;
6. extract privately;
7. validate contents;
8. move or export only accepted results.

Do not use unrestricted `extractall()` on untrusted archives.

---

# 26. Logging and progress

Use stdout for requested machine-readable data and stderr for progress, warnings, errors, and summaries.

```python
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    stream=sys.stderr,
)
```

Do not mix progress text into JSON, CSV, or another stdout data stream.

Use parameterised logging:

```python
logger.info("Processing %s", path)
```

Use `logger.exception()` only inside an exception handler.

Do not log passwords, cookies, tokens, API keys, private keys, authorisation headers, credential-bearing URLs, unredacted environment dumps, or sensitive command arguments.

Long-running scripts should report stage boundaries:

```text
[1/6] Validating environment
[2/6] Inspecting inputs
[3/6] Collecting data
[4/6] Processing results
[5/6] Writing output
[6/6] Validating and finalising
```

For long stages, report items, bytes, elapsed time, retry count, or current phase. Do not flood output with every successful low-level action.

Support an explicit `--debug` mode. Debug mode must not expose secrets or change core control flow.

---

# 27. Exit statuses and summary

Recommended statuses:

- `0`: essential result completed successfully;
- `1`: expected operational failure;
- `2`: invalid invocation or configuration;
- `3`: stopped because continuing would be unsafe;
- `70`: unexpected internal software failure;
- `130`: interrupted by SIGINT;
- `143`: terminated by SIGTERM where represented.

Warnings may still return `0` when the required result is valid.

Do not return `0` when required output is missing, validation failed, output is incomplete, destructive state is ambiguous, or an unexpected exception occurred.

Every multi-stage script should finish with one of:

- `SUCCESS`;
- `COMPLETED WITH WARNINGS`;
- `FAILED`;
- `STOPPED FOR SAFETY`;
- `INTERRUPTED`.

Example:

```text
==================================================
RESULT:                    COMPLETED WITH WARNINGS
Required stages complete: 6
Optional stages skipped:  2
Warnings:                 3
Errors:                   0
Output:                   /path/to/output
Detailed log:             /path/to/run.log
==================================================
```

Do not report success merely because no exception escaped. Validate the promised result first.

---

# 28. Android system-change workflow

For Android, root, Magisk, or configuration scripts, use this order:

1. Inspect current state.
2. Validate target identity.
3. Record before-state.
4. Build the plan.
5. Report intended change.
6. Apply one logical change.
7. Verify immediate result.
8. Record restart or reboot requirement.
9. Checkpoint the completed stage.
10. Continue only when verification permits it.

Do not apply unrelated changes in parallel.

Do not continue applying later changes after a required verification fails.

Do not clean evidence before the root cause is captured.

Classify each change as requiring no restart, app restart, service restart, zygote restart, or full reboot.

Do not claim success solely because a write command returned `0`.

---

# 29. Reusable subprocess helper

Use this only for commands with bounded, reasonably small output:

```python
from __future__ import annotations

import os
import signal
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping, Sequence


@dataclass(frozen=True)
class CommandResult:
    args: tuple[str, ...]
    returncode: int
    stdout: str
    stderr: str


class CommandError(OperationalError):
    pass


def run_command(
    args: Sequence[str | os.PathLike[str]],
    *,
    timeout: float,
    ok_codes: frozenset[int] = frozenset({0}),
    cwd: Path | None = None,
    env: Mapping[str, str] | None = None,
) -> CommandResult:
    if timeout <= 0:
        raise ValueError("timeout must be greater than zero")

    command = tuple(os.fspath(arg) for arg in args)

    if not command:
        raise ValueError("args must not be empty")

    try:
        process = subprocess.Popen(
            command,
            cwd=cwd,
            env=dict(env) if env is not None else None,
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
            errors="replace",
            start_new_session=True,
        )
    except FileNotFoundError as exc:
        raise CommandError(
            f"Command not found: {command[0]}"
        ) from exc
    except OSError as exc:
        raise CommandError(
            f"Could not start {command[0]}: {exc}"
        ) from exc

    try:
        stdout, stderr = process.communicate(timeout=timeout)
    except subprocess.TimeoutExpired as exc:
        try:
            os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass

        try:
            stdout, stderr = process.communicate(timeout=5)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass

            stdout, stderr = process.communicate()

        raise CommandError(
            f"Command exceeded {timeout:.1f} seconds: {command[0]}"
        ) from exc

    result = CommandResult(
        args=command,
        returncode=process.returncode,
        stdout=stdout,
        stderr=stderr,
    )

    if result.returncode not in ok_codes:
        detail = result.stderr.strip() or result.stdout.strip()

        if len(detail) > 2000:
            detail = detail[:2000] + "\n...<truncated>"

        message = (
            f"Command failed with status {result.returncode}: "
            f"{command[0]}"
        )

        if detail:
            message += f"\n{detail}"

        raise CommandError(message)

    return result
```

For large output, redirect to a private file instead of capturing it.

---

# 30. Recommended top-level structure

```python
#!/data/data/com.termux/files/usr/bin/python

from __future__ import annotations

import argparse
import logging
import signal
import sys
from dataclasses import dataclass
from pathlib import Path
from types import FrameType
from typing import Sequence


LOGGER = logging.getLogger("script")

EXIT_SUCCESS = 0
EXIT_OPERATIONAL_FAILURE = 1
EXIT_USAGE = 2
EXIT_SAFETY_STOP = 3
EXIT_INTERNAL_ERROR = 70
EXIT_INTERRUPTED = 130
EXIT_TERMINATED = 143

stop_requested = False
received_signal: int | None = None


class ScriptError(Exception):
    """Base class for expected script failures."""


class ConfigError(ScriptError):
    """Invalid invocation or configuration."""


class OperationalError(ScriptError):
    """Required work could not be completed."""


class SafetyStop(ScriptError):
    """Continuing would be unsafe."""


@dataclass
class RunSummary:
    required_completed: int = 0
    optional_completed: int = 0
    optional_skipped: int = 0
    warnings: int = 0
    errors: int = 0
    output: Path | None = None

    def warning(self, message: str, *args: object) -> None:
        self.warnings += 1
        LOGGER.warning(message, *args)

    def error(self, message: str, *args: object) -> None:
        self.errors += 1
        LOGGER.error(message, *args)


def handle_signal(
    signum: int,
    frame: FrameType | None,
) -> None:
    del frame

    global stop_requested
    global received_signal

    stop_requested = True
    received_signal = signum


def check_stop() -> None:
    if not stop_requested:
        return

    if received_signal == signal.SIGTERM:
        raise InterruptedError("Termination requested")

    raise KeyboardInterrupt


def configure_logging(debug: bool) -> None:
    logging.basicConfig(
        level=logging.DEBUG if debug else logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        stream=sys.stderr,
        force=True,
    )


def run(
    args: argparse.Namespace,
    summary: RunSummary,
) -> int:
    # Preflight.
    # Required stages.
    # Optional stages.
    # Result validation.
    return EXIT_SUCCESS


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    configure_logging(args.debug)

    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    summary = RunSummary()
    result = "FAILED"
    exit_status = EXIT_OPERATIONAL_FAILURE

    try:
        exit_status = run(args, summary)
        result = (
            "COMPLETED WITH WARNINGS"
            if summary.warnings
            else "SUCCESS"
        )

    except KeyboardInterrupt:
        result = "INTERRUPTED"
        exit_status = EXIT_INTERRUPTED
        LOGGER.warning("Interrupted by user")

    except InterruptedError:
        result = "INTERRUPTED"
        exit_status = EXIT_TERMINATED
        LOGGER.warning("Termination requested")

    except SafetyStop as exc:
        result = "STOPPED FOR SAFETY"
        exit_status = EXIT_SAFETY_STOP
        summary.error("STOP: %s", exc)

    except ConfigError as exc:
        result = "FAILED"
        exit_status = EXIT_USAGE
        summary.error("%s", exc)

    except OperationalError as exc:
        result = "FAILED"
        exit_status = EXIT_OPERATIONAL_FAILURE
        summary.error("%s", exc)

    except Exception:
        result = "FAILED"
        exit_status = EXIT_INTERNAL_ERROR
        summary.errors += 1
        LOGGER.exception("Unexpected internal failure")

    finally:
        print_summary(result, summary)
        logging.shutdown()

    return exit_status


if __name__ == "__main__":
    raise SystemExit(main())
```

This is a reference structure. Remove abstractions the actual script does not need.

---

# 31. Validation before delivery

Every non-trivial script must pass:

```bash
python -m py_compile script.py
```

For a package:

```bash
python -m compileall -q package_directory
```

Run the project test suite.

Where adopted by the project, also use:

```bash
ruff check .
ruff format --check .
mypy package_or_script
```

Do not add multiple overlapping tools merely to appear strict.

Also test:

```bash
python -X dev -W default script.py ...
```

Test both diagnostic and normal production modes.

---

# 32. Required test scenarios

Test on the actual supported Termux and Android environment.

At minimum test:

- normal success;
- second run over existing state;
- Ctrl+C;
- SIGTERM;
- abrupt kill followed by rerun;
- missing optional dependency;
- missing required dependency;
- missing optional and required files;
- malformed configuration;
- permission denial;
- read-only output;
- low storage where practical;
- shared-storage input;
- private-storage work directory;
- path containing spaces;
- path beginning with `-`;
- Unicode filename;
- no network;
- DNS failure;
- connect and read timeout;
- subprocess timeout;
- expected and unexpected non-zero status;
- command not found;
- child process with descendants;
- large stdout or stderr;
- no TTY;
- redirected stdin;
- closed stdout consumer;
- stale lock;
- partial checkpoint;
- partial archive;
- cleanup before full initialisation;
- cleanup failure;
- optional failure after required success;
- unexpected internal exception;
- Android system command from Termux;
- root command with changed `HOME` or `PATH`;
- rerun after root-owned output.

For destructive scripts also test:

- empty target;
- `/`;
- parent traversal;
- symlink escape;
- allowed-root boundary;
- target identity mismatch;
- stale state from an older script version.

---

# 33. Agent acceptance checklist

Before declaring a script final, verify:

## Runtime and storage

- [ ] Supported Python version is declared.
- [ ] Interpreter policy works in Termux.
- [ ] The script does not assume `/usr/bin/python`.
- [ ] Scripts, venvs, locks, databases, checkpoints, and working state remain private.
- [ ] Shared storage is used mainly for import/export.
- [ ] The script was tested in real Termux.

## Structure and failure policy

- [ ] Significant work does not occur at import time.
- [ ] `main()` controls execution.
- [ ] Helper functions do not call `sys.exit()`.
- [ ] Arguments are parsed before side effects.
- [ ] Assertions are not used for runtime safety.
- [ ] Expected, optional, retryable, required, and unsafe outcomes are distinguished.
- [ ] Exceptions are caught narrowly.
- [ ] Causes are preserved with `raise ... from ...`.
- [ ] Unexpected defects retain tracebacks.
- [ ] Optional failures cannot falsely fail the run.
- [ ] Required failures cannot be mistaken for success.

## Waiting, subprocesses, and privilege

- [ ] Every uncertain subprocess has a timeout.
- [ ] Every network request has explicit time bounds.
- [ ] Every polling loop and retry policy is bounded.
- [ ] Hidden prompts are prevented.
- [ ] Commands use argument lists.
- [ ] `shell=True` is absent or independently justified.
- [ ] Large output is streamed rather than captured.
- [ ] Timed-out descendants are terminated where necessary.
- [ ] Root is requested only for narrow operations.
- [ ] Untrusted data is not interpolated into `su -c`.
- [ ] Android and Termux binaries are resolved intentionally.

## Filesystem and recovery

- [ ] Paths use `Path`.
- [ ] Caller working directory is not assumed.
- [ ] Destructive targets are resolved and contained.
- [ ] String prefix is not the sole containment check.
- [ ] `ls` output is not parsed.
- [ ] Encodings are explicit.
- [ ] Important files use atomic replacement where supported.
- [ ] Temporary resources are securely created.
- [ ] Context managers own resources.
- [ ] Cleanup tolerates partial initialisation.
- [ ] Cleanup cannot obscure the primary exception.
- [ ] Essential correctness does not depend on `atexit`.
- [ ] SIGKILL and Android forced termination are assumed possible.
- [ ] Checkpoints are atomic and written only after validated stages.
- [ ] Operations are idempotent or transactional.
- [ ] The next run can identify incomplete state.

## Resource use and diagnostics

- [ ] Concurrency is bounded.
- [ ] Future and worker exceptions are consumed.
- [ ] Inputs, output, archives, logs, and memory use are bounded.
- [ ] Low-storage failure preserves the previous valid result.
- [ ] Archive paths and expanded sizes are validated.
- [ ] Logs do not contain secrets.
- [ ] stdout remains clean for machine-readable output.
- [ ] Long stages show meaningful progress.
- [ ] Final status and exit code are explicit.
- [ ] Required output is validated before success is reported.
- [ ] The script can be rerun safely after partial completion.
- [ ] `python -m py_compile` and automated tests pass.

---

# 34. Final rules for agents

When generating or revising a Python script for Termux:

1. Inspect before modifying.
2. Make one logical change at a time.
3. Treat optional failures as warnings, not fatal errors.
4. Treat unsafe ambiguity as a hard stop.
5. Do not catch broadly merely to keep going.
6. Do not add retries unless the failure is genuinely transient.
7. Do not leave any uncertain wait unbounded.
8. Do not capture unbounded output into memory.
9. Do not overwrite important files in place.
10. Do not keep authoritative working state on shared storage.
11. Do not rely on cleanup after SIGKILL.
12. Do not run the entire script as root without necessity.
13. Do not interpolate untrusted data into shell commands.
14. Do not claim success until the required result is validated.
15. Always make rerun behaviour explicit and safe.

The final engineering standard is:

> **Bound every uncertain wait. Preserve the last valid state. Catch only what you understand. Keep authoritative work in private Termux storage. Assume Android can terminate the process at any point.**
