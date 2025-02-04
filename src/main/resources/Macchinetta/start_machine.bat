@echo off
setlocal

set INSTITUTE_ID=%1
set MACHINE_ID=%2

if "%1"=="" (
   echo Inserire ID istituto come primo parametro
   exit /b 1
)
if "%2"=="" (
   echo Inserire ID macchinetta come secondo parametro
   exit /b 1
)

:: Avvia AssistanceService e aspetta 5 secondi
start cmd /k java -jar "%~dp0AssistanceService-1.0-SNAPSHOT-jar-with-dependencies.jar" %INSTITUTE_ID% %MACHINE_ID%
timeout /t 5

:: Avvia gli altri servizi
start cmd /k java -jar "%~dp0BalanceService-1.0-SNAPSHOT-jar-with-dependencies.jar" %INSTITUTE_ID% %MACHINE_ID%
start cmd /k java -jar "%~dp0DispenserService-1.0-SNAPSHOT-jar-with-dependencies.jar" %INSTITUTE_ID% %MACHINE_ID%
start cmd /k java -jar "%~dp0FrontendService-1.0-SNAPSHOT-jar-with-dependencies.jar" %INSTITUTE_ID% %MACHINE_ID%
start cmd /k java -jar "%~dp0TrasnactionService-1.0-SNAPSHOT-jar-with-dependencies.jar" %INSTITUTE_ID% %MACHINE_ID%