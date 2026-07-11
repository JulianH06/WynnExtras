#!/usr/bin/env bash
# Relocates the Wynntils compat shim from com.wynntils.* to julianh06.wynnextras.wtshim.*
# so the standalone fork can run alongside real Wynntils (no duplicate class names).
#
# Idempotent: safe to re-run any time. This is the standing POST-UPSTREAM-MERGE step —
# newly merged files that still reference com.wynntils get rewritten, everything else
# is untouched (the replacement string does not contain the search token).
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

OLD_DIR='src/main/java/com/wynntils'
NEW_DIR='src/main/java/julianh06/wynnextras/wtshim'

# Step 1 — one-time physical move (no-op on re-runs)
if [ -d "$OLD_DIR" ]; then
  mkdir -p "$(dirname "$NEW_DIR")"
  git mv "$OLD_DIR" "$NEW_DIR"
  rmdir src/main/java/com 2>/dev/null || true
fi

# Step 2 — textual rewrite of every tracked text file that references com.wynntils.
# git grep -Il skips binaries automatically; -z/xargs -0 is path-safe.
git grep -Ilz -e 'com\.wynntils' -e 'com/wynntils' -- src build.gradle settings.gradle gradle.properties 2>/dev/null \
  | xargs -0 -r sed -i \
      -e 's/com\.wynntils/julianh06.wynnextras.wtshim/g' \
      -e 's#com/wynntils#julianh06/wynnextras/wtshim#g'

# Step 3 — hard verification gate
if git grep -n -e 'com\.wynntils' -e 'com/wynntils' -- src build.gradle settings.gradle gradle.properties; then
  echo 'FAIL: unrewritten com.wynntils references remain (see above)' >&2
  exit 1
fi
echo 'relocate: OK'
