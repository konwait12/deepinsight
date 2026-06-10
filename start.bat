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
    Write-Info "Env file not found: $Path"
    return
  }

  Write-Info "Loading env: $Path"
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
      Write-Warn "Skipping invalid env key in ${Path}: $key"
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

  Write-Warn "Ignoring invalid integer env $Name=$value; using $DefaultValue."
  return $DefaultValue
}

function Test-JavaHome {
  param([string] $JavaHome)
  if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    return $false
  }
  return Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe')
}

function Resolve-JavaRuntime {
  $candidates = New-Object System.Collections.Generic.List[string]
  if (-not [string]::IsNullOrWhiteSpace($env:DEEPINSIGHT_JAVA_HOME)) {
    $candidates.Add($env:DEEPINSIGHT_JAVA_HOME)
  }
  $candidates.Add('D:\jdk\java21')
  if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $candidates.Add($env:JAVA_HOME)
  }

  foreach ($candidate in $candidates) {
    if (Test-JavaHome $candidate) {
      $javaExe = Join-Path $candidate 'bin\java.exe'
      return [pscustomobject]@{
        JavaHome = (Resolve-Path -LiteralPath $candidate).Path
        JavaExe = (Resolve-Path -LiteralPath $javaExe).Path
      }
    }
    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
      Write-Warn "Java candidate is not usable: $candidate"
    }
  }

  $javaCommand = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue
  if ($javaCommand) {
    $javaExe = $javaCommand.Source
    $javaHome = Split-Path -Parent (Split-Path -Parent $javaExe)
    return [pscustomobject]@{
      JavaHome = $javaHome
      JavaExe = $javaExe
    }
  }

  throw 'No usable Java runtime found. Set DEEPINSIGHT_JAVA_HOME/JAVA_HOME or install JDK 21 at D:\jdk\java21.'
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
    [string] $Label,
    [string] $ReuseUrl = ''
  )

  $processIds = @(Get-ListeningProcessIds $Port)
  if ($processIds.Count -eq 0) {
    Write-Info "Port $Port is free for $Label."
    return $false
  }

  if (-not [string]::IsNullOrWhiteSpace($ReuseUrl)) {
    $reuse = Test-HttpReachable -Url $ReuseUrl
    if ($reuse.Ok) {
      Write-Warn "Port $Port is already occupied, but $Label is reachable at $ReuseUrl. Reusing the existing service."
      return $true
    }
  }

  foreach ($processId in $processIds) {
    Stop-PidTree -ProcessId $processId -Reason "$Label port $Port"
  }

  if (-not (Wait-PortFree -Port $Port -TimeoutSeconds 12)) {
    if (-not [string]::IsNullOrWhiteSpace($ReuseUrl)) {
      $reuse = Test-HttpReachable -Url $ReuseUrl
      if ($reuse.Ok) {
        Write-Warn "Port $Port is still occupied, but $Label is reachable at $ReuseUrl. Reusing the existing service."
        return $true
      }
    }
    throw "Port $Port is still in use after stopping $Label."
  }

  return $false
}

function Stop-ProjectProcesses {
  param(
    [string] $ProjectRoot,
    [bool] $KeepFrontend = $false
  )

  try {
    $processes = @(Get-CimInstance Win32_Process -ErrorAction Stop | Where-Object {
      $_.ProcessId -ne $PID -and
      -not [string]::IsNullOrWhiteSpace($_.CommandLine) -and
      $_.CommandLine.Contains($ProjectRoot) -and
      ($_.CommandLine -match 'spring-boot:run|mvnw(\.cmd)?' -or ((-not $KeepFrontend) -and $_.CommandLine.Contains('node_modules\vite\bin\vite.js')))
    })
  } catch {
    return
  }

  foreach ($process in $processes) {
    Stop-PidTree -ProcessId ([int] $process.ProcessId) -Reason 'old DeepInsight dev process'
  }
}

