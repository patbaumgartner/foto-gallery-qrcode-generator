@echo off
rem schulfotos-mel-rohrer.bat — Generate school photo gallery codes for mel-rohrer.ch/schulfotos.
rem
rem The base URL is hardcoded to https://mel-rohrer.ch/schulfotos/?code= and all standard
rem settings are used (3x4 grid, 200 px QR size, no cutting lines).
rem
rem When run without arguments the script prompts interactively for every parameter.
rem A random 4-character alphanumeric EVENT_CODE is generated automatically
rem and can be accepted or overridden in interactive mode.
rem
rem Usage:
rem   schulfotos-mel-rohrer.bat                                   :: interactive mode
rem   schulfotos-mel-rohrer.bat <KLASSENNAME> [CODE_COUNT] [EXTRA_ARGS...]
rem   schulfotos-mel-rohrer.bat --help
rem
rem Arguments:
rem   KLASSENNAME   Class name used as the event label in the PDF (e.g. "GS1d BA")
rem   CODE_COUNT    Number of codes to generate (default: 17)
rem   EXTRA_ARGS    Any additional --app.* flags passed to both steps
rem
rem Examples:
rem   schulfotos-mel-rohrer.bat
rem   schulfotos-mel-rohrer.bat "GS1d BA"
rem   schulfotos-mel-rohrer.bat "GS1d BA" 30
rem   schulfotos-mel-rohrer.bat "GS1d BA" 30 --app.show-cutting-lines=true

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_NAME=foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
set "NATIVE_NAME=foto-gallery-qrcode-generator.exe"
set "BASE_URL=https://mel-rohrer.ch/schulfotos/?code="
set "DEFAULT_CODE_COUNT=17"

rem --- Help -------------------------------------------------------------------
if "%~1"=="--help" goto show_help
if "%~1"=="-h" goto show_help
goto after_help

:show_help
echo schulfotos-mel-rohrer.bat — Generate school photo gallery codes ^& QR-code PDFs.
echo.
echo Usage:
echo   %~nx0                                   Interactive mode (prompts for all settings)
echo   %~nx0 ^<KLASSENNAME^> [CODE_COUNT] [EXTRA_ARGS...]
echo   %~nx0 --help                            Show this help message
echo.
echo Arguments:
echo   KLASSENNAME   Class name used as the event label in the PDF (e.g. "GS1d BA").
echo                 A random 4-character alphanumeric EVENT_CODE is generated
echo                 automatically. You will be asked to confirm or override it in
echo                 interactive mode.
echo   CODE_COUNT    Number of codes to generate (default: %DEFAULT_CODE_COUNT%)
echo   EXTRA_ARGS    Any additional --app.* flags passed to both steps
echo.
echo Examples:
echo   %~nx0
echo   %~nx0 "GS1d BA"
echo   %~nx0 "GS1d BA" 30
echo   %~nx0 "GS1d BA" 30 --app.show-cutting-lines=true
echo.
echo Defaults:
echo   Base URL         %BASE_URL%
echo   Grid             3 columns x 4 rows
echo   QR size          200 px
echo   Cutting lines    off
exit /b 0

:after_help

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

rem --- Collect parameters -----------------------------------------------------
if "%~1"=="" goto interactive
goto positional

:interactive
echo ===  Schulfotos — mel-rohrer.ch  ===
echo.
set /p "KLASSENNAME=Klassenname (e.g. GS1d BA): "
if "!KLASSENNAME!"=="" (
    echo ERROR: Klassenname must not be empty. >&2
    exit /b 1
)

rem Generate a random 4-char alphanumeric EVENT_CODE
call :generate_event_code
set "SUGGESTED_CODE=!GENERATED_CODE!"
set /p "EVENT_CODE_INPUT=Event-Code [!SUGGESTED_CODE!]: "
if "!EVENT_CODE_INPUT!"=="" (
    set "EVENT_CODE=!SUGGESTED_CODE!"
) else (
    set "EVENT_CODE=!EVENT_CODE_INPUT!"
)

