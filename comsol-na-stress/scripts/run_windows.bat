@echo off
setlocal
if "%PROJECT_HOME%"=="" set "PROJECT_HOME=%~dp0.."
if "%COMSOL_HOME%"=="" set "COMSOL_HOME=C:\Program Files\COMSOL\COMSOL64\Multiphysics"
if not "%JAVA_HOME%"=="" if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JAVA_HOME is set but java.exe was not found.
  exit /b 2
)
set "COMSOLBATCH=%COMSOL_HOME%\bin\win64\comsolbatch.exe"
if not exist "%COMSOLBATCH%" set "COMSOLBATCH=%COMSOL_HOME%\bin\win64\comsolbatch.bat"
if not exist "%COMSOLBATCH%" (
  echo ERROR: COMSOL executable not found. Please set COMSOL_HOME.
  exit /b 2
)
if not exist "%PROJECT_HOME%\target\classes\com\nfm\comsol\Main.class" (
  echo ERROR: Classes not found. Run scripts\compile_windows.bat first.
  exit /b 3
)
pushd "%PROJECT_HOME%"
set "NFM_COMSOL_ARGS=%*"
set "CLASSPATH=%PROJECT_HOME%\target\classes;%CLASSPATH%"
"%COMSOLBATCH%" -inputfile target\classes\com\nfm\comsol\Main.class
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%
