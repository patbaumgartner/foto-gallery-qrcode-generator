#!/usr/bin/env bash
#
# schulfotos-mel-rohrer.sh — Generate school photo gallery codes for mel-rohrer.ch/schulfotos.
#
# The gallery URL used in QR codes is hardcoded to https://mel-rohrer.ch/schulfotos/?code=
# and the base URL printed on the back of the PDF is https://mel-rohrer.ch/schulfotos.
# All standard settings are used (3x4 grid, 200 px QR size, cutting lines enabled).
# A back page with the gallery password and the logo (logo.png) is always included.
#
# When run without arguments the script prompts interactively for every parameter.
# A random 4-character alphanumeric EVENT_CODE is generated automatically
# and can be accepted or overridden in interactive mode.
#
# Usage:
#   ./schulfotos-mel-rohrer.sh                                   # interactive mode
#   ./schulfotos-mel-rohrer.sh <KLASSENNAME> [SHOOTING_DATE] [CODE_COUNT] [EXTRA_ARGS...]
#   ./schulfotos-mel-rohrer.sh --help
#
# Options:
#   -v, --verbose   Show Spring Boot log output (hidden by default)
#
# Arguments:
#   KLASSENNAME    Class name used as the event label in the PDF (e.g. "GS1d BA")
#   SHOOTING_DATE  Shooting date in German format DD.MM.YYYY (default: today)
#   CODE_COUNT     Number of codes to generate (default: 17)
#   EXTRA_ARGS     Any additional --app.* flags passed to both steps
#
# Examples:
#   ./schulfotos-mel-rohrer.sh
#   ./schulfotos-mel-rohrer.sh "GS1d BA"
#   ./schulfotos-mel-rohrer.sh "GS1d BA" 25.03.2026
#   ./schulfotos-mel-rohrer.sh "GS1d BA" 25.03.2026 30
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_NAME="foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
NATIVE_NAME="foto-gallery-qrcode-generator"
BASE_URL="https://mel-rohrer.ch/schulfotos"
GALLERY_URL="https://mel-rohrer.ch/schulfotos/?code="
DEFAULT_CODE_COUNT="17"

# --- Helpers ------------------------------------------------------------------
generate_event_code() {
  local chars
  chars=$(head -c 100 /dev/urandom | tr -cd 'A-Z0-9')
  echo "${chars:0:4}"
}

convert_date() {
  local input="$1"
  if [[ "$input" =~ ^([0-9]{2})\.([0-9]{2})\.([0-9]{4})$ ]]; then
    echo "${BASH_REMATCH[3]}-${BASH_REMATCH[2]}-${BASH_REMATCH[1]}"
  else
    echo "ERROR: Invalid date format '$input'. Expected DD.MM.YYYY." >&2
    return 1
  fi
}

show_help() {
  cat <<EOF
schulfotos-mel-rohrer.sh — Generate school photo gallery codes & QR-code PDFs.

Usage:
  $0                                   Interactive mode (prompts for all settings)
  $0 <KLASSENNAME> [SHOOTING_DATE] [CODE_COUNT] [EXTRA_ARGS...]
  $0 --help                            Show this help message

Options:
  -v, --verbose   Show Spring Boot log output (hidden by default)

Arguments:
  KLASSENNAME    Class name used as the event label in the PDF (e.g. "GS1d BA").
                 A random 4-character alphanumeric EVENT_CODE is generated
                 automatically. You will be asked to confirm or override it in
                 interactive mode.
  SHOOTING_DATE  Shooting date in German format DD.MM.YYYY (default: today)
  CODE_COUNT     Number of codes to generate (default: $DEFAULT_CODE_COUNT)
  EXTRA_ARGS     Any additional --app.* flags passed to both steps

Defaults:
  Base URL (back of PDF)  $BASE_URL
  Gallery URL (QR codes)  $GALLERY_URL
  Grid                    3 columns × 4 rows
  QR size                 200 px
  Cutting lines           on
EOF
  exit 0
}

# --- Parse flags --------------------------------------------------------------
VERBOSE=false
args=()
for arg in "$@"; do
  case "$arg" in
    -v|--verbose) VERBOSE=true ;;
    --help|-h)    show_help ;;
    *)            args+=("$arg") ;;
  esac
done
set -- "${args[@]+"${args[@]}"}"

[[ "$VERBOSE" == false ]] \
  && QUIET_ARGS=(--logging.level.root=WARN --spring.main.banner-mode=off) \
  || QUIET_ARGS=()

PICPEAK_ARGS=()
[[ -f "$SCRIPT_DIR/picpeak.properties" ]] \
  && PICPEAK_ARGS=(--spring.config.additional-location="file:$SCRIPT_DIR/picpeak.properties")

