#!/usr/bin/env bash
# Git merge driver for *.java: normalizes the base (%O) and upstream (%B) sides
# to wtshim package names before merging, so upstream's com.wynntils imports
# don't produce conflicts on every import block.
#
# One-time setup per clone (merge drivers are not committable):
#   git config merge.wtshim.driver 'tools/merge-wtshim.sh %O %A %B'
# Wired up via .gitattributes: *.java merge=wtshim
set -eu
sed -i \
  -e 's/com\.wynntils/julianh06.wynnextras.wtshim/g' \
  -e 's#com/wynntils#julianh06/wynnextras/wtshim#g' \
  "$1" "$3"
exec git merge-file "$2" "$1" "$3"
