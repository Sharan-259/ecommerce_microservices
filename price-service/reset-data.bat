@echo off
REM Stop the service first. Deletes the file-based H2 database; it is re-created and re-seeded on next start.
cd /d "%~dp0"
if exist data rmdir /s /q data
echo price-service data folder deleted.
