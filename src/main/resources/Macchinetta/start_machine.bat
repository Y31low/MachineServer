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
start java -jar AssistanceService-1.0-SNAPSHOT-jar-with-dependencies.jar %INSTITUTE_ID% %MACHINE_ID%
timeout /t 5

:: Avvia gli altri servizi
start java -jar BalanceService-1.0-SNAPSHOT-jar-with-dependencies.jar %INSTITUTE_ID% %MACHINE_ID%
start java -jar DispenserService-1.0-SNAPSHOT-jar-with-dependencies.jar %INSTITUTE_ID% %MACHINE_ID%
start java -jar FrontendService-1.0-SNAPSHOT-jar-with-dependencies.jar %INSTITUTE_ID% %MACHINE_ID%
start java -jar TrasnactionService-1.0-SNAPSHOT-jar-with-dependencies.jar %INSTITUTE_ID% %MACHINE_ID%