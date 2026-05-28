$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendDir = Join-Path $repoRoot "backend"
$mvnw = Join-Path $backendDir "mvnw.cmd"
$logFile = Join-Path $repoRoot "backend.run.log"

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
  $javaHome = @($env:DEEPINSIGHT_JAVA_HOME, $env:JAVA_HOME) |
    Where-Object { Test-JavaHome $_ } |
    Select-Object -First 1

  if ($javaHome) {
    $env:JAVA_HOME = $javaHome
    return Join-Path $javaHome "bin\java.exe"
  }

  $java = Get-Command java -CommandType Application -ErrorAction SilentlyContinue
  if ($java) { return $java.Source }

  throw "No usable Java runtime found. Install JDK 21, add java.exe to PATH, or set DEEPINSIGHT_JAVA_HOME."
}

Import-EnvFile (Join-Path $repoRoot ".env.local")
Import-EnvFile (Join-Path $backendDir ".env.local")

$javaExe = Resolve-JavaExe
$env:PATH = (Split-Path -Parent $javaExe) + ";" + $env:PATH

Set-Location -LiteralPath $backendDir
& $mvnw "spring-boot:run" *> $logFile
