#!/usr/bin/env bash
#
# generate-qrcodes.sh — Generate gallery codes and produce a QR-code PDF in one go.
#
# Usage:
#   ./generate-qrcodes.sh                                      # interactive shell mode
#   ./generate-qrcodes.sh <EVENT_CODE> [CODE_COUNT] [EVENT_NAME] [EXTRA_ARGS...]
#   ./generate-qrcodes.sh --app.mode=... [EXTRA_ARGS...]       # pass flags directly
#
# Options:
#   -v, --verbose   Show Spring Boot log output (hidden by default)
#
# Examples:
#   ./generate-qrcodes.sh
#   ./generate-qrcodes.sh XY9G
#   ./generate-qrcodes.sh XY9G 100
#   ./generate-qrcodes.sh XY9G 100 "My Photo Event"
#   ./generate-qrcodes.sh XY9G 100 "My Photo Event" --app.gallery-url=https://my.site/gallery?code=
#   ./generate-qrcodes.sh --app.mode=generate-codes --app.event-code=XY9G
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_NAME="foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
NATIVE_NAME="foto-gallery-qrcode-generator"

# --- Parse early flags --------------------------------------------------------
VERBOSE=false
args=()
for arg in "$@"; do
  case "$arg" in
    -v|--verbose) VERBOSE=true ;;
    *)            args+=("$arg") ;;
  esac
done
set -- "${args[@]+"${args[@]}"}"

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

# When not verbose, suppress Spring Boot log output
QUIET_ARGS=()
if [[ "$VERBOSE" == false ]]; then
  QUIET_ARGS=(--logging.level.root=WARN --spring.main.banner-mode=off)
fi

# --- Load optional PicPeak credentials ----------------------------------------
# Copy picpeak.properties.example to picpeak.properties and fill in your
# credentials.  The file is .gitignored so it will never be committed.
PICPEAK_ARGS=()
if [[ -f "$SCRIPT_DIR/picpeak.properties" ]]; then
  PICPEAK_ARGS=(--spring.config.additional-location="file:$SCRIPT_DIR/picpeak.properties")
fi

# --- Interactive mode (no arguments) ------------------------------------------
if [[ $# -lt 1 ]]; then
  echo "==> No arguments provided. Launching interactive shell..."
  "${RUN[@]}" ${PICPEAK_ARGS[@]+"${PICPEAK_ARGS[@]}"} ${QUIET_ARGS[@]+"${QUIET_ARGS[@]}"}
  exit 0
fi

# --- Parse arguments ----------------------------------------------------------
# If first arg starts with '--', pass all args directly to the jar (no positional parsing).
# This allows: ./generate-qrcodes.sh --app.mode=generate-codes --app.event-code=XY9G
if [[ "${1:-}" =~ ^-- ]]; then
  "${RUN[@]}" ${PICPEAK_ARGS[@]+"${PICPEAK_ARGS[@]}"} ${QUIET_ARGS[@]+"${QUIET_ARGS[@]}"} "$@"
  exit $?
fi

EVENT_CODE="$1"
shift

CODE_COUNT="17"
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
  ${PICPEAK_ARGS[@]+"${PICPEAK_ARGS[@]}"} \
  ${QUIET_ARGS[@]+"${QUIET_ARGS[@]}"} \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

# --- Step 2: Generate PDF -----------------------------------------------------
echo "==> Generating QR-code PDF ..."
"${RUN[@]}" \
  --app.mode=generate-pdf \
  ${PICPEAK_ARGS[@]+"${PICPEAK_ARGS[@]}"} \
  ${QUIET_ARGS[@]+"${QUIET_ARGS[@]}"} \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

echo "==> Done."
