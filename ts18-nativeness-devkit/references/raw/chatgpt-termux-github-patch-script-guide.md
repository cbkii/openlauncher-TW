# ChatGPT Guide: Reliable GitHub Patch Scripts for Pixel 9a Termux

## Purpose

Use this guide when asking ChatGPT to create Bash or Python scripts that patch, refactor, validate, commit, and push GitHub repository changes from **native Android Termux on Pixel 9a**.

The target outcome is a script that **runs to completion without hanging**, preserves user work, gives visible progress, handles expected failures intentionally, and leaves the repository in a clear state.

This guide is optimised for tasks such as:

- applying repo patches;
- editing GitHub Actions workflows;
- updating CI scripts;
- repairing Gradle/Android project configuration;
- resolving PR review comments;
- staging, committing, and pushing changes;
- generating documentation/templates alongside code changes;
- validating shell/YAML/Gradle-adjacent changes without running unsuitable local CI.

The governing principle is:

> **Explicit policy beats implicit shell behaviour.**

A reliable patch script should not blindly continue and should not stop at harmless conditions. It must know which result matters, bound every uncertain wait, preserve evidence, and print exactly what happened.

---

## Pixel 9a Termux assumptions

Assume the script runs in **native Termux**, not a desktop Linux machine.

Common paths:

```text
HOME=/data/data/com.termux/files/home
PREFIX=/data/data/com.termux/files/usr
```

Preferred working style:

- keep scripts, temporary state, logs, checkpoints, and repo work under Termux private storage;
- use `~/tmp`, not `/tmp`;
- avoid Android shared storage for active repo work;
- avoid interactive prompts;
- avoid commands that may open editors or pagers;
- keep output visible;
- use timeouts around all external commands that may block;
- use conservative validation locally and leave heavy CI proof to GitHub Actions unless explicitly requested.

Do not assume:

- `/usr/bin/python3` exists;
- `/tmp` exists or behaves normally;
- `timeout`, `ruby`, `python`, `gh`, `jq`, or Gradle are installed;
- the user has a clean working tree;
- a direct `./gradlew` invocation is safe on Termux;
- Git authentication can prompt successfully.

---

## Required ChatGPT behaviour before writing a patch script

Before producing a script, ChatGPT should reason through the repository task and classify:

1. **Required outcome** — what must be changed for the script to be useful.
2. **Optional improvements** — useful additions that must not make the script fail if unavailable.
3. **Unsafe conditions** — states where continuing could damage user work or push the wrong branch.
4. **Validation scope** — which checks are fast, local, and meaningful on Termux.
5. **CI-only validation** — which checks must be left to GitHub Actions.
6. **Push behaviour** — whether to push current branch, create a branch, or only commit locally.
7. **Existing user work policy** — whether staged files are preserved, included, or blocked.

For GitHub patch scripts, the default policy should be:

- do **not** require a clean working tree unless the patch would be unsafe with existing changes;
- preserve existing staged files;
- include all staged files in the final commit when the user requested that;
- stage only intended patch files unless the user asked to stage all changes;
- commit only when there are staged changes;
- push to `origin/<current-branch>` only after confirming the branch name is valid and non-empty;
- disable interactive Git prompts for push;
- print a final summary even if optional validation is skipped.

---

## Bash script construction rules

### 1. Interpreter

Use Bash explicitly:

```bash
#!/usr/bin/env bash
```

Do not use Bash syntax under `#!/bin/sh`.

### 2. Do not use blanket strict mode mechanically

Avoid this as a default for complex patch scripts:

```bash
set -euo pipefail
```

It can cause sudden exits when a harmless command returns non-zero, such as `grep` finding no match or `git diff --quiet` reporting a difference.

Prefer explicit handling:

```bash
if run_required 60s git rev-parse --show-toplevel; then
  :
else
  fail "Not inside a Git repository"
fi
```

Use scoped `pipefail` only where every pipeline component matters.

### 3. Use `~/tmp`, not `/tmp`

Always create a private temp root:

