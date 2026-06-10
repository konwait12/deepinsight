$ErrorActionPreference = "Stop"

function Reset-LogFile {
  param([string] $Path)

  if (Test-Path -LiteralPath $Path) {
    Remove-Item -LiteralPath $Path -Force
  }
  New-Item -ItemType File -Path $Path -Force | Out-Null
}

function Write-RunLog {
  param(
    [string] $Path,
    [string] $Message
  )

  Add-Content -LiteralPath $Path -Encoding UTF8 -Value $Message
}

function Test-WritableDirectory {
  param(
    [string] $Path,
    [string] $Label
  )

  if (-not (Test-Path -LiteralPath $Path)) {
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
  }

  $probe = Join-Path $Path (".write-test." + [guid]::NewGuid().ToString("N"))
  try {
    Set-Content -LiteralPath $probe -Encoding ASCII -NoNewline -Value "ok"
  } catch {
    $user = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    throw "$Label is not writable: $Path. Current user: $user. Original error: $($_.Exception.Message)"
  } finally {
    if (Test-Path -LiteralPath $probe) {
      Remove-Item -LiteralPath $probe -Force
    }
  }
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
    $Events.Add("Port $Port is occupied by PID $processId ($name). Stopping it before frontend startup.")
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
$viteBin = Join-Path $repoRoot "node_modules\vite\bin\vite.js"
$outLog = Join-Path $repoRoot "vite.run.log"
$errLog = Join-Path $repoRoot "vite.run.err.log"
$viteTemp = Join-Path $repoRoot "node_modules\.vite-temp"
$frontendPort = 5173

if (-not (Test-Path -LiteralPath $viteBin)) {
  throw "Vite entry not found: $viteBin. Run npm install first."
}

$node = @(Get-Command node.exe -CommandType Application -ErrorAction SilentlyContinue |
  Sort-Object @{ Expression = { if ($_.Source -like "*WindowsApps*OpenAI.Codex*") { 1 } else { 0 } } }, Source |
  Select-Object -First 1)
if (-not $node) {
  throw "node.exe was not found on PATH."
}

Test-WritableDirectory -Path $viteTemp -Label "Vite temp directory"

$startupEvents = New-Object 'System.Collections.Generic.List[string]'
Stop-PortListeners -Port $frontendPort -Events $startupEvents

Reset-LogFile $outLog
Reset-LogFile $errLog
Write-RunLog $outLog "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] DeepInsight frontend stdout"
Write-RunLog $outLog "ProjectRoot=$repoRoot"
Write-RunLog $outLog "WorkingDirectory=$repoRoot"
Write-RunLog $outLog "Command=$($node.Source) $viteBin --host 0.0.0.0 --port $frontendPort --strictPort --clearScreen=false"
Write-RunLog $outLog ""
Write-RunLog $errLog "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] DeepInsight frontend stderr"
Write-RunLog $errLog "ProjectRoot=$repoRoot"
Write-RunLog $errLog "WorkingDirectory=$repoRoot"
Write-RunLog $errLog "Command=$($node.Source) $viteBin --host 0.0.0.0 --port $frontendPort --strictPort --clearScreen=false"
Write-RunLog $errLog ""

foreach ($event in $startupEvents) {
  Write-RunLog $outLog $event
}

Set-Location -LiteralPath $repoRoot
$viteCmd = '"' + $node.Source + '" "' + $viteBin + '" --host 0.0.0.0 --port ' + $frontendPort + ' --strictPort --clearScreen=false 1>>"' + $outLog + '" 2>>"' + $errLog + '"'
& cmd.exe /d /c $viteCmd
exit $LASTEXITCODE
