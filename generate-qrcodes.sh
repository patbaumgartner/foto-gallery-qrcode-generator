#!/usr/bin/env bash
#
# generate-qrcodes.sh — Generate gallery codes and produce a QR-code PDF in one go.
#
# Usage:
#   ./generate-qrcodes.sh                                      # interactive shell mode
#   ./generate-qrcodes.sh <EVENT_CODE> [CODE_COUNT] [EVENT_NAME] [EXTRA_ARGS...]
#
# Examples:
#   ./generate-qrcodes.sh
#   ./generate-qrcodes.sh XY9G
#   ./generate-qrcodes.sh XY9G 100
#   ./generate-qrcodes.sh XY9G 100 "My Photo Event"
#   ./generate-qrcodes.sh XY9G 100 "My Photo Event" --app.base-url=https://my.site/gallery/
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_NAME="foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
NATIVE_NAME="foto-gallery-qrcode-generator"

# --- Resolve executable -------------------------------------------------------
# Check current directory first, then target/ subdirectory
if [[ -x "$SCRIPT_DIR/$NATIVE_NAME" ]]; then
  RUN=("$SCRIPT_DIR/$NATIVE_NAME")
elif [[ -x "$SCRIPT_DIR/target/$NATIVE_NAME" ]]; then
  RUN=("$SCRIPT_DIR/target/$NATIVE_NAME")
elif [[ -f "$SCRIPT_DIR/$JAR_NAME" ]]; then
  RUN=(java -jar "$SCRIPT_DIR/$JAR_NAME")
elif [[ -f "$SCRIPT_DIR/target/$JAR_NAME" ]]; then
  RUN=(java -jar "$SCRIPT_DIR/target/$JAR_NAME")
else
  echo "ERROR: No executable found ('$NATIVE_NAME')." >&2
  exit 1
fi

# --- Interactive mode (no arguments) ------------------------------------------
if [[ $# -lt 1 ]]; then
  echo "==> No arguments provided. Launching interactive shell..."
  "${RUN[@]}"
  exit 0
fi

# --- Parse arguments ----------------------------------------------------------
EVENT_CODE="$1"
shift

CODE_COUNT="50"
if [[ $# -gt 0 && "$1" =~ ^[0-9]+$ ]]; then
  CODE_COUNT="$1"
  shift
fi

EVENT_NAME=""
if [[ $# -gt 0 && ! "$1" =~ ^-- ]]; then
  EVENT_NAME="$1"
  shift
fi

EXTRA_ARGS=("$@")

# --- Step 1: Generate codes ---------------------------------------------------
echo "==> Generating $CODE_COUNT codes for event $EVENT_CODE ..."
"${RUN[@]}" \
  --app.mode=generate-codes \
  --app.event-code="$EVENT_CODE" \
  --app.code-count="$CODE_COUNT" \
  --app.event-name="$EVENT_NAME" \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

# --- Step 2: Generate PDF -----------------------------------------------------
echo "==> Generating QR-code PDF ..."
"${RUN[@]}" \
  --app.mode=generate-pdf \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

echo "==> Done."