```bash
TMP_ROOT="${HOME}/tmp/chatgpt-patch-${SCRIPT_NAME:-script}.$$"
mkdir -p -- "$TMP_ROOT" || fail "Could not create temp root: $TMP_ROOT"
```

Clean only resources the script created.

### 4. Visible progress

Every stage should print progress:

```text
[1/8] Inspecting repository
[2/8] Applying patch files
[3/8] Rewriting workflows
...
```

Do not run long steps silently.

### 5. Bound every uncertain command

Any external command that can block must run through a timeout helper.

Required command helper:

```bash
run_required() {
  local seconds=$1
  shift

  log "RUN(required, ${seconds}): $*"

  if command -v timeout >/dev/null 2>&1; then
    timeout --foreground "$seconds" "$@"
  else
    warn "timeout unavailable; running required command without outer timeout: $*"
    "$@"
  fi

  local rc=$?
  if ((rc != 0)); then
    error "Required command failed with status ${rc}: $*"
    return "$rc"
  fi

  return 0
}
```

Optional command helper:

```bash
run_optional() {
  local seconds=$1
  shift

  log "RUN(optional, ${seconds}): $*"

  if command -v timeout >/dev/null 2>&1; then
    timeout --foreground "$seconds" "$@" || {
      warn "Optional command failed or timed out: $*"
      return 0
    }
  else
    warn "timeout unavailable; skipping optional command to avoid unbounded wait: $*"
    return 0
  fi
}
```

For noncritical inspection commands, use `timeout || true` semantics through `run_optional`, for example:

```bash
run_optional 20s git diff --stat
run_optional 20s git status --short
run_optional 20s git log --oneline -5
```

### 6. Do not swallow output

Avoid redirecting command output to `/dev/null` unless there is a clear reason and the final summary preserves enough evidence.

Preferred:

```bash
run_required 60s git status --short
```

Acceptable for noisy optional probes:

```bash
if run_optional 10s command -v shellcheck; then
  run_optional 120s shellcheck scripts/*.sh
fi
```

### 7. Prevent hidden prompts

Set Git non-interactive behaviour for scripts that push:

```bash
export GIT_TERMINAL_PROMPT=0
export GCM_INTERACTIVE=never
```

Avoid commands that invoke editors:

- no bare `git commit` without `-m`;
- no `git rebase -i`;
- no `gh pr create` unless all fields are supplied and prompts are disabled;
- no commands that open pagers.

Use:

```bash
export GIT_PAGER=cat
export PAGER=cat
```

### 8. Preserve staged files when requested

If the user says “push changes along with all other staged files”, do not reset the index.

Good:

```bash
# Keep existing staged files.
git add path/changed-by-this-script docs/new-file.md

if git diff --cached --quiet; then
  warn "No staged changes to commit"
else
  run_required 90s git commit -m "ci: improve patch workflow"
fi
```

Bad:

```bash
git reset
```

unless the user explicitly requested a reset.

### 9. Avoid clean-tree assumptions

Do not block just because there are uncommitted changes unless patching would be ambiguous.

Safe inspection:

```bash
if ! run_optional 20s git status --short; then
  warn "Could not inspect status; continuing because status is noncritical"
fi
```

Unsafe case:

```bash
if file_has_local_conflict_risk; then
  fail "Refusing to overwrite locally modified critical file: $path"
fi
```

### 10. Validate before pushing

At minimum:

```bash
run_required 10s bash -n "$0"
run_required 60s find scripts -type f -name '*.sh' -print -exec bash -n {} ';'
```

For YAML workflows, use available tools opportunistically:

```bash
if command -v ruby >/dev/null 2>&1; then
  run_required 60s ruby -e 'require "yaml"; ARGV.each { |f| Psych.safe_load(File.read(f), aliases: false); puts "OK #{f}" }' .github/workflows/*.yml
else
  warn "ruby unavailable; skipping YAML parse validation"
fi
```

For Gradle/Android repositories, do not run heavy Gradle tasks on Termux unless specifically requested and known reliable. Prefer workflow-aligned scoped tasks or skip local Gradle validation with a clear note.

