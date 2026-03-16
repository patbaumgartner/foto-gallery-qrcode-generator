@echo off
rem generate-qrcodes.bat — Generate gallery codes and produce a QR-code PDF in one go.
rem
rem Usage:
rem   generate-qrcodes.bat                                      :: interactive shell mode
rem   generate-qrcodes.bat <EVENT_CODE> [CODE_COUNT] [EVENT_NAME] [EXTRA_ARGS...]
rem   generate-qrcodes.bat --app.mode=... [EXTRA_ARGS...]       :: pass flags directly
rem
rem Examples:
rem   generate-qrcodes.bat
rem   generate-qrcodes.bat XY9G
rem   generate-qrcodes.bat XY9G 100
rem   generate-qrcodes.bat XY9G 100 "My Photo Event"
rem   generate-qrcodes.bat XY9G 100 "My Photo Event" --app.gallery-url=https://my.site/gallery?code=
rem   generate-qrcodes.bat --app.mode=generate-codes --app.event-code=XY9G

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_NAME=foto-gallery-qrcode-generator-0.0.1-SNAPSHOT.jar"
set "NATIVE_NAME=foto-gallery-qrcode-generator.exe"

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
    echo ERROR: No executable found ^('%NATIVE_NAME%'^). >&2
    exit /b 1
)

rem --- Interactive mode (no arguments) ----------------------------------------
if "%~1"=="" (
    echo ==^> No arguments provided. Launching interactive shell...
    if "%USE_JAR%"=="1" (
        java -jar "%RUN%"
    ) else (
        "%RUN%"
    )
    exit /b %ERRORLEVEL%
)

rem --- Direct flag passthrough (first arg starts with '--') --------------------
set "FIRST_ARG=%~1"
if "%FIRST_ARG:~0,2%"=="--" (
    if "%USE_JAR%"=="1" (
        java -jar "%RUN%" %*
    ) else (
        "%RUN%" %*
    )
    exit /b %ERRORLEVEL%
)

rem --- Parse arguments --------------------------------------------------------
set "EVENT_CODE=%~1"
shift

set "CODE_COUNT=17"
set "NEXT=%~1"
if defined NEXT (
    echo %NEXT%| findstr /r "^[0-9][0-9]*$" >nul 2>&1
    if not errorlevel 1 (
        set "CODE_COUNT=%NEXT%"
        shift
    )
)

set "EVENT_NAME="
set "NEXT2=%~1"
if defined NEXT2 (
    echo %NEXT2%| findstr /r "^--" >nul 2>&1
    if errorlevel 1 (
        set "EVENT_NAME=%NEXT2%"
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
if "%USE_JAR%"=="1" (
    java -jar "%RUN%" --app.mode=generate-codes --app.event-code=%EVENT_CODE% --app.code-count=%CODE_COUNT% --app.event-name="%EVENT_NAME%" %EXTRA_ARGS%
) else (
    "%RUN%" --app.mode=generate-codes --app.event-code=%EVENT_CODE% --app.code-count=%CODE_COUNT% --app.event-name="%EVENT_NAME%" %EXTRA_ARGS%
)
if errorlevel 1 (
    echo ERROR: Code generation failed. >&2
    exit /b 1
)

rem --- Step 2: Generate PDF ---------------------------------------------------
echo ==^> Generating QR-code PDF ...
if "%USE_JAR%"=="1" (
    java -jar "%RUN%" --app.mode=generate-pdf %EXTRA_ARGS%
) else (
    "%RUN%" --app.mode=generate-pdf %EXTRA_ARGS%
)
if errorlevel 1 (
    echo ERROR: PDF generation failed. >&2
    exit /b 1
)

echo ==^> Done.
endlocal
