@echo off
rem generate-qrcodes.bat — Generate gallery codes and produce a QR-code PDF in one go.
rem
rem Usage:
rem   generate-qrcodes.bat <EVENT_CODE> [CODE_COUNT] [EXTRA_ARGS...]
rem
rem Examples:
rem   generate-qrcodes.bat XY9G
rem   generate-qrcodes.bat XY9G 100
rem   generate-qrcodes.bat XY9G 100 --app.base-url=https://my.site/gallery/

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_NAME=foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
set "NATIVE_NAME=foto-gallery-qrcode-generator.exe"

rem --- Resolve executable -----------------------------------------------------
rem Check current directory first, then target\ subdirectory
if exist "%SCRIPT_DIR%%NATIVE_NAME%" (
    set "RUN=%SCRIPT_DIR%%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%target\%NATIVE_NAME%" (
    set "RUN=%SCRIPT_DIR%target\%NATIVE_NAME%"
) else if exist "%SCRIPT_DIR%%JAR_NAME%" (
    set "RUN=java -jar %SCRIPT_DIR%%JAR_NAME%"
) else if exist "%SCRIPT_DIR%target\%JAR_NAME%" (
    set "RUN=java -jar %SCRIPT_DIR%target\%JAR_NAME%"
) else (
    echo ERROR: No executable found. Build the project first: >&2
    echo   mvn clean package -DskipTests          ^(JAR^) >&2
    echo   mvn clean package -Pnative -DskipTests  ^(native^) >&2
    exit /b 1
)

rem --- Parse arguments --------------------------------------------------------
if "%~1"=="" (
    echo Usage: %~nx0 ^<EVENT_CODE^> [CODE_COUNT] [EXTRA_ARGS...] >&2
    exit /b 1
)

set "EVENT_CODE=%~1"
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

rem --- Step 1: Generate codes -------------------------------------------------
echo ==^> Generating %CODE_COUNT% codes for event %EVENT_CODE% ...
%RUN% --app.mode=generate-codes --app.event-code=%EVENT_CODE% --app.code-count=%CODE_COUNT% %EXTRA_ARGS%
if errorlevel 1 (
    echo ERROR: Code generation failed. >&2
    exit /b 1
)

rem --- Step 2: Generate PDF ---------------------------------------------------
echo ==^> Generating QR-code PDF ...
%RUN% --app.mode=generate-pdf %EXTRA_ARGS%
if errorlevel 1 (
    echo ERROR: PDF generation failed. >&2
    exit /b 1
)

echo ==^> Done.
endlocal