function Test-HttpReachable {
  param([string] $Url)

  try {
    $request = [System.Net.WebRequest]::Create($Url)
    $request.Method = 'GET'
    $request.Timeout = 2000
    $request.AllowAutoRedirect = $false
    $response = $request.GetResponse()
    try {
      return [pscustomobject]@{
        Ok = $true
        StatusCode = [int] $response.StatusCode
        Error = $null
      }
    } finally {
      $response.Close()
    }
  } catch [System.Net.WebException] {
    if ($_.Exception.Response) {
      $response = $_.Exception.Response
      try {
        return [pscustomobject]@{
          Ok = $true
          StatusCode = [int] $response.StatusCode
          Error = $_.Exception.Message
        }
      } finally {
        $response.Close()
      }
    }

    return [pscustomobject]@{
      Ok = $false
      StatusCode = $null
      Error = $_.Exception.Message
    }
  } catch {
    return [pscustomobject]@{
      Ok = $false
      StatusCode = $null
      Error = $_.Exception.Message
    }
  }
}

function Wait-HttpReachable {
  param(
    [string] $Name,
    [string] $Url,
    [int] $TimeoutSeconds,
    [System.Diagnostics.Process] $Process
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    $result = Test-HttpReachable -Url $Url
    if ($result.Ok) {
      Write-Info "$Name is reachable at $Url (HTTP $($result.StatusCode))."
      return
    }

    if ($Process) {
      $Process.Refresh()
      if ($Process.HasExited) {
        throw "$Name process exited early with code $($Process.ExitCode)."
      }
    }

    Start-Sleep -Seconds 2
  }

  throw "$Name did not become reachable at $Url within $TimeoutSeconds seconds."
}

function Test-WritableDirectory {
  param(
    [string] $Path,
    [string] $Label
  )

  try {
    if (-not (Test-Path -LiteralPath $Path)) {
      New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }

    $probe = Join-Path $Path ('.write-test.' + [guid]::NewGuid().ToString('N'))
    Set-Content -LiteralPath $probe -Value 'ok' -Encoding ASCII -NoNewline
    Remove-Item -LiteralPath $probe -Force
  } catch {
    $user = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    throw "$Label is not writable: $Path. Current user: $user. Original error: $($_.Exception.Message)"
  }
}

function Reset-LogFile {
  param([string] $Path)

  if (Test-Path -LiteralPath $Path) {
    Remove-Item -LiteralPath $Path -Force
  }
  New-Item -ItemType File -Path $Path -Force | Out-Null
}

function Write-LogHeader {
  param(
    [string] $Path,
    [string] $Name,
    [string] $WorkingDirectory,
    [string] $Command
  )

  $lines = @(
    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Name",
    "WorkingDirectory=$WorkingDirectory",
    "Command=$Command",
    ''
  )
  Add-Content -LiteralPath $Path -Value $lines -Encoding UTF8
}

function Show-LogTail {
  param(
    [string] $Path,
    [string] $Label
  )

  if ((Test-Path -LiteralPath $Path) -and ((Get-Item -LiteralPath $Path).Length -gt 0)) {
    Write-Host ""
    Write-Host "----- $Label tail -----"
    Get-Content -LiteralPath $Path -Tail 40
  }
}

$backendOutLog = $null
$backendErrLog = $null
$viteOutLog = $null
$viteErrLog = $null

