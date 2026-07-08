#!/usr/bin/env bash
# Lists packaged reference artifacts with size and checksum.
set -u
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P) || exit 1
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P) || exit 1
printf 'Reference artifacts under: %s\n\n' "$ROOT/references"
if command -v python3 >/dev/null 2>&1; then
  python3 - "$ROOT/references/SHA256SUMS.json" <<'EOF_PY'
import json, sys
from pathlib import Path
records=json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
for r in records:
    print(f"{r['size']:>12}  {r['sha256']}  {r['path']}")
EOF_PY
else
  find "$ROOT/references" -type f -print
fi
