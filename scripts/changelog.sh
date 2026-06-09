#!/usr/bin/env bash
#
# Append a single entry to CHANGELOG.md — newest first.
#
# Usage:
#   scripts/changelog.sh <type> "<summary>" [commit-hash]
#
#   <type>        conventional-commit style, e.g. feat(security), fix(db), refactor, docs(adr)
#   <summary>     short description of WHAT changed (one line)
#   [commit-hash] optional; defaults to the current HEAD short hash
#
# Date is filled in automatically. The script inserts the entry right after the
# marker line, so it never has to read or parse the existing changelog.
#
# Examples:
#   scripts/changelog.sh "feat(security)" "enforce user status in authorization"
#   scripts/changelog.sh "docs(adr)" "add ADR-0005 (bundled Authentik)" 5ecf378
#
set -euo pipefail

if [ "$#" -lt 2 ]; then
    echo "usage: $0 <type> \"<summary>\" [commit-hash]" >&2
    exit 2
fi

TYPE="$1"
SUMMARY="$2"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HASH="${3:-$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo '-')}"
DATE="$(date +%F)"
FILE="$ROOT/CHANGELOG.md"
MARKER="<!-- CHANGELOG:INSERT -->"
ENTRY="- ${DATE} \`${TYPE}\` (${HASH}) — ${SUMMARY}"

if [ ! -f "$FILE" ]; then
    cat > "$FILE" <<HEADER
# Changelog

All notable changes to Volantic ERP, newest first.
Each entry: \`date\` \`type(scope)\` (commit) — summary.

${MARKER}
HEADER
fi

tmp="$(mktemp)"
inserted=0
while IFS= read -r line || [ -n "$line" ]; do
    printf '%s\n' "$line" >> "$tmp"
    if [ "$inserted" -eq 0 ] && [ "$line" = "$MARKER" ]; then
        printf '%s\n' "$ENTRY" >> "$tmp"
        inserted=1
    fi
done < "$FILE"

if [ "$inserted" -eq 0 ]; then
    echo "error: marker '$MARKER' not found in $FILE" >&2
    rm -f "$tmp"
    exit 1
fi

mv "$tmp" "$FILE"
echo "changelog += ${ENTRY}"
