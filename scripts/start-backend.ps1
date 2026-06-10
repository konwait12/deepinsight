$ErrorActionPreference = "Stop"

function Write-RunLog {
  param(
    [string] $Path,
    [string] $Message
  )

  Add-Content -LiteralPath $Path -Encoding UTF8 -Value $Message
}

function Import-EnvFile {
  param([string] $Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    return
  }

  foreach ($rawLine in Get-Content -LiteralPath $Path -Encoding UTF8) {
    $line = $rawLine.Trim().Trim([char] 0xFEFF)
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
      continue
    }

    $separator = $line.IndexOf("=")
    if ($separator -le 0) {
      continue
    }

    $key = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()
    if ($key -notmatch "^[A-Za-z_][A-Za-z0-9_]*$") {
      continue
    }

    if ($value.Length -ge 2) {
      $first = $value.Substring(0, 1)
      $last = $value.Substring($value.Length - 1, 1)
      if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }

    [Environment]::SetEnvironmentVariable($key, $value, "Process")
  }
}

function ConvertTo-BatchSetLine {
  param(
    [string] $Key,
    [string] $Value
  )

  $escaped = $Value.
    Replace("^", "^^").
    Replace("&", "^&").
    Replace("|", "^|").
    Replace("<", "^<").
    Replace(">", "^>").
    Replace("%", "%%")

  return "set ""$Key=$escaped"""
}

function Export-EnvFileForBatch {
  param([string] $Path)

  $lines = New-Object 'System.Collections.Generic.List[string]'
  if (-not (Test-Path -LiteralPath $Path)) {
    return $lines
  }

  foreach ($rawLine in Get-Content -LiteralPath $Path -Encoding UTF8) {
    $line = $rawLine.Trim().Trim([char] 0xFEFF)
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
      continue
    }

    $separator = $line.IndexOf("=")
    if ($separator -le 0) {
      continue
    }

    $key = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()
    if ($key -notmatch "^[A-Za-z_][A-Za-z0-9_]*$") {
      continue
    }

    if ($value.Length -ge 2) {
      $first = $value.Substring(0, 1)
      $last = $value.Substring($value.Length - 1, 1)
      if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }

    $lines.Add((ConvertTo-BatchSetLine -Key $key -Value $value))
  }

  return $lines.ToArray()
}

function Test-JavaHome {
  param([string] $Path)

  if ([string]::IsNullOrWhiteSpace($Path)) {
    return $false
  }

  return Test-Path -LiteralPath (Join-Path $Path "bin\java.exe")
}

function Resolve-JavaRuntime {
  $candidates = @(
    $env:DEEPINSIGHT_JAVA_HOME,
    "D:\jdk\java21",
    $env:JAVA_HOME
  ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

  foreach ($candidate in $candidates) {
    if (Test-JavaHome $candidate) {
      $javaHome = (Resolve-Path -LiteralPath $candidate).Path
      return [pscustomobject]@{
        JavaHome = $javaHome
        JavaExe = Join-Path $javaHome "bin\java.exe"
      }
    }
  }

  $java = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue
  if ($java) {
    $javaHome = Split-Path -Parent (Split-Path -Parent $java.Source)
    return [pscustomobject]@{
      JavaHome = $javaHome
      JavaExe = $java.Source
    }
  }

  throw "No usable Java runtime found. Set DEEPINSIGHT_JAVA_HOME/JAVA_HOME or install JDK 21 at D:\jdk\java21."
}

function Reset-LogFile {
  param([string] $Path)

  if (Test-Path -LiteralPath $Path) {
    Remove-Item -LiteralPath $Path -Force
  }
  New-Item -ItemType File -Path $Path -Force | Out-Null
}

function Get-ListeningProcessIds {
  param([int] $Port)

  $ids = @()
  try {
    $ids = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
      Select-Object -ExpandProperty OwningProcess -Unique
  } catch {
    $lines = & cmd.exe /d /s /c "netstat -ano | findstr :$Port" 2>$null
    foreach ($line in $lines) {
      if ($line -match "LISTENING\s+(\d+)\s*$") {
        $ids += [int] $Matches[1]
      }
    }
  }

  return $ids | Where-Object { $_ -and $_ -gt 0 } | Select-Object -Unique
}

function Stop-PortListeners {
  param(
    [int] $Port,
    [System.Collections.Generic.List[string]] $Events
  )

  $ids = @(Get-ListeningProcessIds -Port $Port)
  if ($ids.Count -eq 0) {
    $Events.Add("Port $Port is free.")
    return
  }

  foreach ($processId in $ids) {
    $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
    $name = if ($proc) { $proc.ProcessName } else { "unknown" }
    $Events.Add("Port $Port is occupied by PID $processId ($name). Stopping it before backend startup.")
    Stop-Process -Id $processId -Force -ErrorAction Stop
  }

  for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 300
    if (@(Get-ListeningProcessIds -Port $Port).Count -eq 0) {
      $Events.Add("Port $Port released.")
      return
    }
  }

  $remaining = @(Get-ListeningProcessIds -Port $Port) -join ", "
  throw "Port $Port is still occupied after stop attempt. Remaining PID(s): $remaining"
}

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$backendDir = Join-Path $repoRoot "backend"
$mvnw = Join-Path $backendDir "mvnw.cmd"
$jar = Join-Path $backendDir "target\backend-0.0.1-SNAPSHOT.jar"
$workDir = Join-Path $repoRoot "work"
$launcher = Join-Path $workDir "backend-launch.cmd"
$outLog = Join-Path $repoRoot "backend.run.log"
$errLog = Join-Path $repoRoot "backend.run.err.log"
$backendPort = 8080

