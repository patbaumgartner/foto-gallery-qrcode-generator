#!/usr/bin/env bash
#
# schulfotos-mel-rohrer.sh — Generate school photo gallery codes for mel-rohrer.ch/schulfotos.
#
# The base URL is hardcoded to https://mel-rohrer.ch/schulfotos/ and all standard
# settings are used (50 codes, 3x4 grid, 200 px QR size, no cutting lines).
#
# Usage:
#   ./schulfotos-mel-rohrer.sh <EVENT_CODE> <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]
#
# Arguments:
#   EVENT_CODE    4-character alphanumeric code prefix (e.g. XY9G)
#   KLASSENNAME   Class name used as the event label in the PDF (e.g. "Klasse 3A")
#   CODE_COUNT    Number of codes to generate (default: 50)
#   EXTRA_ARGS    Any additional --app.* flags passed to both steps
#
# Examples:
#   ./schulfotos-mel-rohrer.sh XY9G "Klasse 3A"
#   ./schulfotos-mel-rohrer.sh XY9G "Klasse 3A" 30
#   ./schulfotos-mel-rohrer.sh XY9G "Klasse 3A" 30 --app.show-cutting-lines=true
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_NAME="foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
NATIVE_NAME="foto-gallery-qrcode-generator"
BASE_URL="https://mel-rohrer.ch/schulfotos/"

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
  echo "ERROR: No executable found (neither '$NATIVE_NAME' nor '$JAR_NAME')." >&2
  exit 1
fi

# --- Argument validation ------------------------------------------------------
if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <EVENT_CODE> <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]" >&2
  echo "" >&2
  echo "  EVENT_CODE    4-character alphanumeric code prefix (e.g. XY9G)" >&2
  echo "  KLASSENNAME   Class name for the PDF label (e.g. \"Klasse 3A\")" >&2
  echo "  CODE_COUNT    Number of codes to generate (default: 50)" >&2
  exit 1
fi

EVENT_CODE="$1"
shift

KLASSENNAME="$1"
shift

CODE_COUNT="50"
if [[ $# -gt 0 && "$1" =~ ^[0-9]+$ ]]; then
  CODE_COUNT="$1"
  shift
fi

EXTRA_ARGS=("$@")

# --- Derive output filenames from class name ----------------------------------
# Replace spaces and slashes with hyphens to produce a safe file name prefix.
SAFE_NAME="${KLASSENNAME// /-}"
SAFE_NAME="${SAFE_NAME//\//-}"
CSV_PATH="${SAFE_NAME}-codes.csv"
PDF_PATH="${SAFE_NAME}-qr-codes.pdf"

# --- Step 1: Generate codes ---------------------------------------------------
echo "==> Generating $CODE_COUNT codes for class '$KLASSENNAME' (event: $EVENT_CODE) ..."
"${RUN[@]}" \
  --app.mode=generate-codes \
  --app.event-code="$EVENT_CODE" \
  --app.code-count="$CODE_COUNT" \
  --app.event-name="$KLASSENNAME" \
  --app.csv-output-path="$CSV_PATH" \
  --app.base-url="$BASE_URL" \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

# --- Step 2: Generate PDF -----------------------------------------------------
echo "==> Generating QR-code PDF ..."
"${RUN[@]}" \
  --app.mode=generate-pdf \
  --app.csv-input-path="$CSV_PATH" \
  --app.output-path="$PDF_PATH" \
  --app.base-url="$BASE_URL" \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

echo "==> Done. Output files:"
echo "    CSV: $CSV_PATH"
echo "    PDF: $PDF_PATH"
