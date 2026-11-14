@echo off
rem Builds the dojo and runs a copy of the binary, so a Dojo left open in
rem another window never blocks the next build. Usage: dojo [arguments]
setlocal
cd /d "%~dp0"

set "TARGET=target"
if defined CARGO_TARGET_DIR set "TARGET=%CARGO_TARGET_DIR%"

cargo build --quiet --bin dojo || exit /b %errorlevel%

if not exist "%TARGET%\run" mkdir "%TARGET%\run"
rem Old copies still in use stay; the rest are removed.
del /q "%TARGET%\run\dojo-*.exe" 2>nul

set "DOJO_COPY=%TARGET%\run\dojo-%RANDOM%%RANDOM%.exe"
copy /y "%TARGET%\debug\dojo.exe" "%DOJO_COPY%" >nul || exit /b 1
"%DOJO_COPY%" %*
exit /b %errorlevel%
