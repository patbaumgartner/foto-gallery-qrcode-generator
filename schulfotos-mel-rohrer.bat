@echo off
rem schulfotos-mel-rohrer.bat — Generate school photo gallery codes for mel-rohrer.ch/schulfotos.
rem
rem The gallery URL used in QR codes is hardcoded to https://mel-rohrer.ch/schulfotos/?code=
rem and the base URL printed on the back of the PDF is https://mel-rohrer.ch/schulfotos.
rem All standard settings are used (3x4 grid, 200 px QR size, cutting lines enabled).
rem A back page with the gallery password and the logo (logo.png) is always included.
rem
rem When run without arguments the script prompts interactively for every parameter.
rem A random 4-character alphanumeric EVENT_CODE is generated automatically
rem and can be accepted or overridden in interactive mode.
rem
rem Usage:
rem   schulfotos-mel-rohrer.bat                                   :: interactive mode
rem   schulfotos-mel-rohrer.bat <KLASSENNAME> [SHOOTING_DATE] [CODE_COUNT] [EXTRA_ARGS...]
rem   schulfotos-mel-rohrer.bat --help
rem
rem Options:
rem   -v, --verbose   Show Spring Boot log output (hidden by default)
rem
rem Arguments:
rem   KLASSENNAME    Class name used as the event label in the PDF (e.g. "GS1d BA")
rem   SHOOTING_DATE  Shooting date in German format DD.MM.YYYY (default: today)
rem   CODE_COUNT     Number of codes to generate (default: 17)
rem   EXTRA_ARGS     Any additional --app.* flags passed to both steps
rem
rem Examples:
rem   schulfotos-mel-rohrer.bat
rem   schulfotos-mel-rohrer.bat "GS1d BA"
rem   schulfotos-mel-rohrer.bat "GS1d BA" 25.03.2026
rem   schulfotos-mel-rohrer.bat "GS1d BA" 25.03.2026 30

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_NAME=foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
set "NATIVE_NAME=foto-gallery-qrcode-generator.exe"
set "BASE_URL=https://mel-rohrer.ch/schulfotos"
set "GALLERY_URL=https://mel-rohrer.ch/schulfotos/?code="
set "DEFAULT_CODE_COUNT=17"

rem --- Help -------------------------------------------------------------------
if "%~1"=="--help" goto :show_help
if "%~1"=="-h"     goto :show_help

rem --- Detect -v / --verbose --------------------------------------------------
set "QUIET_ARGS=--logging.level.root=WARN --spring.main.banner-mode=off"
for %%a in (%*) do (
    if /i "%%~a"=="-v"        set "QUIET_ARGS="
    if /i "%%~a"=="--verbose" set "QUIET_ARGS="
)
if /i "%~1"=="-v"        shift
if /i "%~1"=="--verbose" shift

rem --- PicPeak credentials ----------------------------------------------------
rem Copy picpeak.properties.example to picpeak.properties and fill in your
rem credentials.  The file is .gitignored so it will never be committed.
set "PICPEAK_ARGS="
if exist "%SCRIPT_DIR%picpeak.properties" (
    set "PICPEAK_ARGS=--spring.config.additional-location=file:%SCRIPT_DIR%picpeak.properties"
)

