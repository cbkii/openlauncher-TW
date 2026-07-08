#!/usr/bin/env bash
# Validates the reference checksums. Designed to be boring and bounded.
set -u
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P) || exit 1
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P) || exit 1
JSON="$ROOT/references/SHA256SUMS.json"
if ! command -v python3 >/dev/null 2>&1; then
  printf 'ERROR: python3 is required for checksum validation.\n' >&2
  exit 1
fi
python3 - "$ROOT" "$JSON" <<'EOF_PY'
import hashlib, json, sys
from pathlib import Path
root=Path(sys.argv[1])
records=json.loads(Path(sys.argv[2]).read_text(encoding='utf-8'))
errors=0
for r in records:
    p=root/r['path']
    if not p.is_file():
        print(f"MISSING {r['path']}")
        errors+=1
        continue
    h=hashlib.sha256()
    with p.open('rb') as f:
        for chunk in iter(lambda: f.read(1024*1024), b''):
            h.update(chunk)
    got=h.hexdigest()
    if got != r['sha256']:
        print(f"FAIL    {r['path']} expected={r['sha256']} got={got}")
        errors+=1
    else:
        print(f"OK      {r['path']}")
print("RESULT:", "SUCCESS" if errors == 0 else f"FAILED ({errors} errors)")
sys.exit(0 if errors == 0 else 1)
EOF_PY
