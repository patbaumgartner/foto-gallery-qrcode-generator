@echo off
:: schulfotos-mel-rohrer.cmd — Generate school photo gallery codes for mel-rohrer.ch/schulfotos.
::
:: Usage:
::   schulfotos-mel-rohrer.cmd                                   Interactive mode
::   schulfotos-mel-rohrer.cmd <KLASSENNAME> [SHOOTING_DATE] [CODE_COUNT] [EXTRA_ARGS...]
::   schulfotos-mel-rohrer.cmd --help
::
:: Options:
::   --help         Show this help message
::
:: Arguments:
::   KLASSENNAME    Class name used as the event label in the PDF (e.g. "GS1d BA")
::   SHOOTING_DATE  Shooting date in German format DD.MM.YYYY (default: today)
::   CODE_COUNT     Number of codes to generate (default: 17)
::   EXTRA_ARGS     Any additional --app.* flags passed to both steps

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set "JAR_NAME=foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
set "NATIVE_NAME=foto-gallery-qrcode-generator.exe"
set "BASE_URL=https://mel-rohrer.ch/schulfotos"
set "GALLERY_URL=https://mel-rohrer.ch/schulfotos/?code="
set "DEFAULT_CODE_COUNT=17"

:: --- Help ---
if /i "%~1"=="--help" goto :show_help
if /i "%~1"=="-h" goto :show_help

