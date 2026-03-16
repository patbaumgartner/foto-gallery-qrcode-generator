#!/usr/bin/env bash
#
# schulfotos-mel-rohrer.sh — Generate school photo gallery codes for mel-rohrer.ch/schulfotos.
#
# The base URL is hardcoded to https://mel-rohrer.ch/schulfotos/?code= and all standard
# settings are used (3x4 grid, 200 px QR size, cutting lines enabled).
# A back page with the gallery URL https://mel-rohrer.ch/schulfotos is added automatically.
#
# When run without arguments the script prompts interactively for every parameter.
# A random 4-character alphanumeric EVENT_CODE is generated automatically
# and can be accepted or overridden in interactive mode.
#
# Usage:
#   ./schulfotos-mel-rohrer.sh                                   # interactive mode
#   ./schulfotos-mel-rohrer.sh <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]
#   ./schulfotos-mel-rohrer.sh --help
#
# Arguments:
#   KLASSENNAME   Class name used as the event label in the PDF (e.g. "GS1d BA")
#   CODE_COUNT    Number of codes to generate (default: 17)
#   EXTRA_ARGS    Any additional --app.* flags passed to both steps
#
# Examples:
#   ./schulfotos-mel-rohrer.sh
#   ./schulfotos-mel-rohrer.sh "GS1d BA"
#   ./schulfotos-mel-rohrer.sh "GS1d BA" 30
#   ./schulfotos-mel-rohrer.sh "GS1d BA" 30 --app.show-cutting-lines=true
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_NAME="foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
NATIVE_NAME="foto-gallery-qrcode-generator"
BASE_URL="https://mel-rohrer.ch/schulfotos/?code="
GALLERY_URL="https://mel-rohrer.ch/schulfotos"
DEFAULT_CODE_COUNT="17"

# --- Help ---------------------------------------------------------------------
show_help() {
  cat <<EOF
schulfotos-mel-rohrer.sh — Generate school photo gallery codes & QR-code PDFs.

Usage:
  $0                                   Interactive mode (prompts for all settings)
  $0 <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]
  $0 --help                            Show this help message

Arguments:
  KLASSENNAME   Class name used as the event label in the PDF (e.g. "GS1d BA").
                A random 4-character alphanumeric EVENT_CODE is generated
                automatically. You will be asked to confirm or override it in
                interactive mode.
  CODE_COUNT    Number of codes to generate (default: $DEFAULT_CODE_COUNT)
  EXTRA_ARGS    Any additional --app.* flags passed to both steps

Examples:
  $0
  $0 "GS1d BA"
  $0 "GS1d BA" 30
  $0 "GS1d BA" 30 --app.show-cutting-lines=true

Defaults:
  Base URL         $BASE_URL
  Gallery URL      $GALLERY_URL
  Grid             3 columns × 4 rows
  QR size          200 px
  Cutting lines    on
EOF
  exit 0
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  show_help
fi

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

# --- Helper: generate a random 4-char alphanumeric EVENT_CODE -----------------
generate_event_code() {
  local chars
  chars=$(head -c 100 /dev/urandom | tr -cd 'A-Z0-9')
  echo "${chars:0:4}"
}

# --- Collect parameters -------------------------------------------------------
if [[ $# -ge 1 ]]; then
  # --- Non-interactive: positional arguments ----------------------------------
  KLASSENNAME="$1"
  shift

  CODE_COUNT="$DEFAULT_CODE_COUNT"
  if [[ $# -gt 0 && "$1" =~ ^[0-9]+$ ]]; then
    CODE_COUNT="$1"
    shift
  fi

  EXTRA_ARGS=("$@")
  EVENT_CODE="$(generate_event_code)"
else
  # --- Interactive: prompt the user -------------------------------------------
  echo "===  Schulfotos — mel-rohrer.ch  ==="
  echo ""

  read -rp "Klassenname (e.g. GS1d BA): " KLASSENNAME
  if [[ -z "$KLASSENNAME" ]]; then
    echo "ERROR: Klassenname must not be empty." >&2
    exit 1
  fi

  SUGGESTED_CODE="$(generate_event_code)"
  read -rp "Event-Code [$SUGGESTED_CODE]: " EVENT_CODE_INPUT
  EVENT_CODE="${EVENT_CODE_INPUT:-$SUGGESTED_CODE}"

  read -rp "Anzahl Codes [$DEFAULT_CODE_COUNT]: " CODE_COUNT_INPUT
  CODE_COUNT="${CODE_COUNT_INPUT:-$DEFAULT_CODE_COUNT}"

  EXTRA_ARGS=()
  echo ""
fi

# --- Derive output filenames from class name ----------------------------------
# Replace spaces and slashes with hyphens to produce a safe file name prefix.
SAFE_NAME="${KLASSENNAME// /-}"
SAFE_NAME="${SAFE_NAME//\//-}"
CSV_PATH="${SAFE_NAME}-codes.csv"
PDF_PATH="${SAFE_NAME}-qr-codes.pdf"

# --- Summary ------------------------------------------------------------------
echo "==> Settings:"
echo "    Klassenname : $KLASSENNAME"
echo "    Event-Code  : $EVENT_CODE"
echo "    Code Count  : $CODE_COUNT"
echo "    CSV         : $CSV_PATH"
echo "    PDF         : $PDF_PATH"
echo ""

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
  --app.gallery-url="$GALLERY_URL" \
  --app.show-cutting-lines=true \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

echo "==> Done. Output files:"
echo "    CSV: $CSV_PATH"
echo "    PDF: $PDF_PATH"
