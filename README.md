# Foto Gallery QR Code Generator

A Spring Boot CLI application that generates gallery access codes and produces PDF documents with QR codes.

## Prerequisites

- **Java 21** (or later)
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

| Property             | Default      | Description                            |
|----------------------|--------------|----------------------------------------|
| `app.event-code`     | *(required)* | 4-character event prefix (e.g. `XY9G`) |
| `app.code-count`     | `50`         | Number of codes to generate            |
| `app.csv-output-path`| `codes.csv`  | Output CSV file path                   |
| `app.event-name`     | *(empty)*    | Event name for CSV column & PDF label  |

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
Each QR code links to `https://my.site/gallery/{code}`, shows a sequential number overlay in the center,
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
./generate-qrcodes.sh <EVENT_CODE> [CODE_COUNT] [EVENT_NAME] [EXTRA_ARGS...]

# Examples
./generate-qrcodes.sh XY9G            # 50 codes (default)
./generate-qrcodes.sh XY9G 100        # 100 codes
./generate-qrcodes.sh XY9G 100 "My Photo Event"
./generate-qrcodes.sh XY9G 100 "My Photo Event" --app.base-url=https://my.site/gallery/
```

**Windows (`generate-qrcodes.bat`):**

```cmd
generate-qrcodes.bat <EVENT_CODE> [CODE_COUNT] [EVENT_NAME] [EXTRA_ARGS...]

rem Examples
generate-qrcodes.bat XY9G
generate-qrcodes.bat XY9G 100
generate-qrcodes.bat XY9G 100 "My Photo Event"
generate-qrcodes.bat XY9G 100 "My Photo Event" --app.base-url=https://my.site/gallery/
```

| Argument       | Required | Default   | Description                                          |
|----------------|----------|-----------|------------------------------------------------------|
| `EVENT_CODE`   | yes      | —         | 4-character event prefix (e.g. `XY9G`)               |
| `CODE_COUNT`   | no       | `50`      | Number of codes to generate                          |
| `EVENT_NAME`   | no       | *(empty)* | Event name for CSV header & PDF label                |
| `EXTRA_ARGS`   | no       | —         | Any additional `--app.*` options passed to both steps |