try {
  $entryPath = $env:DEEPINSIGHT_BAT
  if ([string]::IsNullOrWhiteSpace($entryPath)) {
    throw 'DEEPINSIGHT_BAT is not set.'
  }

  $scriptDir = Split-Path -Parent $entryPath
  $projectRoot = Resolve-ProjectRoot -StartPath $scriptDir
  $backendDir = Join-Path $projectRoot 'backend'
  $mvnw = Join-Path $backendDir 'mvnw.cmd'
  $viteBin = Join-Path $projectRoot 'node_modules\vite\bin\vite.js'

  Set-Location -LiteralPath $projectRoot

  Write-Host '========================================'
  Write-Host ' DeepInsight local startup'
  Write-Host '========================================'
  Write-Info "Project root: $projectRoot"

  Import-EnvFile -Path (Join-Path $projectRoot '.env.local')
  Import-EnvFile -Path (Join-Path $backendDir '.env.local')

  $backendPort = Get-EnvInt -Name 'SERVER_PORT' -DefaultValue 8080
  $frontendPort = 5173
  [Environment]::SetEnvironmentVariable('SERVER_PORT', [string] $backendPort, 'Process')
  [Environment]::SetEnvironmentVariable('FRONTEND_PORT', [string] $frontendPort, 'Process')
  [Environment]::SetEnvironmentVariable('NO_COLOR', '1', 'Process')
  [Environment]::SetEnvironmentVariable('FORCE_COLOR', '0', 'Process')
  if ([string]::IsNullOrWhiteSpace($env:APP_CORS_ALLOWED_ORIGINS)) {
    $origins = "http://localhost:$frontendPort,http://127.0.0.1:$frontendPort,http://localhost:3000,http://127.0.0.1:3000"
    [Environment]::SetEnvironmentVariable('APP_CORS_ALLOWED_ORIGINS', $origins, 'Process')
  }

  if (-not (Test-Path -LiteralPath $mvnw)) {
    throw "Maven wrapper not found: $mvnw"
  }
  if (-not (Test-Path -LiteralPath $viteBin)) {
    throw "Vite entry not found: $viteBin. Run npm install first."
  }

  $nodeCommand = @(Get-Command node.exe -CommandType Application -ErrorAction SilentlyContinue |
    Sort-Object @{ Expression = { if ($_.Source -like '*WindowsApps*OpenAI.Codex*') { 1 } else { 0 } } }, Source |
    Select-Object -First 1)
  if (-not $nodeCommand) {
    throw 'node.exe was not found on PATH.'
  }

  $java = Resolve-JavaRuntime
  [Environment]::SetEnvironmentVariable('DEEPINSIGHT_JAVA_HOME', $java.JavaHome, 'Process')
  [Environment]::SetEnvironmentVariable('JAVA_HOME', $java.JavaHome, 'Process')
  [Environment]::SetEnvironmentVariable('MAVEN_OPTS', "-Dfile.encoding=UTF-8 $env:MAVEN_OPTS", 'Process')
  $env:PATH = (Join-Path $java.JavaHome 'bin') + ';' + $env:PATH

  $javaVersion = & $java.JavaExe --version 2>&1 | Select-Object -First 1
  if ($javaVersion -notmatch '\b21\b') {
    Write-Warn "Java version does not look like JDK 21: $javaVersion"
  }

  Test-WritableDirectory -Path (Join-Path $projectRoot 'node_modules\.vite-temp') -Label 'Vite temp directory'

  $reuseBackend = Stop-ListeningPort -Port $backendPort -Label 'backend'
  $reuseFrontend = Stop-ListeningPort -Port $frontendPort -Label 'frontend' -ReuseUrl "http://127.0.0.1:$frontendPort/"
  Stop-ProjectProcesses -ProjectRoot $projectRoot -KeepFrontend $reuseFrontend

  $backendOutLog = Join-Path $projectRoot 'backend.run.log'
  $backendErrLog = Join-Path $projectRoot 'backend.run.err.log'
  $viteOutLog = Join-Path $projectRoot 'vite.run.log'
  $viteErrLog = Join-Path $projectRoot 'vite.run.err.log'
  foreach ($logFile in @($backendOutLog, $backendErrLog)) {
    Reset-LogFile -Path $logFile
  }
  if (-not $reuseFrontend) {
    foreach ($logFile in @($viteOutLog, $viteErrLog)) {
      Reset-LogFile -Path $logFile
    }
  }

  Write-Info "Java: $($java.JavaExe)"
  Write-Info "Node: $($nodeCommand.Source)"
  Write-Info "Backend logs: $backendOutLog / $backendErrLog"
  if ($reuseFrontend) {
    Write-Info "Frontend logs: reusing existing Vite process; log files were left untouched."
  } else {
    Write-Info "Frontend logs: $viteOutLog / $viteErrLog"
  }

  Write-Host ''
  Write-Info "Starting backend on http://127.0.0.1:$backendPort ..."
  Write-LogHeader -Path $backendOutLog -Name 'DeepInsight backend stdout' -WorkingDirectory $backendDir -Command """$mvnw"" spring-boot:run"
  Write-LogHeader -Path $backendErrLog -Name 'DeepInsight backend stderr' -WorkingDirectory $backendDir -Command """$mvnw"" spring-boot:run"
  $backendCmd = '""' + $mvnw + '" spring-boot:run 1>>"' + $backendOutLog + '" 2>>"' + $backendErrLog + '""'
  $backendProcess = Start-Process `
    -FilePath 'cmd.exe' `
    -ArgumentList @('/d', '/s', '/c', $backendCmd) `
    -WorkingDirectory $backendDir `
    -WindowStyle Hidden `
    -PassThru
  Write-Info "Backend process started: PID $($backendProcess.Id)"
  Wait-HttpReachable -Name 'Backend' -Url "http://127.0.0.1:$backendPort/" -TimeoutSeconds 120 -Process $backendProcess

  if ($reuseFrontend) {
    Write-Host ''
    Write-Info "Reusing frontend on http://127.0.0.1:$frontendPort ..."
    Wait-HttpReachable -Name 'Frontend' -Url "http://127.0.0.1:$frontendPort/" -TimeoutSeconds 10 -Process $null
  } else {
    Write-Host ''
    Write-Info "Starting frontend on http://127.0.0.1:$frontendPort ..."
    Write-LogHeader -Path $viteOutLog -Name 'DeepInsight frontend stdout' -WorkingDirectory $projectRoot -Command """$($nodeCommand.Source)"" ""$viteBin"" --host 0.0.0.0 --port $frontendPort --strictPort --clearScreen=false"
    Write-LogHeader -Path $viteErrLog -Name 'DeepInsight frontend stderr' -WorkingDirectory $projectRoot -Command """$($nodeCommand.Source)"" ""$viteBin"" --host 0.0.0.0 --port $frontendPort --strictPort --clearScreen=false"
    $viteCmd = '""' + $nodeCommand.Source + '" "' + $viteBin + '" --host 0.0.0.0 --port ' + $frontendPort + ' --strictPort --clearScreen=false 1>>"' + $viteOutLog + '" 2>>"' + $viteErrLog + '""'
    $viteProcess = Start-Process `
      -FilePath 'cmd.exe' `
      -ArgumentList @('/d', '/s', '/c', $viteCmd) `
      -WorkingDirectory $projectRoot `
      -WindowStyle Hidden `
      -PassThru
    Write-Info "Frontend process started: PID $($viteProcess.Id)"
    Wait-HttpReachable -Name 'Frontend' -Url "http://127.0.0.1:$frontendPort/" -TimeoutSeconds 45 -Process $viteProcess
  }

  Write-Host ''
  Write-Host '========================================'
  Write-Host ' DeepInsight is running'
  Write-Host " Frontend: http://localhost:$frontendPort"
  Write-Host " Backend : http://localhost:$backendPort"
  Write-Host ' Stop    : stop.bat'
  Write-Host '========================================'
  exit 0
} catch {
  Write-Host ''
  Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
  if ($backendErrLog) { Show-LogTail -Path $backendErrLog -Label 'backend.run.err.log' }
  if ($backendOutLog) { Show-LogTail -Path $backendOutLog -Label 'backend.run.log' }
  if ($viteErrLog) { Show-LogTail -Path $viteErrLog -Label 'vite.run.err.log' }
  if ($viteOutLog) { Show-LogTail -Path $viteOutLog -Label 'vite.run.log' }
  exit 1
}
