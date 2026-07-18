[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$project = if ($env:PROJECT_HOME) { $env:PROJECT_HOME } else { (Resolve-Path (Join-Path $PSScriptRoot '..')).Path }
$comsol = if ($env:COMSOL_HOME) { $env:COMSOL_HOME } else { 'C:\Program Files\COMSOL\COMSOL64\Multiphysics' }
if ($env:JAVA_HOME -and -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { throw 'JAVA_HOME is set but java.exe was not found.' }
$compiler = @((Join-Path $comsol 'bin\win64\comsolcompile.exe'), (Join-Path $comsol 'bin\win64\comsolcompile.bat')) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $compiler) { throw 'COMSOL executable not found. Please set COMSOL_HOME.' }
$classes = Join-Path $project 'target\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = Get-ChildItem (Join-Path $project 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object FullName
Push-Location $project
try { & $compiler -d $classes @sources; if ($LASTEXITCODE -ne 0) { throw "COMSOL Java compilation failed: $LASTEXITCODE" } }
finally { Pop-Location }
