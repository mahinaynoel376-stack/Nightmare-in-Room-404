@echo off
rem Builds dist\Nightmareon404.jar, a runnable JAR of the game.
rem   1. compiles every .java file in src (for Java 21, so it runs on Java 21 or newer)
rem   2. copies the res folder (sprites and tiles) in next to the compiled classes
rem   3. packs it all into one JAR whose entry point is Nightmareon404.Main
rem Double-click this file, or run it from a terminal in the project folder.
setlocal
cd /d "%~dp0"

if exist build\jar rmdir /s /q build\jar
mkdir build\jar\classes

echo Compiling...
dir /s /b src\*.java > build\jar\sources.txt
javac --release 21 -d build\jar\classes @build\jar\sources.txt || goto :error

echo Copying resources...
xcopy res build\jar\classes /e /i /q /y > nul || goto :error

echo Packing the JAR...
if not exist dist mkdir dist
jar --create --file dist\Nightmareon404.jar --main-class Nightmareon404.Main -C build\jar\classes . || goto :error

echo.
echo Built dist\Nightmareon404.jar
echo Run it by double-clicking it, or with:  java -jar dist\Nightmareon404.jar
goto :end

:error
echo.
echo Build failed - see the messages above.
exit /b 1

:end
endlocal
