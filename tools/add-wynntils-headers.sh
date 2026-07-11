#!/usr/bin/env bash
# Prepends an LGPL attribution header to every shim file that lacks one.
# Idempotent: files already containing the marker line are skipped. Files
# copied verbatim from Wynntils that already carry a Wynntils copyright
# header are also skipped.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

HEADER='// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
'

count=0
while IFS= read -r -d '' f; do
  if grep -q -e '© Wynntils' -e 'Copyright © Wynntils' "$f"; then
    continue
  fi
  printf '%s' "$HEADER" | cat - "$f" > "$f.tmp" && mv "$f.tmp" "$f"
  count=$((count+1))
done < <(find src/main/java/julianh06/wynnextras/wtshim -name '*.java' -print0)

echo "headers added to $count files"
