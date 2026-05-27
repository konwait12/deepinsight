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

function Resolve-JavaExe {
  $home = @($env:DEEPINSIGHT_JAVA_HOME, $env:JAVA_HOME) |
    Where-Object { Test-JavaHome $_ } |
    Select-Object -First 1

  if ($home) {
    $env:JAVA_HOME = $home
    return Join-Path $home "bin\java.exe"
  }

  $java = Get-Command java -CommandType Application -ErrorAction SilentlyContinue
  if ($java) { return $java.Source }

  throw "No usable Java runtime found. Install Java 21, add java.exe to PATH, or set DEEPINSIGHT_JAVA_HOME."
}

Import-EnvFile (Join-Path $repoRoot ".env.local")
Import-EnvFile (Join-Path $deployDir ".env")

$javaExe = Resolve-JavaExe

Set-Location -LiteralPath $deployDir
& $javaExe "-jar" $jarPath
