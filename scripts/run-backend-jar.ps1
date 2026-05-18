$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$deployDir = Join-Path $repoRoot "deploy"
$jarPath = Join-Path $deployDir "backend.jar"

function Import-EnvFile {
  param([string] $Path)
  if (-not (Test-Path -LiteralPath $Path)) { return }
  Get-Content -LiteralPath $Path | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#") -or -not $line.Contains("=")) { return }
    $key, $value = $line.Split("=", 2)
    $key = $key.Trim()
    $value = $value.Trim().Trim('"').Trim("'")
    if ($key -match '^[A-Za-z_][A-Za-z0-9_]*$') {
      [Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
  }
}

function Test-JavaHome {
  param([string] $Path)
  if ([string]::IsNullOrWhiteSpace($Path)) { return $false }
  return Test-Path -LiteralPath (Join-Path $Path "bin\java.exe")
}

Import-EnvFile (Join-Path $repoRoot ".env.local")
Import-EnvFile (Join-Path $deployDir ".env")

$javaHomeCandidates = @(
  $env:DEEPINSIGHT_JAVA_HOME,
  "D:\jdk\java21",
  $env:JAVA_HOME
) | Where-Object { Test-JavaHome $_ } | Select-Object -Unique

if ($javaHomeCandidates.Count -eq 0) {
  throw "No usable Java home found. Install JDK 21 or set DEEPINSIGHT_JAVA_HOME."
}

$env:JAVA_HOME = $javaHomeCandidates[0]
$javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"

Set-Location -LiteralPath $deployDir
& $javaExe "-jar" $jarPath