---

## Python script construction rules for Termux

Use Python when the patch is complex enough that Bash string manipulation becomes fragile.

### Interpreter

Portable:

```python
#!/usr/bin/env python3
```

Termux-only:

```python
#!/data/data/com.termux/files/usr/bin/python
```

### Structure

Use a single entry point:

```python
def main(argv=None) -> int:
    ...
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
```

Do not perform file writes, network access, Git operations, or argument parsing at import time.

### Subprocesses

Always use argument lists, timeouts, and visible output.

```python
import subprocess

completed = subprocess.run(
    ["git", "status", "--short"],
    check=False,
    timeout=20,
    text=True,
)
```

Do not use:

```python
subprocess.run("git status --short", shell=True)
```

unless shell syntax is genuinely required.

### Atomic writes

Write important files through a same-directory temporary file, then replace:

```python
from pathlib import Path
import os
import tempfile


def atomic_write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=path.parent)
    tmp = Path(tmp_name)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
            fh.flush()
            os.fsync(fh.fileno())
        os.replace(tmp, path)
    except BaseException:
        try:
            tmp.unlink(missing_ok=True)
        except OSError:
            pass
        raise
```

### Exception policy

Catch only what you understand.

Good:

```python
try:
    text = path.read_text(encoding="utf-8")
except FileNotFoundError:
    text = ""
```

Bad:

```python
try:
    ...
except Exception:
    pass
```

---

## GitHub patch-script standard flow

A successful patch script should follow this sequence.

### Stage 1 — Preflight

- Confirm running inside a Git repo.
- Resolve repo root.
- Move to repo root.
- Capture branch name.
- Confirm branch name is non-empty.
- Optionally block protected branches if the script will push.
- Confirm required commands exist.
- Print current `git status --short` as optional output.

Example:

```bash
repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail "Not inside a Git repo"
cd "$repo_root" || fail "Cannot cd to repo root"

branch=$(git branch --show-current 2>/dev/null || true)
[[ -n $branch ]] || fail "Detached HEAD or branch name unavailable; refusing to push"
```

### Stage 2 — Apply changes

- Generate or patch files deterministically.
- Prefer complete file rewrites for generated docs/templates.
- Use small, targeted scripts for source transformations.
- Keep repo-specific policy in docs when appropriate.
- Do not modify unrelated files.

### Stage 3 — Local validation

Use bounded checks only.

Examples:

```bash
run_required 30s bash -n scripts/new-script.sh
run_required 60s find scripts -type f -name '*.sh' -print -exec bash -n {} ';'
run_optional 60s ruby -e 'require "yaml"; ARGV.each { |f| Psych.safe_load(File.read(f), aliases: false); puts "OK #{f}" }' .github/workflows/*.yml
```

Do **not** claim Gradle/build/test/lint success unless those commands actually passed.

### Stage 4 — Stage changes

- Preserve existing staged files.
- Add patch files explicitly.
- If the user requested “all other staged files”, do not unstage anything.

```bash
git add \
  .github/workflows/android.yml \
  scripts/ci-gradle.sh \
  docs/CI_TASK_POLICY.md
```

### Stage 5 — Commit

- Commit only if there are staged changes.
- Use an explicit commit message.
- Avoid opening an editor.

```bash
if git diff --cached --quiet; then
  warn "No staged changes to commit"
else
  run_required 120s git commit -m "ci: improve workflow efficiency"
fi
```

### Stage 6 — Push

- Push current HEAD to the current branch.
- Disable prompts.
- Bound the push.

```bash
export GIT_TERMINAL_PROMPT=0
run_required 180s git push origin "HEAD:${branch}"
```

### Stage 7 — Final summary

Always print:

- result;
- branch;
- commit SHA if available;
- files changed;
- warnings;
- whether push was attempted and whether it succeeded;
- any validation skipped.

---

## Gradle and Android CI patching rules

For Android/Gradle repositories on Termux:

### Prefer CI-aligned scoped tasks

Do not replace carefully scoped CI tasks with generic aggregates.

Prefer:

