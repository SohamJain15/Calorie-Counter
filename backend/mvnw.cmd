@echo off
rem Minimal Maven wrapper for Windows: downloads Apache Maven once into
rem %USERPROFILE%\.m2\wrapper and then runs it with whatever arguments you pass.
setlocal
set "MVN_VERSION=3.9.9"
set "MVN_HOME=%USERPROFILE%\.m2\wrapper\apache-maven-%MVN_VERSION%"

if not exist "%MVN_HOME%\bin\mvn.cmd" (
  echo Downloading Apache Maven %MVN_VERSION% - first run only...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $d=Join-Path $env:USERPROFILE '.m2\wrapper'; New-Item -ItemType Directory -Force -Path $d | Out-Null; $z=Join-Path $d 'maven.zip'; Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip' -OutFile $z; Expand-Archive -Force -Path $z -DestinationPath $d; Remove-Item $z"
  if errorlevel 1 (
    echo Failed to download Maven. Check your internet connection.
    exit /b 1
  )
)

call "%MVN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
