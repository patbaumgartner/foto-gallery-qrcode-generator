@echo off
rem schulfotos-mel-rohrer.bat — Generate school photo gallery codes for mel-rohrer.ch/schulfotos.
rem
rem The base URL is hardcoded to https://mel-rohrer.ch/schulfotos/ and all standard
rem settings are used (50 codes, 3x4 grid, 200 px QR size, no cutting lines).
rem
rem Usage:
rem   schulfotos-mel-rohrer.bat <EVENT_CODE> <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]
rem
rem Arguments:
rem   EVENT_CODE    4-character alphanumeric code prefix (e.g. XY9G)
rem   KLASSENNAME   Class name used as the event label in the PDF (e.g. "Klasse 3A")
rem   CODE_COUNT    Number of codes to generate (default: 50)
rem   EXTRA_ARGS    Any additional --app.* flags passed to both steps
rem
rem Examples:
rem   schulfotos-mel-rohrer.bat XY9G "Klasse 3A"
rem   schulfotos-mel-rohrer.bat XY9G "Klasse 3A" 30
rem   schulfotos-mel-rohrer.bat XY9G "Klasse 3A" 30 --app.show-cutting-lines=true

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_NAME=foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
set "NATIVE_NAME=foto-gallery-qrcode-generator.exe"
set "BASE_URL=https://mel-rohrer.ch/schulfotos/"

rem --- Resolve executable -----------------------------------------------------
rem Check current directory first, then target\ subdirectory
set "USE_JAR=0"
if exist "%SCRIPT_DIR%%NATIVE_NAME%" (
    set "RUN=%SCRIPT_DIR%%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%target\%NATIVE_NAME%" (
    set "RUN=%SCRIPT_DIR%target\%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%%JAR_NAME%" (
    set "USE_JAR=1"
    set "RUN=%SCRIPT_DIR%%JAR_NAME%"
) else if exist "%SCRIPT_DIR%target\%JAR_NAME%" (
    set "USE_JAR=1"
    set "RUN=%SCRIPT_DIR%target\%JAR_NAME%"
) else (
    echo ERROR: No executable found ^(neither '%NATIVE_NAME%' nor '%JAR_NAME%'^). >&2
    exit /b 1
)

rem --- Argument validation ----------------------------------------------------
if "%~1"=="" (
    echo Usage: %~nx0 ^<EVENT_CODE^> ^<KLASSENNAME^> [CODE_COUNT] [EXTRA_ARGS...] >&2
    echo. >&2
    echo   EVENT_CODE    4-character alphanumeric code prefix ^(e.g. XY9G^) >&2
    echo   KLASSENNAME   Class name for the PDF label ^(e.g. "Klasse 3A"^) >&2
    echo   CODE_COUNT    Number of codes to generate ^(default: 50^) >&2
    exit /b 1
)
if "%~2"=="" (
    echo Usage: %~nx0 ^<EVENT_CODE^> ^<KLASSENNAME^> [CODE_COUNT] [EXTRA_ARGS...] >&2
    echo. >&2
    echo   EVENT_CODE    4-character alphanumeric code prefix ^(e.g. XY9G^) >&2
    echo   KLASSENNAME   Class name for the PDF label ^(e.g. "Klasse 3A"^) >&2
    echo   CODE_COUNT    Number of codes to generate ^(default: 50^) >&2
    exit /b 1
)

rem --- Parse arguments --------------------------------------------------------
set "EVENT_CODE=%~1"
set "KLASSENNAME=%~2"
shift
shift

set "CODE_COUNT=50"
set "NEXT=%~1"
if defined NEXT (
    echo %NEXT%| findstr /r "^[0-9][0-9]*$" >nul 2>&1
    if not errorlevel 1 (
        set "CODE_COUNT=%NEXT%"
        shift
    )
)

rem Collect remaining extra arguments
set "EXTRA_ARGS="
:parse_extra
if "%~1"=="" goto done_extra
set "EXTRA_ARGS=!EXTRA_ARGS! %~1"
shift
goto parse_extra
:done_extra

rem --- Derive safe file name prefix from class name ---------------------------
rem Replace spaces and forward slashes with hyphens
set "SAFE_NAME=%KLASSENNAME: =-%"
set "SAFE_NAME=%SAFE_NAME:/=-%"
set "CSV_PATH=%SAFE_NAME%-codes.csv"
set "PDF_PATH=%SAFE_NAME%-qr-codes.pdf"

rem --- Step 1: Generate codes -------------------------------------------------
echo ==^> Generating %CODE_COUNT% codes for class '%KLASSENNAME%' ^(event: %EVENT_CODE%^) ...
if "%USE_JAR%"=="1" (
    java -jar "%RUN%" --app.mode=generate-codes --app.event-code=%EVENT_CODE% --app.code-count=%CODE_COUNT% --app.event-name="%KLASSENNAME%" --app.csv-output-path="%CSV_PATH%" --app.base-url=%BASE_URL% %EXTRA_ARGS%
) else (
    "%RUN%" --app.mode=generate-codes --app.event-code=%EVENT_CODE% --app.code-count=%CODE_COUNT% --app.event-name="%KLASSENNAME%" --app.csv-output-path="%CSV_PATH%" --app.base-url=%BASE_URL% %EXTRA_ARGS%
)
if errorlevel 1 (
    echo ERROR: Code generation failed. >&2
    exit /b 1
)

rem --- Step 2: Generate PDF ---------------------------------------------------
echo ==^> Generating QR-code PDF ...
if "%USE_JAR%"=="1" (
    java -jar "%RUN%" --app.mode=generate-pdf --app.csv-input-path="%CSV_PATH%" --app.output-path="%PDF_PATH%" --app.base-url=%BASE_URL% %EXTRA_ARGS%
) else (
    "%RUN%" --app.mode=generate-pdf --app.csv-input-path="%CSV_PATH%" --app.output-path="%PDF_PATH%" --app.base-url=%BASE_URL% %EXTRA_ARGS%
)
if errorlevel 1 (
    echo ERROR: PDF generation failed. >&2
    exit /b 1
)

echo ==^> Done. Output files:
echo     CSV: %CSV_PATH%
echo     PDF: %PDF_PATH%
endlocal