# --- Resolve executable -------------------------------------------------------
if   [[ -x "$SCRIPT_DIR/$NATIVE_NAME" ]];        then RUN=("$SCRIPT_DIR/$NATIVE_NAME")
elif [[ -x "$SCRIPT_DIR/target/$NATIVE_NAME" ]]; then RUN=("$SCRIPT_DIR/target/$NATIVE_NAME")
elif [[ -f "$SCRIPT_DIR/$JAR_NAME" ]];            then RUN=(java -jar "$SCRIPT_DIR/$JAR_NAME")
elif [[ -f "$SCRIPT_DIR/target/$JAR_NAME" ]];     then RUN=(java -jar "$SCRIPT_DIR/target/$JAR_NAME")
else
  echo "ERROR: No executable found (neither '$NATIVE_NAME' nor '$JAR_NAME')." >&2
  exit 1
fi

# --- Collect parameters -------------------------------------------------------
if [[ $# -ge 1 ]]; then
  KLASSENNAME="$1"; shift

  SHOOTING_DATE_DE="$(date +%d.%m.%Y)"
  [[ $# -gt 0 && "$1" =~ ^[0-9]{2}\.[0-9]{2}\.[0-9]{4}$ ]] && { SHOOTING_DATE_DE="$1"; shift; }

  CODE_COUNT="$DEFAULT_CODE_COUNT"
  [[ $# -gt 0 && "$1" =~ ^[0-9]+$ ]] && { CODE_COUNT="$1"; shift; }

  EXTRA_ARGS=("$@")
  EVENT_CODE="$(generate_event_code)"
  PICPEAK_ENABLED=false
else
  echo "===  Schulfotos — mel-rohrer.ch  ==="
  echo ""

  read -rp "Klassenname (e.g. GS1d BA): " KLASSENNAME
  [[ -z "$KLASSENNAME" ]] && { echo "ERROR: Klassenname must not be empty." >&2; exit 1; }

  SUGGESTED_CODE="$(generate_event_code)"
  read -rp "Event-Code [$SUGGESTED_CODE]: " EVENT_CODE_INPUT
  EVENT_CODE="${EVENT_CODE_INPUT:-$SUGGESTED_CODE}"

  TODAY="$(date +%d.%m.%Y)"
  read -rp "Shooting-Datum (DD.MM.YYYY) [$TODAY]: " SHOOTING_DATE_INPUT
  SHOOTING_DATE_DE="${SHOOTING_DATE_INPUT:-$TODAY}"

  read -rp "Anzahl Codes [$DEFAULT_CODE_COUNT]: " CODE_COUNT_INPUT
  CODE_COUNT="${CODE_COUNT_INPUT:-$DEFAULT_CODE_COUNT}"

  read -rp "Galerie-Events in PicPeak erstellen? [y/N]: " PICPEAK_INPUT
  [[ "${PICPEAK_INPUT,,}" =~ ^(y|yes)$ ]] && PICPEAK_ENABLED=true || PICPEAK_ENABLED=false

  EXTRA_ARGS=()
  echo ""
fi

SHOOTING_DATE="$(convert_date "$SHOOTING_DATE_DE")" || exit 1

# --- Derive output paths ------------------------------------------------------
SAFE_NAME="${KLASSENNAME//[ \/]/-}"
OUTPUT_DIR="schulfotos"
mkdir -p "$OUTPUT_DIR"
CSV_PATH="${OUTPUT_DIR}/${SAFE_NAME}-codes.csv"
PDF_PATH="${OUTPUT_DIR}/${SAFE_NAME}-qr-codes.pdf"

# --- Summary ------------------------------------------------------------------
echo "==> Settings:"
echo "    Klassenname    : $KLASSENNAME"
echo "    Event-Code     : $EVENT_CODE"
echo "    Shooting-Datum : $SHOOTING_DATE_DE ($SHOOTING_DATE)"
echo "    Code Count     : $CODE_COUNT"
echo "    PicPeak        : $PICPEAK_ENABLED"
echo "    CSV            : $CSV_PATH"
echo "    PDF            : $PDF_PATH"
echo ""

# --- Step 1: Generate codes ---------------------------------------------------
echo "==> Generating $CODE_COUNT codes for class '$KLASSENNAME' (event: $EVENT_CODE) ..."
"${RUN[@]}" \
  --app.mode=generate-codes \
  --app.event-code="$EVENT_CODE" \
  --app.code-count="$CODE_COUNT" \
  --app.event-name="$KLASSENNAME" \
  --app.csv-output-path="$CSV_PATH" \
  --app.gallery-url="$GALLERY_URL" \
  --app.picpeak.enabled="$PICPEAK_ENABLED" \
  --app.picpeak.event-date="$SHOOTING_DATE" \
  ${PICPEAK_ARGS[@]+"${PICPEAK_ARGS[@]}"} \
  ${QUIET_ARGS[@]+"${QUIET_ARGS[@]}"} \
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
  --app.logo-url=logo.png \
  ${PICPEAK_ARGS[@]+"${PICPEAK_ARGS[@]}"} \
  ${QUIET_ARGS[@]+"${QUIET_ARGS[@]}"} \
  ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}

echo "==> Done. Output files:"
echo "    CSV: $CSV_PATH"
echo "    PDF: $PDF_PATH"