```bash
./gradlew --no-daemon --stacktrace :app:assembleDebug
./gradlew --no-daemon --stacktrace :app:testDebugUnitTest
./gradlew --no-daemon --stacktrace :app:lintDebug
```

Avoid generic aliases such as:

```bash
./gradlew check
./gradlew build
./gradlew test
./gradlew lint
```

unless the repo explicitly documents that these are the intended validation commands.

### Avoid local Gradle on Termux unless required

For patch scripts whose purpose is CI/workflow editing, local validation should normally be:

- shell syntax;
- YAML parsing;
- grep/structural checks;
- no local Gradle execution.

If Gradle must be run:

- use scoped tasks;
- use `--no-daemon` by default;
- use `--console=plain`;
- add an external heartbeat wrapper;
- set an outer timeout;
- avoid `--stop` and `--status` unless bounded;
- do not enable configuration cache globally without compatibility proof.

### Do not lower CI efficacy for speed

Acceptable speed improvements:

- path filters excluding irrelevant files such as docs from Android CI;
- workflow concurrency cancellation for superseded PR runs;
- Gradle build cache;
- scoped tasks matching intended validation;
- avoiding redundant artifact uploads;
- sharing a CI Gradle wrapper script;
- avoiding repeated boilerplate where reusable scripts preserve the same checks;
- adding PR templates/checklists that improve triage quality.

Risky or unacceptable speed improvements unless separately justified:

- deleting required jobs;
- converting failures to warnings;
- replacing scoped tests with no tests;
- skipping dependency pin verification;
- skipping release/signing verification;
- enabling broad config cache without evidence;
- using stale dependency caches that hide pin mismatches;
- suppressing lint/test failures;
- treating docs/templates as proof of runtime behaviour.

---

## Workflow path-filter policy

When excluding documentation from Android CI:

- remove `docs/**` from Android build/test/lint path triggers if docs changes do not affect code;
- keep workflow files, scripts, Gradle files, dependency manifests, app modules, and repo policy files as triggers;
- consider whether `README.md` and `AGENTS.md` should still trigger CI based on repository policy;
- ensure branch protection does not require skipped checks for docs-only PRs;
- document the policy in the repo.

Do not use broad negative patterns without understanding workflow syntax order. If simple omission from `paths` is sufficient, prefer omission over complex `paths-ignore` interactions.

---

## PR review/comment resolution scripts

For scripts that respond to PR comments or resolve threads:

- fetch full PR metadata and comments before deciding;
- do not rely on truncated previews;
- classify each item as valid, stale, duplicate, fixed, out-of-scope, or needs human input;
- implement valid fixes first;
- only resolve threads that are genuinely addressed;
- post concise evidence-backed comments;
- avoid resolving comments merely because a script ran;
- keep GitHub API actions bounded or perform them through local `gh` with timeouts.

Do not hide or dismiss review feedback unless the reason is explicit and technically justified.

---

## Recommended Bash skeleton for GitHub patch scripts

Use this as the default structure for generated patch scripts.

