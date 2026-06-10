@echo off
chcp 65001 >nul
setlocal EnableExtensions
set "DEEPINSIGHT_BAT=%~f0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "try { $path = $env:DEEPINSIGHT_BAT; $script = Get-Content -LiteralPath $path -Raw; $marker = '# POWERSHELL_PAYLOAD_START'; $idx = $script.LastIndexOf($marker); if ($idx -lt 0) { throw 'PowerShell payload marker not found.' }; Invoke-Expression ($script.Substring($idx + $marker.Length)) } catch { Write-Host ('[ERROR] ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }"
set "EXIT_CODE=%ERRORLEVEL%"
if not defined DEEPINSIGHT_NO_PAUSE pause
exit /b %EXIT_CODE%
# POWERSHELL_PAYLOAD_START

$ErrorActionPreference = 'Stop'

function Write-Info {
  param([string] $Message)
  Write-Host "[INFO] $Message"
}

function Write-Warn {
  param([string] $Message)
  Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Resolve-ProjectRoot {
  param([string] $StartPath)

  $dir = Resolve-Path -LiteralPath $StartPath
  $current = $dir.Path

  while ($true) {
    $packageJson = Join-Path $current 'package.json'
    $backendPom = Join-Path $current 'backend\pom.xml'
    if ((Test-Path -LiteralPath $packageJson) -and (Test-Path -LiteralPath $backendPom)) {
      return $current
    }

    $parent = Split-Path -Parent $current
    if ([string]::IsNullOrWhiteSpace($parent) -or $parent -eq $current) {
      throw "Cannot locate project root from $StartPath. Expected package.json and backend\pom.xml."
    }
    $current = $parent
  }
}

function Import-EnvFile {
  param([string] $Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    return
  }

  foreach ($rawLine in Get-Content -LiteralPath $Path) {
    $line = $rawLine.Trim().Trim([char] 0xFEFF)
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
      continue
    }

    $separator = $line.IndexOf('=')
    if ($separator -le 0) {
      continue
    }

    $key = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()
    if ($key -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
      continue
    }

    if ($value.Length -ge 2) {
      $first = $value.Substring(0, 1)
      $last = $value.Substring($value.Length - 1, 1)
      if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }

    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
  }
}

function Get-EnvInt {
  param(
    [string] $Name,
    [int] $DefaultValue
  )

  $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
  if ([string]::IsNullOrWhiteSpace($value)) {
    return $DefaultValue
  }

  $parsed = 0
  if ([int]::TryParse($value, [ref] $parsed)) {
    return $parsed
  }

  return $DefaultValue
}

function Get-ListeningProcessIds {
  param([int] $Port)

  $ids = @()
  try {
    $ids += @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop | ForEach-Object { [int] $_.OwningProcess })
  } catch {
    $ids = @()
  }

  if ($ids.Count -eq 0) {
    foreach ($line in (& netstat.exe -ano -p tcp 2>$null)) {
      if ($line -match "^\s*TCP\s+\S+:$Port\s+\S+\s+LISTENING\s+(\d+)\s*$") {
        $ids += [int] $Matches[1]
      }
    }
  }

  return @($ids | Where-Object { $_ -gt 0 } | Sort-Object -Unique)
}

function Test-PortOpen {
  param([int] $Port)

  $client = New-Object Net.Sockets.TcpClient
  try {
    $async = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
    if (-not $async.AsyncWaitHandle.WaitOne(600, $false)) {
      return $false
    }
    $client.EndConnect($async)
    return $true
  } catch {
    return $false
  } finally {
    $client.Close()
  }
}

function Stop-PidTree {
  param(
    [int] $ProcessId,
    [string] $Reason
  )

  if ($ProcessId -eq $PID) {
    return
  }

  $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
  if (-not $process) {
    return
  }

  Write-Info "Stopping PID $ProcessId ($($process.ProcessName)) for $Reason."
  & taskkill.exe /PID $ProcessId /T /F | Out-Null
}

function Wait-PortFree {
  param(
    [int] $Port,
    [int] $TimeoutSeconds = 10
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (-not (Test-PortOpen $Port)) {
      return $true
    }
    Start-Sleep -Milliseconds 500
  }

  return -not (Test-PortOpen $Port)
}

function Stop-ListeningPort {
  param(
    [int] $Port,
    [string] $Label
  )

  $processIds = @(Get-ListeningProcessIds $Port)
  if ($processIds.Count -eq 0) {
    Write-Info "Port $Port is already free for $Label."
    return
  }

  foreach ($processId in $processIds) {
    Stop-PidTree -ProcessId $processId -Reason "$Label port $Port"
  }

  if (Wait-PortFree -Port $Port -TimeoutSeconds 12) {
    Write-Info "Port $Port is free."
  } else {
    Write-Warn "Port $Port is still in use."
  }
}

function Stop-ProjectProcesses {
  param([string] $ProjectRoot)

  try {
    $processes = @(Get-CimInstance Win32_Process -ErrorAction Stop | Where-Object {
      $_.ProcessId -ne $PID -and
      -not [string]::IsNullOrWhiteSpace($_.CommandLine) -and
      $_.CommandLine.Contains($ProjectRoot) -and
      ($_.CommandLine -match 'spring-boot:run|mvnw(\.cmd)?' -or $_.CommandLine.Contains('node_modules\vite\bin\vite.js'))
    })
  } catch {
    return
  }

  foreach ($process in $processes) {
    Stop-PidTree -ProcessId ([int] $process.ProcessId) -Reason 'DeepInsight dev process'
  }
}

try {
  $entryPath = $env:DEEPINSIGHT_BAT
  if ([string]::IsNullOrWhiteSpace($entryPath)) {
    throw 'DEEPINSIGHT_BAT is not set.'
  }

  $scriptDir = Split-Path -Parent $entryPath
  $projectRoot = Resolve-ProjectRoot -StartPath $scriptDir
  $backendDir = Join-Path $projectRoot 'backend'

  Import-EnvFile -Path (Join-Path $projectRoot '.env.local')
  Import-EnvFile -Path (Join-Path $backendDir '.env.local')

  $backendPort = Get-EnvInt -Name 'SERVER_PORT' -DefaultValue 8080
  $frontendPort = 5173

  Write-Host '========================================'
  Write-Host ' DeepInsight local stop'
  Write-Host '========================================'
  Write-Info "Project root: $projectRoot"

  Stop-ListeningPort -Port $backendPort -Label 'backend'
  Stop-ListeningPort -Port $frontendPort -Label 'frontend'
  Stop-ProjectProcesses -ProjectRoot $projectRoot

  if (Test-PortOpen -Port $backendPort) {
    Write-Warn "Backend port $backendPort is still open."
  } else {
    Write-Info "Backend port $backendPort verified free."
  }

  if (Test-PortOpen -Port $frontendPort) {
    Write-Warn "Frontend port $frontendPort is still open."
  } else {
    Write-Info "Frontend port $frontendPort verified free."
  }

  Write-Host '========================================'
  Write-Host ' Stop completed'
  Write-Host '========================================'
  exit 0
} catch {
  Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}
