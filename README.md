# Foto Gallery QR Code Generator

A Spring Boot CLI application that generates gallery access codes and produces PDF documents with QR codes.

## Build

### JVM (JAR)

```bash
mvn clean package -DskipTests
```

### GraalVM Native Image

Requires GraalVM with `native-image` installed.

```bash
mvn clean package -Pnative -DskipTests
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
  --app.code-count=50
```

**Using native binary:**

```bash
./target/foto-gallery-qrcode-generator \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.code-count=50
```

This generates `codes.csv` with 50 codes like:

```
XY9G-AB7K-92QF
XY9G-TK2H-88PL
XY9G-MN5R-AA11
...
```

**Options:**

| Property         | Default        | Description                              |
|------------------|----------------|------------------------------------------|
| `app.event-code` | *(required)*   | 4-character event prefix (e.g. `XY9G`)   |
| `app.code-count` | `50`           | Number of codes to generate              |
| `app.input-path` | `codes.csv`    | Output CSV file path                     |

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

This reads `codes.csv` and generates `qr-codes.pdf` with a 3x4 grid of QR codes per page.
Each QR code links to `https://my.site/gallery/{code}` and has the code printed below.
Optionally, dashed cutting lines can be drawn between cells with `--app.show-cutting-lines=true`.

**Options:**

| Property                 | Default                              | Description                       |
|--------------------------|--------------------------------------|-----------------------------------|
| `app.input-path`         | `codes.csv`                          | Input CSV file path               |
| `app.output-path`        | `qr-codes.pdf`                       | Output PDF file path              |
| `app.base-url`           | `https://my.site/gallery/`           | Base URL for QR codes             |
| `app.qr-size`            | `200`                                | QR code image size in px          |
| `app.grid-columns`       | `3`                                  | Columns per page                  |
| `app.grid-rows`          | `4`                                  | Rows per page                     |
| `app.show-cutting-lines` | `false`                              | Draw dashed cutting lines on PDF  |

### Full Workflow Example

```bash
# Step 1: Generate 50 codes for event XY9G
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.code-count=50

# Step 2: Generate the PDF
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-pdf
```

### Convenience Scripts

Two scripts in the project root combine both steps into a single command.
They auto-detect whether the native binary or the JAR is available (native takes
precedence).

**Linux / macOS (`generate-qrcodes.sh`):**

```bash
./generate-qrcodes.sh <EVENT_CODE> [CODE_COUNT] [EXTRA_ARGS...]

# Examples
./generate-qrcodes.sh XY9G            # 50 codes (default)
./generate-qrcodes.sh XY9G 100        # 100 codes
./generate-qrcodes.sh XY9G 100 --app.base-url=https://my.site/gallery/
```

**Windows (`generate-qrcodes.bat`):**

```cmd
generate-qrcodes.bat <EVENT_CODE> [CODE_COUNT] [EXTRA_ARGS...]

rem Examples
generate-qrcodes.bat XY9G
generate-qrcodes.bat XY9G 100
generate-qrcodes.bat XY9G 100 --app.base-url=https://my.site/gallery/
```

| Argument       | Required | Default | Description                                       |
|----------------|----------|---------|---------------------------------------------------|
| `EVENT_CODE`   | yes      | —       | 4-character event prefix (e.g. `XY9G`)            |
| `CODE_COUNT`   | no       | `50`    | Number of codes to generate                       |
| `EXTRA_ARGS`   | no       | —       | Any additional `--app.*` options passed to both steps |
