@echo off
REM ============================================================
REM  Evolution Tiers Tagger - multi-version build launcher
REM
REM  Opens ONE Windows Terminal window with a separate tab for
REM  each supported Minecraft version (1.21 -> 1.21.11), and
REM  runs the Fabric build for that version in its own tab, all
REM  running side by side so you don't have to open/run these
REM  one at a time yourself.
REM
REM  Requires Windows Terminal (wt.exe) - it ships with Windows
REM  11 and is available for Windows 10 via the Microsoft Store.
REM  If wt isn't found, this falls back to running the builds
REM  one after another in this same window instead.
REM
REM  Output jars land in fabric\build\libs\ after each finishes,
REM  named by version, e.g. evolutiontiers-tagger-1.0.0+mc1.21.11-fabric.jar
REM ============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"

set VERSIONS=1.21 1.21.1 1.21.2 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11

where wt.exe >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Found Windows Terminal - opening one tab per version...

    set WT_CMD=wt.exe
    set FIRST=1

    for %%V in (%VERSIONS%) do (
        if !FIRST! EQU 1 (
            set WT_CMD=!WT_CMD! new-tab --title "MC %%V" cmd /k "gradlew.bat :fabric:build -DmcVersion=%%V && echo. && echo === %%V DONE === || echo. && echo === %%V FAILED, see above ==="
            set FIRST=0
        ) else (
            set WT_CMD=!WT_CMD! ; new-tab --title "MC %%V" cmd /k "gradlew.bat :fabric:build -DmcVersion=%%V && echo. && echo === %%V DONE === || echo. && echo === %%V FAILED, see above ==="
        )
    )

    !WT_CMD!
) else (
    echo Windows Terminal ^(wt.exe^) not found - building versions one at a time instead.
    echo Install Windows Terminal from the Microsoft Store for the parallel-tabs behavior.
    echo.

    for %%V in (%VERSIONS%) do (
        echo ============================================
        echo  Building for Minecraft %%V
        echo ============================================
        call gradlew.bat :fabric:build -DmcVersion=%%V
        if !ERRORLEVEL! NEQ 0 (
            echo === %%V FAILED, see above ===
        ) else (
            echo === %%V DONE ===
        )
        echo.
    )

    pause
)
