@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-gradle.ps1" %*
exit /b %ERRORLEVEL%
