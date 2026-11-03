@echo off
rem Builds dist\NightmareInRoom404-windows.zip: the game as a Windows app that brings its own Java.
rem Whoever you send it to just unzips it and double-clicks NightmareInRoom404.exe; they don't need Java.
rem   1. builds the JAR (build-jar.bat)
rem   2. jpackage wraps it with a small Java runtime (only the parts the game uses) and an .exe launcher
rem   3. zips the result, ready to upload to Google Drive or a GitHub Release
rem Double-click this file, or run it from a terminal in the project folder.
setlocal
cd /d "%~dp0"

call "%~dp0build-jar.bat" || goto :error

echo.
echo Packaging the app with its own Java (this takes a minute)...
if exist build\package rmdir /s /q build\package
mkdir build\package\input
copy dist\NightmareInRoom404.jar build\package\input > nul || goto :error
jpackage --type app-image --name NightmareInRoom404 --app-version 1.0 ^
    --input build\package\input --main-jar NightmareInRoom404.jar --main-class nightmare404.main.Main ^
    --add-modules java.desktop --dest build\package\out || goto :error

echo Zipping...
if exist dist\NightmareInRoom404-windows.zip del dist\NightmareInRoom404-windows.zip
tar -a -c -f dist\NightmareInRoom404-windows.zip -C build\package\out NightmareInRoom404 || goto :error

echo.
echo Built dist\NightmareInRoom404-windows.zip
echo Send that one file. To play: unzip it, open the NightmareInRoom404 folder, double-click NightmareInRoom404.exe
goto :end

:error
echo.
echo Build failed - see the messages above.
exit /b 1

:end
endlocal
