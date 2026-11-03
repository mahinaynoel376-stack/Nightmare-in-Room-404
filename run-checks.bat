@echo off
rem Compiles the game and its checks, then runs every check in src\test\java\nightmare404\checks.
rem Each check plays part of the game without opening a window (walking, doors, stairs, the camera...)
rem and prints "ok" or "FAIL" for every test. Run this after changing the code or a map.
rem The compiled classes stay in build\test\classes, so the debug tools can be run afterwards, e.g.
rem   java -Djava.awt.headless=true -cp build\test\classes nightmare404.debug.MapPreviewDebug map_previews
setlocal
cd /d "%~dp0"

if exist build\test rmdir /s /q build\test
mkdir build\test\classes

echo Compiling the game and the checks...
dir /s /b src\main\java\*.java src\test\java\*.java | findstr /v /i "module-info.java" > build\test\sources.txt
javac --release 21 -d build\test\classes @build\test\sources.txt || goto :error
xcopy src\main\resources build\test\classes /e /i /q /y > nul || goto :error

set passed=0
set failed=0
for %%f in (src\test\java\nightmare404\checks\*.java) do (
    echo.
    echo ===== %%~nf
    java -Djava.awt.headless=true -cp build\test\classes nightmare404.checks.%%~nf
    if errorlevel 1 (set /a failed+=1) else (set /a passed+=1)
)

echo.
echo %passed% checks passed, %failed% failed.
if %failed% gtr 0 exit /b 1
goto :end

:error
echo.
echo Compiling failed - see the messages above.
exit /b 1

:end
endlocal
