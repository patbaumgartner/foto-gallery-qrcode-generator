# Foto Gallery QR Code Generator

A Spring Boot CLI application that generates gallery access codes and produces PDF documents with QR codes.

## Prerequisites

- **Java 25** (or later)
- **Maven** (or use the included `./mvnw` wrapper)
- **GraalVM with `native-image`** (only needed for native binary builds)

## Build

### JVM (JAR)

```bash
./mvnw clean package -DskipTests
```

### GraalVM Native Image

Requires GraalVM with `native-image` installed. The build uses the
GraalVM Reachability Metadata Repository for automatic reflection and
resource configuration of third-party libraries (PDFBox, ZXing, etc.).

```bash
./mvnw clean package -Pnative -DskipTests
```

The native binary will be at `target/foto-gallery-qrcode-generator`.

## Usage

The application supports two usage styles:

- **Interactive mode** (default) — run the app with no arguments and answer prompts
- **Command-line mode** — pass `--app.*` flags directly for scripting and automation

### Interactive Mode (default)

When `app.mode` is not set (the default), the application launches an interactive shell
that guides you through every setting. Pressing Enter at any prompt accepts the shown default.

**Using JAR:**

```bash
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar
```

**Using native binary:**

```bash
./target/foto-gallery-qrcode-generator
```

**Using the convenience script (no arguments):**

```bash
./generate-qrcodes.sh    # Linux / macOS
generate-qrcodes.bat     # Windows
```

**Example session:**

```
=== Foto Gallery QR Code Generator - Interactive Shell ===

What would you like to do?
  1) generate-codes - Generate gallery access codes
  2) generate-pdf   - Generate QR code PDF from existing codes
  3) both           - Generate codes and then produce QR code PDF
Enter choice (1/2/3 or name) [both]:
Event code (4-char prefix, e.g. XY9G): XY9G
Number of codes to generate [50]:
Event name []: My Photo Event
CSV output path [codes.csv]:
CSV input path [codes.csv]:
PDF output path [qr-codes.pdf]:
Base URL for QR codes [https://my.site/gallery/]:
QR code size (pixels) [200]:
Grid columns per page [3]:
Grid rows per page [4]:
Show cutting lines (yes/no) [no]: yes
```

The event code is validated at the prompt — it must be exactly 4 alphanumeric characters.
Invalid input is rejected and re-prompted rather than causing an error later.

### 1. Generate Gallery Codes

Generates a CSV file with random gallery codes for a given event.

**Using JAR:**

```bash
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.event-name="My Photo Event" \
  --app.code-count=50
```

**Using native binary:**

```bash
./target/foto-gallery-qrcode-generator \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.event-name="My Photo Event" \
  --app.code-count=50
```

This generates `codes.csv` with 50 codes like:

```
XY9G-AB7K-92QF
XY9G-TK2H-88PL
XY9G-MN5R-AA11
...
```

The CSV file includes a header row (`Number,Code,Event Name`) with numbered rows and the event name.

**Options:**

| Property              | Default                                    | Description                                          |
|-----------------------|--------------------------------------------|------------------------------------------------------|
| `app.event-code`      | *(blank — prompted interactively if unset)* | 4-character alphanumeric event prefix (e.g. `XY9G`) |
| `app.code-count`      | `50`                                       | Number of codes to generate                          |
| `app.csv-output-path` | `codes.csv`                                | Output CSV file path                                 |
| `app.event-name`      | *(empty)*                                  | Event name for CSV column & PDF label                |

### 2. Generate PDF with QR Codes

Reads the CSV file and produces a PDF with QR codes arranged in a grid.

**Using JAR:**

```bash
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-pdf
```

**Using native binary:**

```bash
./target/foto-gallery-qrcode-generator \
  --app.mode=generate-pdf
```

This reads `codes.csv` and generates `qr-codes.pdf` with a 3×4 grid of QR codes per page.
Each QR code links to `https://my.site/gallery/{code}`, shows a sequential number overlay in the centre,
and has the code printed below. When the CSV contains an event name, it appears above the code.
Optionally, dashed cutting lines can be drawn between cells with `--app.show-cutting-lines=true`.

**Options:**

| Property                 | Default                    | Description                      |
|--------------------------|----------------------------|----------------------------------|
| `app.csv-input-path`     | `codes.csv`                | Input CSV file path              |
| `app.output-path`        | `qr-codes.pdf`             | Output PDF file path             |
| `app.base-url`           | `https://my.site/gallery/` | Base URL for QR codes            |
| `app.qr-size`            | `200`                      | QR code image size in px         |
| `app.grid-columns`       | `3`                        | Columns per page                 |
| `app.grid-rows`          | `4`                        | Rows per page                    |
| `app.show-cutting-lines` | `false`                    | Draw dashed cutting lines on PDF |