set /p "CODE_COUNT_INPUT=Anzahl Codes [%DEFAULT_CODE_COUNT%]: "
if "!CODE_COUNT_INPUT!"=="" (
    set "CODE_COUNT=%DEFAULT_CODE_COUNT%"
) else (
    set "CODE_COUNT=!CODE_COUNT_INPUT!"
)

set "EXTRA_ARGS="
echo.
goto run

:positional
set "KLASSENNAME=%~1"
shift
set "CODE_COUNT=%DEFAULT_CODE_COUNT%"
set "NEXT=%~1"
if defined NEXT (
    echo !NEXT!| findstr /r "^[0-9][0-9]*$" >nul 2>&1
    if not errorlevel 1 (
        set "CODE_COUNT=!NEXT!"
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

rem Generate a random EVENT_CODE
call :generate_event_code
set "EVENT_CODE=!GENERATED_CODE!"
goto run

:run
rem --- Derive safe file name prefix from class name ---------------------------
rem Replace spaces and forward slashes with hyphens
set "SAFE_NAME=!KLASSENNAME: =-!"
set "SAFE_NAME=!SAFE_NAME:/=-!"
set "CSV_PATH=!SAFE_NAME!-codes.csv"
set "PDF_PATH=!SAFE_NAME!-qr-codes.pdf"

rem --- Summary ----------------------------------------------------------------
echo ==^> Settings:
echo     Klassenname : !KLASSENNAME!
echo     Event-Code  : !EVENT_CODE!
echo     Code Count  : !CODE_COUNT!
echo     CSV         : !CSV_PATH!
echo     PDF         : !PDF_PATH!
echo.

rem --- Step 1: Generate codes -------------------------------------------------
echo ==^> Generating !CODE_COUNT! codes for class '!KLASSENNAME!' ^(event: !EVENT_CODE!^) ...
if "%USE_JAR%"=="1" (
    java -jar "%RUN%" --app.mode=generate-codes --app.event-code=!EVENT_CODE! --app.code-count=!CODE_COUNT! --app.event-name="!KLASSENNAME!" --app.csv-output-path="!CSV_PATH!" --app.base-url=%BASE_URL% !EXTRA_ARGS!
) else (
    "%RUN%" --app.mode=generate-codes --app.event-code=!EVENT_CODE! --app.code-count=!CODE_COUNT! --app.event-name="!KLASSENNAME!" --app.csv-output-path="!CSV_PATH!" --app.base-url=%BASE_URL% !EXTRA_ARGS!
)
if errorlevel 1 (
    echo ERROR: Code generation failed. >&2
    exit /b 1
)

rem --- Step 2: Generate PDF ---------------------------------------------------
echo ==^> Generating QR-code PDF ...
if "%USE_JAR%"=="1" (
    java -jar "%RUN%" --app.mode=generate-pdf --app.csv-input-path="!CSV_PATH!" --app.output-path="!PDF_PATH!" --app.base-url=%BASE_URL% !EXTRA_ARGS!
) else (
    "%RUN%" --app.mode=generate-pdf --app.csv-input-path="!CSV_PATH!" --app.output-path="!PDF_PATH!" --app.base-url=%BASE_URL% !EXTRA_ARGS!
)
if errorlevel 1 (
    echo ERROR: PDF generation failed. >&2
    exit /b 1
)

echo ==^> Done. Output files:
echo     CSV: !CSV_PATH!
echo     PDF: !PDF_PATH!
goto :eof

rem --- Subroutine: generate_event_code ----------------------------------------
rem Generates a random 4-character alphanumeric (uppercase + digits) code.
rem Result returned in GENERATED_CODE.
:generate_event_code
set "_chars=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
set "GENERATED_CODE="
for /L %%i in (1,1,4) do (
    set /a "_r=!random! %% 36"
    for %%r in (!_r!) do set "GENERATED_CODE=!GENERATED_CODE!!_chars:~%%r,1!"
)
goto :eof