:: --- Resolve executable ---
set "RUN="
if exist "%SCRIPT_DIR%\%NATIVE_NAME%" (
    set "RUN=%SCRIPT_DIR%\%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%\target\%NATIVE_NAME%" (
    set "RUN=%SCRIPT_DIR%\target\%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%\%JAR_NAME%" (
    set "RUN=java -jar %SCRIPT_DIR%\%JAR_NAME%"
) else if exist "%SCRIPT_DIR%\target\%JAR_NAME%" (
    set "RUN=java -jar %SCRIPT_DIR%\target\%JAR_NAME%"
) else (
    echo ERROR: No executable found ^(neither '%NATIVE_NAME%' nor '%JAR_NAME%'^). >&2
    exit /b 1
)

:: --- Suppress Spring Boot log output ---
set "QUIET_ARGS=--logging.level.root=WARN --spring.main.banner-mode=off"

:: --- Load optional PicPeak credentials ---
set "PICPEAK_ARG="
if exist "%SCRIPT_DIR%\picpeak.properties" (
    set "PICPEAK_ARG=--spring.config.additional-location=file:%SCRIPT_DIR%\picpeak.properties"
)

:: --- Interactive or non-interactive ---
if "%~1"=="" goto :interactive

:: =============================================================================
:: Non-interactive mode (positional arguments)
:: =============================================================================
set "KLASSENNAME=%~1"
shift

:: Generate a random 4-char event code via PowerShell
for /f "tokens=*" %%i in ('powershell -NoProfile -Command "-join ((65..90 + 48..57) | Get-Random -Count 4 | ForEach-Object {[char]$_})"') do set "EVENT_CODE=%%i"

:: Shooting date (default: today in DD.MM.YYYY)
for /f "tokens=*" %%i in ('powershell -NoProfile -Command "Get-Date -Format dd.MM.yyyy"') do set "DEFAULT_DATE=%%i"
set "SHOOTING_DATE_DE=%DEFAULT_DATE%"
if not "%~1"=="" (
    echo %~1 | findstr /r "^[0-9][0-9]\.[0-9][0-9]\.[0-9][0-9][0-9][0-9]$" >nul 2>&1
    if not errorlevel 1 (
        set "SHOOTING_DATE_DE=%~1"
        shift
    )
)

:: Code count
set "CODE_COUNT=%DEFAULT_CODE_COUNT%"
if not "%~1"=="" (
    echo %~1 | findstr /r "^[0-9][0-9]*$" >nul 2>&1
    if not errorlevel 1 (
        set "CODE_COUNT=%~1"
        shift
    )
)

:: PicPeak: preserve whatever picpeak.properties specifies (no prompt in non-interactive mode)
set "CREATE_GALLERY_EVENTS=from picpeak.properties"
set "PICPEAK_ENABLED_ARG="

goto :run_steps

:: =============================================================================
:: Interactive mode
:: =============================================================================
:interactive
echo ===  Schulfotos ^— mel-rohrer.ch  ===
echo.

set "KLASSENNAME="
set /p "KLASSENNAME=Klassenname (e.g. GS1d BA): "
if "!KLASSENNAME!"=="" (
    echo ERROR: Klassenname must not be empty. >&2
    exit /b 1
)

:: Generate a suggested event code
for /f "tokens=*" %%i in ('powershell -NoProfile -Command "-join ((65..90 + 48..57) | Get-Random -Count 4 | ForEach-Object {[char]$_})"') do set "SUGGESTED_CODE=%%i"
set "EVENT_CODE_INPUT="
set /p "EVENT_CODE_INPUT=Event-Code [!SUGGESTED_CODE!]: "
if "!EVENT_CODE_INPUT!"=="" (set "EVENT_CODE=!SUGGESTED_CODE!") else (set "EVENT_CODE=!EVENT_CODE_INPUT!")

:: Shooting date
for /f "tokens=*" %%i in ('powershell -NoProfile -Command "Get-Date -Format dd.MM.yyyy"') do set "DEFAULT_DATE=%%i"
set "SHOOTING_DATE_INPUT="
set /p "SHOOTING_DATE_INPUT=Shooting-Datum (DD.MM.YYYY) [!DEFAULT_DATE!]: "
if "!SHOOTING_DATE_INPUT!"=="" (set "SHOOTING_DATE_DE=!DEFAULT_DATE!") else (set "SHOOTING_DATE_DE=!SHOOTING_DATE_INPUT!")

:: Code count
set "CODE_COUNT_INPUT="
set /p "CODE_COUNT_INPUT=Anzahl Codes [%DEFAULT_CODE_COUNT%]: "
if "!CODE_COUNT_INPUT!"=="" (set "CODE_COUNT=%DEFAULT_CODE_COUNT%") else (set "CODE_COUNT=!CODE_COUNT_INPUT!")

:: PicPeak gallery creation prompt (only when picpeak.properties exists)
set "CREATE_GALLERY_EVENTS=false"
set "PICPEAK_ENABLED_ARG=--app.picpeak.enabled=false"
if exist "%SCRIPT_DIR%\picpeak.properties" (
    set "PICPEAK_INPUT="
    set /p "PICPEAK_INPUT=PicPeak-Galerie-Events erstellen? (yes/no) [no]: "
    if /i "!PICPEAK_INPUT!"=="y" set "PICPEAK_INPUT=yes"
    if /i "!PICPEAK_INPUT!"=="yes" (
        set "CREATE_GALLERY_EVENTS=true"
        set "PICPEAK_ENABLED_ARG=--app.picpeak.enabled=true"
    )
)

echo.

:: =============================================================================
:: Shared: convert date, derive paths, print summary, run steps
:: =============================================================================
:run_steps

:: Convert DD.MM.YYYY to YYYY-MM-DD via PowerShell
for /f "tokens=*" %%i in ('powershell -NoProfile -Command "try { [datetime]::ParseExact(\"!SHOOTING_DATE_DE!\",\"dd.MM.yyyy\",$null).ToString(\"yyyy-MM-dd\") } catch { exit 1 }"') do set "SHOOTING_DATE=%%i"
if "!SHOOTING_DATE!"=="" (
    echo ERROR: Invalid date format '!SHOOTING_DATE_DE!'. Expected DD.MM.YYYY. >&2
    exit /b 1
)

:: Derive output filenames (replace spaces with hyphens)
set "SAFE_NAME=!KLASSENNAME: =-!"
set "OUTPUT_DIR=schulfotos"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
set "CSV_PATH=%OUTPUT_DIR%\!SAFE_NAME!-codes.csv"
set "PDF_PATH=%OUTPUT_DIR%\!SAFE_NAME!-qr-codes.pdf"

:: Summary
echo ==^> Settings:
echo     Klassenname    : !KLASSENNAME!
echo     Event-Code     : !EVENT_CODE!
echo     Shooting-Datum : !SHOOTING_DATE_DE! (!SHOOTING_DATE!)
echo     Code Count     : !CODE_COUNT!
echo     PicPeak Events : !CREATE_GALLERY_EVENTS!
echo     CSV            : !CSV_PATH!
echo     PDF            : !PDF_PATH!
echo.

:: Step 1: Generate codes
echo ==^> Generating !CODE_COUNT! codes for class '!KLASSENNAME!' (event: !EVENT_CODE!) ...
%RUN% ^
  --app.mode=generate-codes ^
  --app.event-code="!EVENT_CODE!" ^
  --app.code-count="!CODE_COUNT!" ^
  --app.event-name="!KLASSENNAME!" ^
  --app.csv-output-path="!CSV_PATH!" ^
  --app.gallery-url="%GALLERY_URL%" ^
  --app.picpeak.event-date="!SHOOTING_DATE!" ^
  !PICPEAK_ENABLED_ARG! ^
  %PICPEAK_ARG% ^
  %QUIET_ARGS%
if errorlevel 1 (
    echo ERROR: Code generation failed. >&2
    exit /b 1
)

:: Step 2: Generate PDF
echo ==^> Generating QR-code PDF ...
%RUN% ^
  --app.mode=generate-pdf ^
  --app.csv-input-path="!CSV_PATH!" ^
  --app.output-path="!PDF_PATH!" ^
  --app.base-url="%BASE_URL%" ^
  --app.gallery-url="%GALLERY_URL%" ^
  --app.show-cutting-lines=true ^
  --app.logo-url=logo.png ^
  %PICPEAK_ARG% ^
  %QUIET_ARGS%
if errorlevel 1 (
    echo ERROR: PDF generation failed. >&2
    exit /b 1
)

echo ==^> Done. Output files:
echo     CSV: !CSV_PATH!
echo     PDF: !PDF_PATH!
exit /b 0

:: =============================================================================
:show_help
echo schulfotos-mel-rohrer.cmd ^— Generate school photo gallery codes ^& QR-code PDFs.
echo.
echo Usage:
echo   %~nx0                                   Interactive mode
echo   %~nx0 ^<KLASSENNAME^> [SHOOTING_DATE] [CODE_COUNT]
echo   %~nx0 --help                            Show this help message
echo.
echo Arguments:
echo   KLASSENNAME    Class name used as event label in the PDF (e.g. "GS1d BA")
echo   SHOOTING_DATE  Shooting date in DD.MM.YYYY format (default: today)
echo   CODE_COUNT     Number of codes to generate (default: %DEFAULT_CODE_COUNT%)
echo.
echo Defaults:
echo   Base URL (back of PDF^)  %BASE_URL%
echo   Gallery URL (QR codes^)  %GALLERY_URL%
echo   Grid                    3 columns x 4 rows
echo   QR size                 200 px
echo   Cutting lines           on
exit /b 0