### Full Workflow Example

```bash
# Step 1: Generate 50 codes for event XY9G
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.event-name="My Photo Event" \
  --app.code-count=50

# Step 2: Generate the PDF (event name is read from CSV)
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-pdf
```

### Convenience Scripts

Two scripts in the project root combine both steps into a single command.
They auto-detect whether the native binary or the JAR is available (native takes
precedence).

**Linux / macOS (`generate-qrcodes.sh`):**

```bash
# Interactive mode — no arguments
./generate-qrcodes.sh

# Positional arguments
./generate-qrcodes.sh <EVENT_CODE> [CODE_COUNT] [EVENT_NAME] [EXTRA_ARGS...]

# Pass --app.* flags directly (bypasses positional parsing)
./generate-qrcodes.sh --app.mode=generate-codes --app.event-code=XY9G

# Examples
./generate-qrcodes.sh XY9G                                                        # 50 codes (default)
./generate-qrcodes.sh XY9G 100                                                    # 100 codes
./generate-qrcodes.sh XY9G 100 "My Photo Event"
./generate-qrcodes.sh XY9G 100 "My Photo Event" --app.base-url=https://my.site/gallery/
```

**Windows (`generate-qrcodes.bat`):**

```cmd
rem Interactive mode — no arguments
generate-qrcodes.bat

rem Positional arguments
generate-qrcodes.bat <EVENT_CODE> [CODE_COUNT] [EVENT_NAME] [EXTRA_ARGS...]

rem Pass --app.* flags directly (bypasses positional parsing)
generate-qrcodes.bat --app.mode=generate-codes --app.event-code=XY9G

rem Examples
generate-qrcodes.bat XY9G
generate-qrcodes.bat XY9G 100
generate-qrcodes.bat XY9G 100 "My Photo Event"
generate-qrcodes.bat XY9G 100 "My Photo Event" --app.base-url=https://my.site/gallery/
```

| Argument       | Required  | Default   | Description                                            |
|----------------|-----------|-----------|--------------------------------------------------------|
| `EVENT_CODE`   | yes\*     | —         | 4-character alphanumeric event prefix (e.g. `XY9G`)    |
| `CODE_COUNT`   | no        | `50`      | Number of codes to generate                            |
| `EVENT_NAME`   | no        | *(empty)* | Event name for CSV header & PDF label                  |
| `EXTRA_ARGS`   | no        | —         | Any additional `--app.*` options passed to both steps  |

\* Required when using positional arguments; omit all arguments to use interactive mode instead.

### School Photo Scripts (mel-rohrer.ch/schulfotos)

Two dedicated scripts for generating school photo gallery codes on `mel-rohrer.ch/schulfotos`.
The base URL `https://mel-rohrer.ch/schulfotos/?code=` is hardcoded, cutting lines are enabled by
default, and a back page with the gallery URL `https://mel-rohrer.ch/schulfotos` is added
automatically. Output files are named after the class (e.g. `GS1d-BA-codes.csv` and `GS1d-BA-qr-codes.pdf`).

**Linux / macOS (`schulfotos-mel-rohrer.sh`):**

```bash
./schulfotos-mel-rohrer.sh <EVENT_CODE> <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]

# Examples
./schulfotos-mel-rohrer.sh XY9G "GS1d BA"
./schulfotos-mel-rohrer.sh XY9G "GS1d BA" 30
./schulfotos-mel-rohrer.sh XY9G "GS1d BA" 30 --app.show-cutting-lines=true
```

**Windows (`schulfotos-mel-rohrer.bat`):**

```cmd
schulfotos-mel-rohrer.bat <EVENT_CODE> <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]

rem Examples
schulfotos-mel-rohrer.bat XY9G "GS1d BA"
schulfotos-mel-rohrer.bat XY9G "GS1d BA" 30
schulfotos-mel-rohrer.bat XY9G "GS1d BA" 30 --app.show-cutting-lines=true
```

| Argument       | Required | Default | Description                                           |
|----------------|----------|---------|-------------------------------------------------------|
| `EVENT_CODE`   | yes      | —       | 4-character alphanumeric code prefix (e.g. `XY9G`)   |
| `KLASSENNAME`  | yes      | —       | Class name used as the event label in the PDF         |
| `CODE_COUNT`   | no       | `50`    | Number of codes to generate                           |
| `EXTRA_ARGS`   | no       | —       | Any additional `--app.*` options passed to both steps |
