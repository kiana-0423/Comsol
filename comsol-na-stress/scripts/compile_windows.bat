@echo off
setlocal EnableDelayedExpansion
if "%PROJECT_HOME%"=="" set "PROJECT_HOME=%~dp0.."
if "%COMSOL_HOME%"=="" set "COMSOL_HOME=C:\Program Files\COMSOL\COMSOL64\Multiphysics"
if not "%JAVA_HOME%"=="" if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JAVA_HOME is set but java.exe was not found.
  exit /b 2
)
set "COMSOLCOMPILE=%COMSOL_HOME%\bin\win64\comsolcompile.exe"
if not exist "%COMSOLCOMPILE%" set "COMSOLCOMPILE=%COMSOL_HOME%\bin\win64\comsolcompile.bat"
if not exist "%COMSOLCOMPILE%" (
  echo ERROR: COMSOL executable not found. Please set COMSOL_HOME.
  exit /b 2
)
if not exist "%PROJECT_HOME%\target\classes" mkdir "%PROJECT_HOME%\target\classes"
pushd "%PROJECT_HOME%"
set "SOURCES="
for /r src\main\java %%F in (*.java) do set SOURCES=!SOURCES! "%%F"
"%COMSOLCOMPILE%" -d target\classes !SOURCES!
set "RC=%ERRORLEVEL%"
popd
if not "%RC%"=="0" echo ERROR: COMSOL Java compilation failed with exit code %RC%.
exit /b %RC%