```bash
#!/usr/bin/env bash

# No blanket set -euo pipefail.
# Failure policy is explicit per command.

SCRIPT_NAME=${0##*/}
TMP_ROOT="${HOME}/tmp/${SCRIPT_NAME}.$$"
WARNINGS=0
ERRORS=0
PUSH_ATTEMPTED=0
PUSH_SUCCEEDED=0

log() { printf '[INFO] %s\n' "$*" >&2; }
warn() { WARNINGS=$((WARNINGS + 1)); printf '[WARN] %s\n' "$*" >&2; }
error() { ERRORS=$((ERRORS + 1)); printf '[ERROR] %s\n' "$*" >&2; }
fail() { error "$*"; print_summary FAILED; exit 1; }

cleanup() {
  local rc=$?
  trap - EXIT INT TERM
  if [[ -n ${TMP_ROOT:-} && -d ${TMP_ROOT:-} ]]; then
    rm -rf -- "$TMP_ROOT" 2>/dev/null || :
  fi
  exit "$rc"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

run_required() {
  local seconds=$1
  shift
  log "RUN(required, ${seconds}): $*"
  if command -v timeout >/dev/null 2>&1; then
    timeout --foreground "$seconds" "$@"
  else
    warn "timeout unavailable; running required command without outer timeout: $*"
    "$@"
  fi
  local rc=$?
  if ((rc != 0)); then
    error "Required command failed with status ${rc}: $*"
    return "$rc"
  fi
  return 0
}

run_optional() {
  local seconds=$1
  shift
  log "RUN(optional, ${seconds}): $*"
  if command -v timeout >/dev/null 2>&1; then
    timeout --foreground "$seconds" "$@" || {
      warn "Optional command failed or timed out: $*"
      return 0
    }
  else
    warn "timeout unavailable; skipping optional command: $*"
    return 0
  fi
}

print_summary() {
  local result=$1
  local branch='unknown'
  local head='unknown'

  branch=$(git branch --show-current 2>/dev/null || printf 'unknown')
  head=$(git rev-parse --short HEAD 2>/dev/null || printf 'unknown')

  printf '\n========================================\n' >&2
  printf 'RESULT: %s\n' "$result" >&2
  printf 'Branch: %s\n' "$branch" >&2
  printf 'HEAD:   %s\n' "$head" >&2
  printf 'Warnings: %d\n' "$WARNINGS" >&2
  printf 'Errors:   %d\n' "$ERRORS" >&2
  printf 'Push attempted: %d\n' "$PUSH_ATTEMPTED" >&2
  printf 'Push succeeded: %d\n' "$PUSH_SUCCEEDED" >&2
  printf '========================================\n' >&2
}

main() {
  mkdir -p -- "$TMP_ROOT" || fail "Could not create temp root: $TMP_ROOT"

  export GIT_TERMINAL_PROMPT=0
  export GCM_INTERACTIVE=never
  export GIT_PAGER=cat
  export PAGER=cat

  log '[1/7] Inspecting repository'
  repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail 'Not inside a Git repository'
  cd "$repo_root" || fail "Cannot cd to repo root: $repo_root"

  branch=$(git branch --show-current 2>/dev/null || true)
  [[ -n $branch ]] || fail 'Detached HEAD or empty branch name; refusing to push'

  run_optional 20s git status --short

  log '[2/7] Applying patch'
  # write/update files here

  log '[3/7] Validating syntax'
  run_required 30s bash -n "$0" || fail 'Self syntax validation failed'
  if [[ -d scripts ]]; then
    run_required 90s find scripts -type f -name '*.sh' -print -exec bash -n {} ';' || fail 'Script syntax validation failed'
  fi

  log '[4/7] Staging patch files'
  # git add explicit paths here

  log '[5/7] Inspecting staged diff'
  run_optional 30s git diff --cached --stat
  run_optional 30s git diff --cached --name-only

  log '[6/7] Committing'
  if git diff --cached --quiet; then
    warn 'No staged changes to commit'
  else
    run_required 120s git commit -m 'chore: apply repository patch' || fail 'Commit failed'
  fi

  log '[7/7] Pushing current branch'
  PUSH_ATTEMPTED=1
  run_required 180s git push origin "HEAD:${branch}" || fail 'Push failed'
  PUSH_SUCCEEDED=1

  print_summary SUCCESS
}

main "$@"
```

---

## ChatGPT prompt template for future tasks

Use this structure when asking ChatGPT for a patch script.

```text
Compose a Termux-safe GitHub patch script for repo <owner/repo>.

Requirements:
- run from the repo root on native Android Termux / Pixel 9a;
- do not use /tmp; use ~/tmp;
- do not use interactive commands;
- do not invoke editors or pagers;
- do not require a clean working tree unless technically unsafe;
- preserve existing staged files and include them in the final commit;
- use explicit timeouts for all external commands;
- use timeout || true semantics for noncritical inspection commands such as git diff/status/log;
- do not swallow output;
- print stage progress and a final summary;
- validate locally only with checks that are meaningful and bounded on Termux;
- do not run Gradle locally unless required and justified;
- if Gradle is required, use scoped tasks, --no-daemon, --console=plain, heartbeat output, and an outer timeout;
- commit with message: <message>;
- push HEAD to origin/<current-branch>.

Task-specific requirements:
- <describe files/features/workflows/comments to update>

Before writing the script, analyse the repo/task and explain briefly:
- what is required vs optional;
- what is unsafe to continue;
- what validation is local vs CI-only;
- why the patch does not lower CI/test/build efficacy.
```

