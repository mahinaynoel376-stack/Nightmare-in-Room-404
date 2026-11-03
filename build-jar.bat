@echo off
rem Builds dist\NightmareInRoom404.jar, a runnable JAR of the game.
rem   1. compiles the game's code in src\main\java (for Java 21, so it runs on Java 21 or newer)
rem   2. copies src\main\resources (sprites and tiles) in next to the compiled classes
rem   3. packs it all into one JAR whose entry point is nightmare404.main.Main
rem The checks and debug tools in src\test\java are left out: players don't need them.
rem Double-click this file, or run it from a terminal in the project folder.
setlocal
cd /d "%~dp0"

if exist build\jar rmdir /s /q build\jar
mkdir build\jar\classes

echo Compiling...
dir /s /b src\main\java\*.java > build\jar\sources.txt
javac --release 21 -d build\jar\classes @build\jar\sources.txt || goto :error

echo Copying resources...
xcopy src\main\resources build\jar\classes /e /i /q /y > nul || goto :error

echo Packing the JAR...
if not exist dist mkdir dist
jar --create --file dist\NightmareInRoom404.jar --main-class nightmare404.main.Main -C build\jar\classes . || goto :error

echo.
echo Built dist\NightmareInRoom404.jar
echo Run it by double-clicking it, or with:  java -jar dist\NightmareInRoom404.jar
goto :end

:error
echo.
echo Build failed - see the messages above.
exit /b 1

:end
endlocal
