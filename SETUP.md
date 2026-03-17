# Setup Guide

Step-by-step instructions to set up, build, and run the Foto Gallery QR Code Generator from scratch.

## Prerequisites

- **Java 25** — [Temurin 25](https://adoptium.net/temurin/releases/?version=25) or any JDK 25+
- **Maven** — included via `./mvnw` wrapper (no separate install needed)
- **GraalVM 25 with `native-image`** — only required for native binary builds

Verify your Java version:

```bash
java -version   # must report 25
```

## Clone

```bash
git clone https://github.com/patbaumgartner/foto-gallery-qrcode-generator.git
cd foto-gallery-qrcode-generator
```

## Build

```bash
# Build fat JAR (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test
```

The JAR is produced at `target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar`.

## Run

### Option 1 — Interactive shell (recommended for first use)

```bash
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar
```

The shell prompts for mode, event code, code count, file paths, and PDF options.

### Option 2 — Convenience script (Linux / macOS)

```bash
chmod +x generate-qrcodes.sh

# Interactive shell
./generate-qrcodes.sh

# Generate codes + PDF in one command
./generate-qrcodes.sh <EVENT_CODE> [CODE_COUNT] ["Event Name"]
# Example:
./generate-qrcodes.sh XY9G 50 "School Photos 2025"
```

Output lands in `generated/codes.csv` and `generated/qr-codes.pdf`.

### Option 3 — CLI flags

```bash
JAR=target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar

# Step 1 — generate access codes
java -jar $JAR \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.code-count=50 \
  --app.event-name="School Photos 2025" \
  --app.csv-output-path=generated/codes.csv

# Step 2 — generate QR-code PDF
java -jar $JAR \
  --app.mode=generate-pdf \
  --app.csv-input-path=generated/codes.csv \
  --app.output-path=generated/qr-codes.pdf \
  --app.gallery-url=https://my.site/gallery?code= \
  --app.base-url=https://my.site
```

## Key Configuration Properties

All properties live in `src/main/resources/application.properties` and can be overridden via `--app.*` flags or an external properties file.

| Property | Default | Description |
|---|---|---|
| `app.mode` | *(blank)* | `generate-codes`, `generate-pdf`, or blank for interactive |
| `app.event-code` | *(blank)* | 4-char alphanumeric prefix (e.g. `XY9G`) |
| `app.event-name` | *(blank)* | Label printed on codes and PDF |
| `app.code-count` | `17` | Number of codes to generate |
| `app.csv-output-path` | `codes.csv` | Output CSV path |
| `app.csv-input-path` | `codes.csv` | Input CSV path for PDF generation |
| `app.output-path` | `qr-codes.pdf` | Output PDF path |
| `app.gallery-url` | `https://my.site/gallery?code=` | URL embedded in QR codes (must end with `=` or `/`) |
| `app.base-url` | `https://my.site` | URL printed on the back of each card |
| `app.logo-url` | *(blank)* | Logo for back page (JPEG / PNG / WebP) |
| `app.qr-size` | `200` | QR image size in pixels |
| `app.grid-columns` | `3` | Columns per PDF page |
| `app.grid-rows` | `4` | Rows per PDF page |
| `app.show-cutting-lines` | `false` | Draw dashed cutting guides |

## PicPeak Integration (optional)

PicPeak automatically creates gallery events and writes share links into the CSV.

1. Copy the example file and fill in your credentials:

   ```bash
   cp picpeak.properties.example picpeak.properties
   # edit picpeak.properties — set api-url, username, password, customer-email
   ```

2. The convenience script picks up `picpeak.properties` automatically. To use it manually:

   ```bash
   java -jar $JAR \
     --spring.config.additional-location=file:picpeak.properties \
     --app.mode=generate-codes \
     --app.event-code=XY9G
   ```

`picpeak.properties` is listed in `.gitignore` and will never be committed.

## Native Binary (optional)

Requires GraalVM 25 with the `native-image` component.

```bash
# Build native executable
./mvnw clean package -Pnative -DskipTests

# Run directly (no JVM needed)
./target/foto-gallery-qrcode-generator
# or via the convenience script — it auto-detects the native binary
./generate-qrcodes.sh XY9G 50 "School Photos 2025"
```
