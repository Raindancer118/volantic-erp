#!/usr/bin/env bash
#
# Append a single entry to CHANGELOG.md — newest first.
#
# Usage:
#   scripts/changelog.sh <type> "<summary>" [commit-hash] [details]
#
#   <type>        conventional-commit style, e.g. feat(security), fix(db), refactor, docs(adr)
#   <summary>     short headline of WHAT changed (one line)
#   [commit-hash] optional; defaults to the current HEAD short hash. Pass "" to force the default
#                 while still providing details.
#   [details]     optional; a longer explanation rendered as an indented block below the headline.
#                 Use " | " to separate it into multiple indented bullet lines.
#
# Date is filled in automatically. The script inserts the entry right after the
# marker line, so it never has to read or parse the existing changelog.
#
# Examples:
#   scripts/changelog.sh "feat(security)" "enforce user status in authorization"
#   scripts/changelog.sh "docs(adr)" "add ADR-0005 (bundled Authentik)" 5ecf378 \
#       "Each self-hosted install ships its own Authentik | ERP stays a pure OIDC resource server"
#
set -euo pipefail

if [ "$#" -lt 2 ]; then
    echo "usage: $0 <type> \"<summary>\" [commit-hash] [details]" >&2
    exit 2
fi

TYPE="$1"
SUMMARY="$2"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HASH="${3:-}"
if [ -z "$HASH" ]; then
    HASH="$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo '-')"
fi
DETAILS="${4:-}"
DATE="$(date +%F)"
FILE="$ROOT/CHANGELOG.md"
MARKER="<!-- CHANGELOG:INSERT -->"
HEADLINE="- ${DATE} \`${TYPE}\` (${HASH}) — ${SUMMARY}"

if [ ! -f "$FILE" ]; then
    cat > "$FILE" <<HEADER
# Changelog

All notable changes to Volantic ERP, newest first.
Each entry: \`date\` \`type(scope)\` (commit) — summary, with optional details indented below.

${MARKER}
HEADER
fi

# Build the block (headline + optional indented detail bullets) into a temp file.
block="$(mktemp)"
printf '%s\n' "$HEADLINE" >> "$block"
if [ -n "$DETAILS" ]; then
    # Split on " | " into separate indented bullet lines.
    printf '%s\n' "$DETAILS" | tr '\n' ' ' | awk -F ' \\| ' '{ for (i = 1; i <= NF; i++) print "  - " $i }' >> "$block"
fi

tmp="$(mktemp)"
inserted=0
while IFS= read -r line || [ -n "$line" ]; do
    printf '%s\n' "$line" >> "$tmp"
    if [ "$inserted" -eq 0 ] && [ "$line" = "$MARKER" ]; then
        cat "$block" >> "$tmp"
        inserted=1
    fi
done < "$FILE"

rm -f "$block"

if [ "$inserted" -eq 0 ]; then
    echo "error: marker '$MARKER' not found in $FILE" >&2
    rm -f "$tmp"
    exit 1
fi

mv "$tmp" "$FILE"
echo "changelog += ${HEADLINE}"
