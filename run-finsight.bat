@echo off
setlocal
cd /d "%~dp0"

echo ====================================================================
echo Starting FinSight — Personal Wealth Journal
echo ====================================================================

if exist "dist\FinSight\FinSight.exe" (
    echo Launching packaged desktop application...
    start "" "dist\FinSight\FinSight.exe"
) else if exist "dist\FinanceCopilot\FinanceCopilot.exe" (
    echo Launching packaged desktop application...
    start "" "dist\FinanceCopilot\FinanceCopilot.exe"
) else (
    echo Packaged executable not found. Launching via Maven JavaFX...
    cd desktop-app
    mvn javafx:run
)

endlocal