if (-not (Test-Path -LiteralPath $mvnw)) {
  throw "Maven wrapper not found: $mvnw"
}

$repoEnvFile = Join-Path $repoRoot ".env.local"
$backendEnvFile = Join-Path $backendDir ".env.local"
Import-EnvFile $repoEnvFile
Import-EnvFile $backendEnvFile

$java = Resolve-JavaRuntime
$env:DEEPINSIGHT_JAVA_HOME = $java.JavaHome
$env:JAVA_HOME = $java.JavaHome
$env:PATH = (Join-Path $java.JavaHome "bin") + ";" + $env:PATH
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8 $env:MAVEN_OPTS".Trim()
$env:NO_COLOR = "1"
$env:FORCE_COLOR = "0"

$startupEvents = New-Object 'System.Collections.Generic.List[string]'
Stop-PortListeners -Port $backendPort -Events $startupEvents

Reset-LogFile $outLog
Reset-LogFile $errLog
Write-RunLog $outLog "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] DeepInsight backend stdout"
Write-RunLog $outLog "ProjectRoot=$repoRoot"
Write-RunLog $outLog "WorkingDirectory=$backendDir"
Write-RunLog $outLog "JavaHome=$($java.JavaHome)"
Write-RunLog $outLog "Command=java -jar $jar"
Write-RunLog $outLog ""
Write-RunLog $errLog "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] DeepInsight backend stderr"
Write-RunLog $errLog "ProjectRoot=$repoRoot"
Write-RunLog $errLog "WorkingDirectory=$backendDir"
Write-RunLog $errLog "JavaHome=$($java.JavaHome)"
Write-RunLog $errLog "Command=java -jar $jar"
Write-RunLog $errLog ""

foreach ($event in $startupEvents) {
  Write-RunLog $outLog $event
}

if (-not (Test-Path -LiteralPath $workDir)) {
  New-Item -ItemType Directory -Path $workDir -Force | Out-Null
}

$launcherLines = @(
  "@echo off",
  "set ""DEEPINSIGHT_JAVA_HOME=$($java.JavaHome)""",
  "set ""JAVA_HOME=$($java.JavaHome)""",
  "set ""PATH=$($java.JavaHome)\bin;%PATH%""",
  "set ""MAVEN_OPTS=$env:MAVEN_OPTS""",
  "set ""NO_COLOR=1""",
  "set ""FORCE_COLOR=0"""
) + @(Export-EnvFileForBatch $repoEnvFile) + @(Export-EnvFileForBatch $backendEnvFile) + @(
  "cd /d ""$backendDir""",
  """$($java.JavaExe)"" -jar ""$jar"" >> ""$outLog"" 2>> ""$errLog"""
)
Set-Content -LiteralPath $launcher -Encoding ASCII -Value $launcherLines
Write-RunLog $outLog "Launcher=$launcher"

$process = Start-Process -FilePath "cmd.exe" `
  -ArgumentList @("/d", "/c", "`"$launcher`"") `
  -WorkingDirectory $backendDir `
  -WindowStyle Hidden `
  -PassThru

for ($i = 0; $i -lt 45; $i++) {
  Start-Sleep -Seconds 1
  if (@(Get-ListeningProcessIds -Port $backendPort).Count -gt 0) {
    exit 0
  }
  if ($process.HasExited) {
    Write-RunLog $errLog "Backend launcher exited before port $backendPort was available. ExitCode=$($process.ExitCode)"
    exit 1
  }
}

Write-RunLog $errLog "Backend did not become available on port $backendPort within 45 seconds."
exit 1