rem --- Resolve executable -----------------------------------------------------
rem Sets APP (path only) and RUN_PREFIX (empty for native, "java -jar" for JAR).
set "RUN_PREFIX="
if exist "%SCRIPT_DIR%%NATIVE_NAME%" (
    set "APP=%SCRIPT_DIR%%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%target\%NATIVE_NAME%" (
    set "APP=%SCRIPT_DIR%target\%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%%JAR_NAME%" (
    set "APP=%SCRIPT_DIR%%JAR_NAME%"
    set "RUN_PREFIX=java -jar"
) else if exist "%SCRIPT_DIR%target\%JAR_NAME%" (
    set "APP=%SCRIPT_DIR%target\%JAR_NAME%"
    set "RUN_PREFIX=java -jar"
) else (
    echo ERROR: No executable found ^(neither '%NATIVE_NAME%' nor '%JAR_NAME%'^). >&2
    exit /b 1
)

rem --- Collect parameters -----------------------------------------------------
if "%~1"=="" goto :interactive

rem Non-interactive: positional arguments
set "KLASSENNAME=%~1"
shift

rem Optional date argument (DD.MM.YYYY)
set "SHOOTING_DATE_DE="
set "_next=%~1"
if defined _next (
    echo !_next!| findstr /r "^[0-9][0-9]\.[0-9][0-9]\.[0-9][0-9][0-9][0-9]$" >nul 2>&1
    if not errorlevel 1 ( set "SHOOTING_DATE_DE=!_next!" & shift )
)
if not defined SHOOTING_DATE_DE (
    for /f "usebackq" %%d in (`powershell -NoProfile -Command "(Get-Date).ToString('dd.MM.yyyy')"`) do set "SHOOTING_DATE_DE=%%d"
)

rem Optional code count argument
set "CODE_COUNT=%DEFAULT_CODE_COUNT%"
set "_next=%~1"
if defined _next (
    echo !_next!| findstr /r "^[0-9][0-9]*$" >nul 2>&1
    if not errorlevel 1 ( set "CODE_COUNT=!_next!" & shift )
)

rem Collect remaining extra arguments (skip -v / --verbose)
set "EXTRA_ARGS="
:parse_extra
if "%~1"=="" goto :done_extra
if /i "%~1"=="-v"        ( shift & goto :parse_extra )
if /i "%~1"=="--verbose" ( shift & goto :parse_extra )
set "EXTRA_ARGS=!EXTRA_ARGS! %~1"
shift
goto :parse_extra
:done_extra

call :generate_event_code
set "EVENT_CODE=!GENERATED_CODE!"
set "PICPEAK_ENABLED=false"
goto :convert_date

:interactive
echo ===  Schulfotos — mel-rohrer.ch  ===
echo.
set /p "KLASSENNAME=Klassenname (e.g. GS1d BA): "
if "!KLASSENNAME!"=="" (
    echo ERROR: Klassenname must not be empty. >&2
    exit /b 1
)

call :generate_event_code
set "SUGGESTED_CODE=!GENERATED_CODE!"
set /p "EVENT_CODE_INPUT=Event-Code [!SUGGESTED_CODE!]: "
set "EVENT_CODE=!SUGGESTED_CODE!"
if not "!EVENT_CODE_INPUT!"=="" set "EVENT_CODE=!EVENT_CODE_INPUT!"

for /f "usebackq" %%d in (`powershell -NoProfile -Command "(Get-Date).ToString('dd.MM.yyyy')"`) do set "TODAY=%%d"
set /p "SHOOTING_DATE_INPUT=Shooting-Datum (DD.MM.YYYY) [!TODAY!]: "
set "SHOOTING_DATE_DE=!TODAY!"
if not "!SHOOTING_DATE_INPUT!"=="" set "SHOOTING_DATE_DE=!SHOOTING_DATE_INPUT!"

set /p "CODE_COUNT_INPUT=Anzahl Codes [%DEFAULT_CODE_COUNT%]: "
set "CODE_COUNT=%DEFAULT_CODE_COUNT%"
if not "!CODE_COUNT_INPUT!"=="" set "CODE_COUNT=!CODE_COUNT_INPUT!"

set /p "PICPEAK_INPUT=Galerie-Events in PicPeak erstellen? [y/N]: "
set "PICPEAK_ENABLED=false"
if /i "!PICPEAK_INPUT!"=="y"   set "PICPEAK_ENABLED=true"
if /i "!PICPEAK_INPUT!"=="yes" set "PICPEAK_ENABLED=true"

set "EXTRA_ARGS="
echo.

rem --- Convert DD.MM.YYYY to YYYY-MM-DD ---------------------------------------
:convert_date
set "SHOOTING_DATE=!SHOOTING_DATE_DE:~6,4!-!SHOOTING_DATE_DE:~3,2!-!SHOOTING_DATE_DE:~0,2!"

rem --- Derive output paths ----------------------------------------------------
set "SAFE_NAME=!KLASSENNAME: =-!"
set "SAFE_NAME=!SAFE_NAME:/=-!"
set "OUTPUT_DIR=schulfotos"
if not exist "!OUTPUT_DIR!" mkdir "!OUTPUT_DIR!"
set "CSV_PATH=!OUTPUT_DIR!\!SAFE_NAME!-codes.csv"
set "PDF_PATH=!OUTPUT_DIR!\!SAFE_NAME!-qr-codes.pdf"

rem --- Summary ----------------------------------------------------------------
echo ==^> Settings:
echo     Klassenname    : !KLASSENNAME!
echo     Event-Code     : !EVENT_CODE!
echo     Shooting-Datum : !SHOOTING_DATE_DE! ^(!SHOOTING_DATE!^)
echo     Code Count     : !CODE_COUNT!
echo     PicPeak        : !PICPEAK_ENABLED!
echo     CSV            : !CSV_PATH!
echo     PDF            : !PDF_PATH!
echo.

rem --- Step 1: Generate codes -------------------------------------------------
echo ==^> Generating !CODE_COUNT! codes for class '!KLASSENNAME!' ^(event: !EVENT_CODE!^) ...
%RUN_PREFIX% "%APP%" --app.mode=generate-codes --app.event-code=!EVENT_CODE! --app.code-count=!CODE_COUNT! --app.event-name="!KLASSENNAME!" --app.csv-output-path="!CSV_PATH!" --app.gallery-url=%GALLERY_URL% --app.picpeak.enabled=!PICPEAK_ENABLED! --app.picpeak.event-date=!SHOOTING_DATE! !PICPEAK_ARGS! !QUIET_ARGS! !EXTRA_ARGS!
if errorlevel 1 (
    echo ERROR: Code generation failed. >&2
    exit /b 1
)

rem --- Step 2: Generate PDF ---------------------------------------------------
echo ==^> Generating QR-code PDF ...
%RUN_PREFIX% "%APP%" --app.mode=generate-pdf --app.csv-input-path="!CSV_PATH!" --app.output-path="!PDF_PATH!" --app.base-url=%BASE_URL% --app.gallery-url=%GALLERY_URL% --app.show-cutting-lines=true --app.logo-url=logo.png !PICPEAK_ARGS! !QUIET_ARGS! !EXTRA_ARGS!
if errorlevel 1 (
    echo ERROR: PDF generation failed. >&2
    exit /b 1
)

echo ==^> Done. Output files:
echo     CSV: !CSV_PATH!
echo     PDF: !PDF_PATH!
goto :eof

rem --- Subroutines ------------------------------------------------------------
:show_help
echo schulfotos-mel-rohrer.bat — Generate school photo gallery codes ^& QR-code PDFs.
echo.
echo Usage:
echo   %~nx0                                   Interactive mode (prompts for all settings)
echo   %~nx0 ^<KLASSENNAME^> [SHOOTING_DATE] [CODE_COUNT] [EXTRA_ARGS...]
echo   %~nx0 --help                            Show this help message
echo.
echo Options:
echo   -v, --verbose   Show Spring Boot log output (hidden by default)
echo.
echo Arguments:
echo   KLASSENNAME    Class name used as the event label in the PDF (e.g. "GS1d BA").
echo                  A random 4-character alphanumeric EVENT_CODE is generated
echo                  automatically. You will be asked to confirm or override it in
echo                  interactive mode.
echo   SHOOTING_DATE  Shooting date in German format DD.MM.YYYY (default: today)
echo   CODE_COUNT     Number of codes to generate (default: %DEFAULT_CODE_COUNT%)
echo   EXTRA_ARGS     Any additional --app.* flags passed to both steps
echo.
echo Examples:
echo   %~nx0
echo   %~nx0 "GS1d BA"
echo   %~nx0 "GS1d BA" 25.03.2026
echo   %~nx0 "GS1d BA" 25.03.2026 30
echo.
echo Defaults:
echo   Base URL (back of PDF)  %BASE_URL%
echo   Gallery URL (QR codes)  %GALLERY_URL%
echo   Grid                    3 columns x 4 rows
echo   QR size                 200 px
echo   Cutting lines           on
exit /b 0

:generate_event_code
set "_chars=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
set "GENERATED_CODE="
for /L %%i in (1,1,4) do (
    set /a "_r=!random! %% 36"
    for %%r in (!_r!) do set "GENERATED_CODE=!GENERATED_CODE!!_chars:~%%r,1!"
)
goto :eof
