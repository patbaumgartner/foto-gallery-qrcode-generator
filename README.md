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
Base URL (displayed on back of PDF) [https://my.site]:
Gallery URL for QR codes (must start with https://) [https://my.site/gallery?code=]:
QR code size (pixels) [200]:
Grid columns per page [3]:
Grid rows per page [4]:
Show cutting lines (yes/no) [no]: yes
Logo URL for back page (JPEG/PNG) [logo.png]:
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
  --app.code-count=17
```

**Using native binary:**

```bash
./target/foto-gallery-qrcode-generator \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.event-name="My Photo Event" \
  --app.code-count=17
```

This generates `codes.csv` with 17 codes like:

```
XY9G-AB7K-92QF
XY9G-TK2H-88PL
XY9G-MN5R-AA11
...
```

The CSV file includes a header row (`Number,Code,Password,Event Name,URL`) with numbered rows, the event name, and the full gallery URL.

**Options:**

| Property              | Default                                    | Description                                          |
|-----------------------|--------------------------------------------|------------------------------------------------------|
| `app.event-code`      | *(blank — prompted interactively if unset)* | 4-character alphanumeric event prefix (e.g. `XY9G`) |
| `app.code-count`      | `17`                                       | Number of codes to generate                          |
| `app.csv-output-path` | `codes.csv`                                | Output CSV file path                                 |
| `app.event-name`      | *(empty)*                                  | Event name for CSV column & PDF label                |
| `app.gallery-url`     | `https://my.site/gallery?code=`            | Full URL used in CSV URL column (gallery URL + code) |

### 2. Generate PDF with QR Codes

Reads the CSV file and produces a PDF with QR codes arranged in a grid. Each page pair
consists of a **front page** (QR code + gallery code) and a **back page** (gallery password +
base URL, for duplex printing).

**Using JAR:**

```bash
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-pdf \
  --app.gallery-url=https://my.site/gallery?code= \
  --app.base-url=https://my.site
```

**Using native binary:**

```bash
./target/foto-gallery-qrcode-generator \
  --app.mode=generate-pdf \
  --app.gallery-url=https://my.site/gallery?code= \
  --app.base-url=https://my.site
```

This reads `codes.csv` and generates `qr-codes.pdf` with a 3×4 grid of QR codes per page.
Each QR code encodes the full gallery URL (`app.gallery-url` + code), shows a sequential
number overlay in the centre, and has the code printed below. When the CSV contains an event
name, it appears above the code. The back of every page always contains the gallery password
and the base URL (`app.base-url`). Optionally, dashed cutting lines can be drawn between
cells with `--app.show-cutting-lines=true`.

**Options:**

| Property                 | Default                           | Description                                               |
|--------------------------|-----------------------------------|-----------------------------------------------------------|
| `app.csv-input-path`     | `codes.csv`                       | Input CSV file path                                       |
| `app.output-path`        | `qr-codes.pdf`                    | Output PDF file path                                      |
| `app.gallery-url`        | `https://my.site/gallery?code=`   | Full URL used in QR codes (must start with `https://`)    |
| `app.base-url`           | `https://my.site`                 | Base URL printed on the back of the PDF                   |
| `app.logo-url`           | *(empty)*                         | Logo image for back page (JPEG, PNG, or WebP)             |
| `app.qr-size`            | `200`                             | QR code image size in px                                  |
| `app.grid-columns`       | `3`                               | Columns per page                                          |
| `app.grid-rows`          | `4`                               | Rows per page                                             |
| `app.show-cutting-lines` | `false`                           | Draw dashed cutting lines on PDF                          |

### Full Workflow Example

```bash
# Step 1: Generate 17 codes for event XY9G
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-codes \
  --app.event-code=XY9G \
  --app.event-name="My Photo Event" \
  --app.code-count=17

# Step 2: Generate the PDF (event name is read from CSV)
java -jar target/foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar \
  --app.mode=generate-pdf \
  --app.gallery-url=https://my.site/gallery?code= \
  --app.base-url=https://my.site
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
./generate-qrcodes.sh XY9G                                                        # 17 codes (default)
./generate-qrcodes.sh XY9G 100                                                    # 100 codes
./generate-qrcodes.sh XY9G 100 "My Photo Event"
./generate-qrcodes.sh XY9G 100 "My Photo Event" --app.gallery-url=https://my.site/gallery?code=
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
generate-qrcodes.bat XY9G 100 "My Photo Event" --app.gallery-url=https://my.site/gallery?code=
```

| Argument       | Required  | Default   | Description                                            |
|----------------|-----------|-----------|--------------------------------------------------------|
| `EVENT_CODE`   | yes\*     | —         | 4-character alphanumeric event prefix (e.g. `XY9G`)    |
| `CODE_COUNT`   | no        | `17`      | Number of codes to generate                            |
| `EVENT_NAME`   | no        | *(empty)* | Event name for CSV header & PDF label                  |
| `EXTRA_ARGS`   | no        | —         | Any additional `--app.*` options passed to both steps  |

\* Required when using positional arguments; omit all arguments to use interactive mode instead.

### School Photo Scripts (mel-rohrer.ch/schulfotos)

Two dedicated scripts for generating school photo gallery codes on `mel-rohrer.ch/schulfotos`.
The gallery URL `https://mel-rohrer.ch/schulfotos/?code=` is used for QR codes and CSV URL entries,
the base URL `https://mel-rohrer.ch/schulfotos` is printed on the back page, cutting lines are enabled by
default, the classpath logo (`logo.png`) is rendered on the back page, and a back page with the gallery
password is always included.
A random 4-character `EVENT_CODE` is generated automatically (no need to pass it).
Output files are named after the class (e.g. `GS1d-BA-codes.csv` and `GS1d-BA-qr-codes.pdf`).

By default, Spring Boot log output is suppressed. Pass `-v` or `--verbose` to show it.

**Linux / macOS (`schulfotos-mel-rohrer.sh`):**

```bash
./schulfotos-mel-rohrer.sh [OPTIONS] <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]

# Examples
./schulfotos-mel-rohrer.sh "GS1d BA"              # 17 codes, random EVENT_CODE
./schulfotos-mel-rohrer.sh "GS1d BA" 30
./schulfotos-mel-rohrer.sh -v "GS1d BA"            # verbose output
```

**Windows (`schulfotos-mel-rohrer.bat`):**

```cmd
schulfotos-mel-rohrer.bat [OPTIONS] <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]

rem Examples
schulfotos-mel-rohrer.bat "GS1d BA"
schulfotos-mel-rohrer.bat "GS1d BA" 30
schulfotos-mel-rohrer.bat -v "GS1d BA"
```

| Argument       | Required | Default | Description                                           |
|----------------|----------|---------|-------------------------------------------------------|
| `-v`           | no       | off     | Show Spring Boot log output (verbose mode)            |
| `KLASSENNAME`  | yes      | —       | Class name used as the event label in the PDF         |
| `CODE_COUNT`   | no       | `17`    | Number of codes to generate                           |
| `EXTRA_ARGS`   | no       | —       | Any additional `--app.*` options passed to both steps |

## PicPeak Integration

The application can automatically create gallery events on a
[PicPeak](https://picpeak.app) instance for every generated access code and
write the resulting share link back into the CSV file.

### Quick Start

1. Copy `picpeak.properties.example` to `picpeak.properties` in the same
   directory as the script / JAR:

   ```bash
   cp picpeak.properties.example picpeak.properties
   ```

2. Open `picpeak.properties` and fill in at least the required fields:

   ```properties
   app.picpeak.enabled=true
   app.picpeak.api-url=https://pics.example.com
   app.picpeak.username=admin@example.com
   app.picpeak.password=secret
   app.picpeak.customer-email=customer@example.com
   ```

3. Run any of the scripts as usual — `picpeak.properties` is picked up
   automatically:

   ```bash
   ./schulfotos-mel-rohrer.sh "GS1d BA"
   ./generate-qrcodes.sh XY9G 50 "My Event"
   ```

`picpeak.properties` is listed in `.gitignore` and will never be committed to
version control.

### All PicPeak Settings

| Property | Default | Description |
|---|---|---|
| `app.picpeak.enabled` | `false` | Set to `true` to activate the integration |
| `app.picpeak.api-url` | `https://pics.example.com` | Base URL of your PicPeak instance (no trailing slash) |
| `app.picpeak.username` | *(blank)* | Admin login username |
| `app.picpeak.password` | *(blank)* | Admin login password |
| `app.picpeak.event-type` | `schule` | Event type passed to the PicPeak API |
| `app.picpeak.event-date` | *(today)* | Fixed event date (`YYYY-MM-DD`); defaults to today when blank |
| `app.picpeak.customer-email` | *(blank)* | Customer e-mail shown in the gallery event |
| `app.picpeak.admin-email` | *(customer-email)* | Admin notification e-mail; falls back to `customer-email` |
| `app.picpeak.require-password` | `true` | Require a password to view the gallery |
| `app.picpeak.welcome-message` | *(blank)* | Welcome message shown to gallery visitors |
| `app.picpeak.expiration-days` | `30` | Days until the gallery expires |
| `app.picpeak.allow-user-uploads` | `false` | Allow visitors to upload photos |
| `app.picpeak.feedback-enabled` | `true` | Enable visitor feedback |
| `app.picpeak.allow-ratings` | `true` | Allow photo ratings |
| `app.picpeak.allow-likes` | `false` | Allow photo likes |
| `app.picpeak.allow-comments` | `false` | Allow photo comments |
| `app.picpeak.allow-favorites` | `true` | Allow photo favourites |
| `app.picpeak.require-name-email` | `false` | Require name & e-mail before viewing |
| `app.picpeak.moderate-comments` | `true` | Hold comments for moderation |
| `app.picpeak.show-feedback-to-guests` | `false` | Show feedback to unauthenticated guests |
| `app.picpeak.header-style` | `minimal` | Header style (`minimal`, `classic`, `hero`) |
| `app.picpeak.hero-divider-style` | `wave` | Hero divider style |
| `app.picpeak.css-template-id` | `2` | CSS template ID (integer) |

### Alternative: Environment Variables

Spring Boot's relaxed-binding maps environment variables to properties, so you
can also supply credentials without a file:

```bash
export APP_PICPEAK_ENABLED=true
export APP_PICPEAK_API_URL=https://pics.example.com
export APP_PICPEAK_USERNAME=admin@example.com
export APP_PICPEAK_PASSWORD=secret
export APP_PICPEAK_CUSTOMER_EMAIL=customer@example.com

./schulfotos-mel-rohrer.sh "GS1d BA"
```

### Alternative: Command-Line Flags

You can also pass any PicPeak setting as an `EXTRA_ARGS` flag:

```bash
./generate-qrcodes.sh XY9G 50 "My Event" \
  --app.picpeak.enabled=true \
  --app.picpeak.api-url=https://pics.example.com \
  --app.picpeak.username=admin@example.com \
  --app.picpeak.password=secret
```

