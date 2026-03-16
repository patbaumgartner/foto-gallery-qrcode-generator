# Copilot Instructions for Foto Gallery QR Code Generator

## Project Overview

This is a **Spring Boot CLI application** (Java 25) that generates gallery access codes and produces PDF documents containing QR codes. It has three operational modes:

- **`generate-codes`**: Creates a CSV file with random gallery access codes (e.g., `XY9G-AB7K-92QF`)
- **`generate-pdf`**: Reads the CSV and produces a PDF with QR codes arranged in a configurable grid layout
- **interactive shell** (default, when `app.mode` is blank): Prompts the user for all settings via stdin; validates input (e.g. event code format) before proceeding

## Technology Stack

- **Language**: Java 25
- **Framework**: Spring Boot 4.x (using `ApplicationRunner`)
- **Build Tool**: Maven (use `./mvnw` wrapper)
- **Key Libraries**:
  - ZXing (QR code generation)
  - PDFBox (PDF generation)
  - Apache Commons CSV (CSV reading/writing)
  - Taikai (architecture testing)

## Project Structure

```
src/main/java/com/fortytwotalents/fotogallery/
├── config/       - AppProperties (Spring @ConfigurationProperties)
├── model/        - GalleryCode, CsvReadResult, PdfOptions (domain model)
├── runner/       - CodeGeneratorRunner, QrCodeGeneratorRunner, InteractiveRunner (CLI runners)
└── service/      - Business logic services (CSV, QR code, PDF generation)

src/test/java/com/fortytwotalents/fotogallery/
├── integration/  - EndToEndIntegrationTest
├── model/        - Unit tests for model classes
├── runner/       - Runner integration tests (including InteractiveRunnerTest)
├── service/      - Service unit tests
└── verification/ - TaikaiVerificationTest (architecture rules)
```

## Build & Test Commands

```bash
# Build JAR (skip tests)
./mvnw clean package -DskipTests

# Build GraalVM native image (requires GraalVM with native-image)
./mvnw clean package -Pnative -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName
```

## Code Conventions

- **Package**: `com.fortytwotalents.fotogallery`
- **Configuration**: All app settings live in `AppProperties` (validated with Jakarta Bean Validation) and are bound via `@ConfigurationProperties(prefix = "app")`
- **Services**: Stateless Spring `@Service` beans; use constructor injection
- **Runners**: `CodeGeneratorRunner` and `QrCodeGeneratorRunner` implement Spring's `ApplicationRunner` and are conditional on `app.mode`; `InteractiveRunner` fires when `app.mode` is blank and collects all settings interactively
- **Testing**: JUnit 5 with Spring Boot Test; Mockito for mocking; Taikai for architecture verification
- **No REST layer**: This is a CLI tool only — do not add web/REST endpoints

## Architecture Rules (enforced by TaikaiVerificationTest)

- Follow standard layered conventions: `config`, `model`, `runner`, `service`
- Services must not depend on runners
- No circular dependencies

## Key Configuration Properties (`app.*`)

| Property | Default | Description |
|---|---|---|
| `app.mode` | *(blank — launches interactive shell)* | `generate-codes`, `generate-pdf`, or blank |
| `app.event-code` | *(blank — prompted interactively if unset)* | 4-character alphanumeric event prefix |
| `app.event-name` | *(empty)* | Event name for CSV and PDF labels |
| `app.code-count` | `50` | Number of codes to generate |
| `app.csv-input-path` | `codes.csv` | CSV input file path (used by generate-pdf) |
| `app.csv-output-path` | `codes.csv` | CSV output file path (used by generate-codes) |
| `app.output-path` | `qr-codes.pdf` | PDF output file path |
| `app.gallery-url` | `https://my.site/gallery?code=` | Full URL used in QR codes and CSV URL column (must start with `https://`) |
| `app.base-url` | `https://my.site` | Base URL printed on the back of the PDF |
| `app.logo-url` | *(empty)* | Logo image for back page (JPEG, PNG, or WebP; empty = no logo) |
| `app.qr-size` | `200` | QR code image size in pixels |
| `app.grid-columns` | `3` | Number of columns per PDF page |
| `app.grid-rows` | `4` | Number of rows per PDF page |
| `app.show-cutting-lines` | `false` | Draw dashed cutting lines between cells |
