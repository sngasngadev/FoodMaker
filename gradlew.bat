@echo off
setlocal
set GRADLE_VERSION=9.6.0
set CACHE_ROOT=%USERPROFILE%\.gradle\foodmaker
set DIST_DIR=%CACHE_ROOT%\gradle-%GRADLE_VERSION%
set ZIP_PATH=%CACHE_ROOT%\gradle-%GRADLE_VERSION%-bin.zip

if not exist "%DIST_DIR%\bin\gradle.bat" (
  if not exist "%CACHE_ROOT%" mkdir "%CACHE_ROOT%"
  if not exist "%ZIP_PATH%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_PATH%'"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%CACHE_ROOT%' -Force"
)

call "%DIST_DIR%\bin\gradle.bat" %*
