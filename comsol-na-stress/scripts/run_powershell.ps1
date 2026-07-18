[CmdletBinding()]
param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MainArgs)
$ErrorActionPreference = 'Stop'
$project = if ($env:PROJECT_HOME) { $env:PROJECT_HOME } else { (Resolve-Path (Join-Path $PSScriptRoot '..')).Path }
$comsol = if ($env:COMSOL_HOME) { $env:COMSOL_HOME } else { 'C:\Program Files\COMSOL\COMSOL64\Multiphysics' }
if ($env:JAVA_HOME -and -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { throw 'JAVA_HOME is set but java.exe was not found.' }
$batch = @((Join-Path $comsol 'bin\win64\comsolbatch.exe'), (Join-Path $comsol 'bin\win64\comsolbatch.bat')) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $batch) { throw 'COMSOL executable not found. Please set COMSOL_HOME.' }
$mainClass = Join-Path $project 'target\classes\com\nfm\comsol\Main.class'
if (-not (Test-Path $mainClass)) { throw 'Classes not found. Run scripts\compile_powershell.ps1 first.' }
Push-Location $project
try {
  $env:NFM_COMSOL_ARGS = $MainArgs -join ' '
  $env:CLASSPATH = (Join-Path $project 'target\classes') + ';' + $env:CLASSPATH
  & $batch -inputfile 'target\classes\com\nfm\comsol\Main.class'
  exit $LASTEXITCODE
}
finally { Pop-Location }