---

## Acceptance checklist for generated patch scripts

Before accepting a ChatGPT-generated script, verify:

### Termux safety

- [ ] Uses `#!/usr/bin/env bash` or correct Termux Python interpreter.
- [ ] Uses `~/tmp`, not `/tmp`.
- [ ] Does not rely on shared storage for active state.
- [ ] Does not require interactive input.
- [ ] Does not launch editors or pagers.
- [ ] Sets `GIT_TERMINAL_PROMPT=0` before push.
- [ ] Prints visible stage progress.
- [ ] Prints a final summary.

### Command execution

- [ ] Every external command that may block is timeout-bound.
- [ ] Noncritical commands use warning/continue behaviour.
- [ ] Required failures stop at controlled boundaries.
- [ ] Output is visible and not swallowed.
- [ ] No broad unexplained `|| true` is used for required work.
- [ ] Optional `timeout || true` use is documented by function or stage name.

### Git correctness

- [ ] Resolves and uses repo root.
- [ ] Captures current branch.
- [ ] Refuses to push detached HEAD.
- [ ] Preserves existing staged files when requested.
- [ ] Does not reset or clean the working tree unless explicitly required.
- [ ] Stages intended patch files explicitly.
- [ ] Commits only if staged changes exist.
- [ ] Pushes `HEAD:<current-branch>`.

### Patch correctness

- [ ] Keeps changes scoped to the requested goal.
- [ ] Does not remove validation just for speed.
- [ ] Documents repo policy changes where useful.
- [ ] Avoids speculative or unrelated refactors.
- [ ] Includes templates/docs only when they improve future workflow quality.

### Validation

- [ ] Runs `bash -n` on generated shell scripts.
- [ ] Parses workflow YAML when tooling is available.
- [ ] Uses bounded validation only.
- [ ] Does not claim build/test/lint success unless actually run and passed.
- [ ] Clearly states what was left to GitHub Actions.

---

## Common failure modes to avoid

### Hanging

Caused by:

- unbounded `git push` waiting for credentials;
- Gradle daemon operations;
- `gh` commands prompting;
- package managers asking questions;
- hidden pagers/editors;
- missing `timeout` around network operations.

Mitigation:

- disable prompts;
- set pager to `cat`;
- use bounded command helpers;
- avoid daemon-management commands;
- print heartbeat for long runs.

### Losing staged work

Caused by:

- `git reset`;
- `git checkout -- .`;
- `git clean -xfd`;
- assuming clean-tree scripts are always acceptable.

Mitigation:

- preserve staged files;
- add only intended files;
- never reset unless explicitly requested and safe.

### False success

Caused by:

- `|| true` on required commands;
- broad exception swallowing;
- treating optional validation as required proof;
- claiming CI success after only syntax checks.

Mitigation:

- classify required vs optional;
- use final summaries;
- say exactly which validations passed and which were skipped.

### Lowering CI efficacy

Caused by:

- deleting jobs to make CI green;
- converting failures to warnings;
- replacing scoped tests with no tests;
- skipping dependency pin validation;
- broad path ignores that skip relevant changes.

Mitigation:

- optimise orchestration, caching, and path filters only where safe;
- preserve the same semantic validation;
- document task policy.

---

## Final rule

A good ChatGPT-generated GitHub patch script for Pixel 9a Termux should be boring to run:

- it starts visibly;
- every long action has a timeout;
- every optional failure is named and nonfatal;
- every required failure stops clearly;
- user work is preserved;
- the commit is explicit;
- the push is bounded;
- the final summary tells the truth.
