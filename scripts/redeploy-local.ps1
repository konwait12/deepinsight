param(
  [switch] $SkipBuild,
  [switch] $SkipLint,
  [switch] $SkipBSARec,
  [switch] $NoStop
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendDir = Join-Path $repoRoot "backend"
$deployDir = Join-Path $repoRoot "deploy"
$distDir = Join-Path $repoRoot "dist"
$runDir = Join-Path $repoRoot ".tmp-deploy-run"

$backendPort = 8080
$frontendPort = 5173
$bsarecPort = 5000

$javaExe = "D:\software\environment\Java\jdk21\bin\java.exe"
$pythonExe = "C:\TOOLS\python\py311\python.exe"
$bsarecRoot = "C:\Users\Madoka\Desktop\web$([char]0x4F5C)$([char]0x4E1A)\BSARec-main-api"
$bsarecApp = Join-Path $bsarecRoot "bsarec_api\app.py"

function Write-Step {
  param([string] $Message)
  Write-Host ""
  Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
  param([string] $Message)
  Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warn {
  param([string] $Message)
  Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Resolve-Application {
  param([string] $Name)
  $cmd = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $cmd) {
    throw "Cannot find $Name on PATH."
  }
  return $cmd.Source
}

function Test-TcpPort {
  param(
    [string] $HostName,
    [int] $Port,
    [int] $TimeoutMs = 1000
  )

  $client = New-Object Net.Sockets.TcpClient
  try {
    $iar = $client.BeginConnect($HostName, $Port, $null, $null)
    if (-not $iar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
      return $false
    }
    $client.EndConnect($iar)
    return $true
  } catch {
    return $false
  } finally {
    $client.Close()
  }
}

function Import-EnvFile {
  param([string] $Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    return
  }

  Get-Content -LiteralPath $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
      return
    }
    $name, $value = $line -split "=", 2
    $name = $name.Trim()
    $value = $value.Trim()
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
      $value = $value.Substring(1, $value.Length - 2)
    }
    if ($name) {
      Set-Item -Path "Env:$name" -Value $value
    }
  }
}

function Get-ListeningPids {
  param([int] $Port)

  $lines = netstat -ano | Select-String -Pattern ":$Port\s+.*LISTENING"
  $pids = @()
  foreach ($line in $lines) {
    $parts = ($line.ToString().Trim() -split "\s+")
    if ($parts.Count -gt 0) {
      $processIdText = $parts[$parts.Count - 1]
      $processId = 0
      if ([int]::TryParse($processIdText, [ref] $processId)) {
        $pids += $processId
      }
    }
  }
  return $pids | Sort-Object -Unique
}

function Stop-Port {
  param([int] $Port)

  $pids = @(Get-ListeningPids -Port $Port)
  if ($pids.Count -eq 0) {
    Write-Ok "Port $Port is free."
    return
  }

  foreach ($processId in $pids) {
    try {
      $process = Get-Process -Id $processId -ErrorAction Stop
      Stop-Process -Id $processId -Force
      Write-Ok "Stopped port $Port process $processId ($($process.ProcessName))."
    } catch {
      Write-Warn "Could not stop process $processId on port ${Port}: $($_.Exception.Message)"
    }
  }
}

function Invoke-Checked {
  param(
    [string] $FilePath,
    [string[]] $ArgumentList,
    [string] $WorkingDirectory
  )

  Push-Location -LiteralPath $WorkingDirectory
  try {
    & $FilePath @ArgumentList
    if ($LASTEXITCODE -ne 0) {
      throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($ArgumentList -join ' ')"
    }
  } finally {
    Pop-Location
  }
}

function Invoke-UrlCheck {
  param(
    [string] $Name,
    [string] $Url,
    [string] $Method = "GET",
    [string] $Body = $null,
    [int] $TimeoutSec = 90
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSec)
  $headers = @{}
  if ($Body) {
    $headers["Content-Type"] = "application/json"
  }

  while ((Get-Date) -lt $deadline) {
    try {
      if ($Body) {
        Invoke-WebRequest -Uri $Url -Method $Method -Body $Body -Headers $headers -UseBasicParsing -TimeoutSec 5 | Out-Null
      } else {
        Invoke-WebRequest -Uri $Url -Method $Method -UseBasicParsing -TimeoutSec 5 | Out-Null
      }
      Write-Ok "$Name is ready."
      return
    } catch {
      Start-Sleep -Seconds 2
    }
  }

  throw "$Name did not answer in $TimeoutSec seconds: $Url"
}

function New-CmdFile {
  param(
    [string] $Path,
    [string[]] $Lines
  )

  $content = "@echo off`r`nchcp 65001 >nul`r`n" + (($Lines -join "`r`n") + "`r`n")
  $encoding = New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($Path, $content, $encoding)
}

function Start-CmdHidden {
  param(
    [string] $CmdPath,
    [string] $Name
  )

  $process = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$CmdPath`"" -WindowStyle Hidden -PassThru
  Write-Ok "Started $Name, launcher PID $($process.Id)."
}

Write-Host "DeepInsight local redeploy" -ForegroundColor White
Write-Host "Root: $repoRoot"

if (-not (Test-Path -LiteralPath $deployDir)) {
  throw "Missing deploy directory: $deployDir"
}
if (-not (Test-Path -LiteralPath $javaExe)) {
  throw "Java not found: $javaExe"
}

$nodeExe = Resolve-Application "node"
$npmExe = Resolve-Application "npm.cmd"

Import-EnvFile (Join-Path $repoRoot ".env.local")
Import-EnvFile (Join-Path $deployDir ".env")

New-Item -ItemType Directory -Path $runDir -Force | Out-Null

Write-Step "Dependency check"
& $javaExe --version
& $nodeExe --version
if (Test-TcpPort -HostName "localhost" -Port 3306) {
  Write-Ok "MySQL is listening on 3306."
} else {
  Write-Warn "MySQL is not reachable on 3306. Backend login/data APIs may fail."
}
if (Test-TcpPort -HostName "localhost" -Port 6379) {
  Write-Ok "Redis is listening on 6379."
} else {
  Write-Warn "Redis is not reachable on 6379. Verification-code/login related APIs may fail."
}

if (-not $SkipBuild) {
  if (-not $SkipLint) {
    Write-Step "Frontend type check"
    Invoke-Checked -FilePath $npmExe -ArgumentList @("run", "lint") -WorkingDirectory $repoRoot
  }

  Write-Step "Backend package"
  Invoke-Checked -FilePath (Join-Path $backendDir "mvnw.cmd") -ArgumentList @("clean", "package", "-DskipTests") -WorkingDirectory $backendDir

  Write-Step "Frontend build"
  Invoke-Checked -FilePath $npmExe -ArgumentList @("run", "build") -WorkingDirectory $repoRoot

  Write-Step "Sync deploy artifacts"
  Copy-Item -LiteralPath (Join-Path $backendDir "target\backend-0.0.1-SNAPSHOT.jar") -Destination (Join-Path $deployDir "backend.jar") -Force
  robocopy $distDir (Join-Path $deployDir "dist") /MIR | Out-Host
  if ($LASTEXITCODE -ge 8) {
    throw "robocopy failed with exit code $LASTEXITCODE."
  }
  Write-Ok "Deploy artifacts are synced."
} else {
  Write-Step "Skip build"
  Write-Ok "Using existing deploy/backend.jar and deploy/dist."
}

if (-not $NoStop) {
  Write-Step "Stop old local services"
  Stop-Port -Port $backendPort
  Stop-Port -Port $frontendPort
  if (-not $SkipBSARec) {
    Stop-Port -Port $bsarecPort
  }
}

Write-Step "Start backend"
if (Test-TcpPort -HostName "localhost" -Port $backendPort) {
  Write-Warn "Port $backendPort is already in use; reusing the running backend."
} else {
  $backendCmd = Join-Path $runDir "start-backend.cmd"
  New-CmdFile -Path $backendCmd -Lines @(
    "cd /d `"$deployDir`"",
    "set `"DB_HOST=localhost`"",
    "set `"DB_PORT=3306`"",
    "set `"DB_NAME=deepinsight`"",
    "set `"DB_USER=root`"",
    "set `"DB_PASSWORD=123456`"",
    "set `"REDIS_HOST=localhost`"",
    "set `"REDIS_PORT=6379`"",
    "set `"SERVER_PORT=$backendPort`"",
    "set `"APP_CORS_ALLOWED_ORIGINS=http://localhost:$frontendPort,http://127.0.0.1:$frontendPort,http://localhost:3000,http://127.0.0.1:3000`"",
    "set `"JWT_SECRET=RGVlcEluc2lnaHQtSldULVNlY3JldC1LZXktMjAyNS1TZWN1cmUtQXQtTGVhc3QtMzItQnl0ZXMh`"",
    "set `"JWT_EXPIRATION=86400000`"",
    "`"$javaExe`" -jar `"$deployDir\backend.jar`" > `"$deployDir\backend.out.log`" 2> `"$deployDir\backend.err.log`""
  )
  Start-CmdHidden -CmdPath $backendCmd -Name "backend"
}

Write-Step "Start frontend"
if (Test-TcpPort -HostName "localhost" -Port $frontendPort) {
  Write-Warn "Port $frontendPort is already in use; reusing the running frontend."
} else {
  $frontendCmd = Join-Path $runDir "start-frontend.cmd"
  New-CmdFile -Path $frontendCmd -Lines @(
    "cd /d `"$deployDir`"",
    "`"$nodeExe`" `"$deployDir\serve-dist.cjs`" `"$deployDir\dist`" $frontendPort > `"$deployDir\frontend.out.log`" 2> `"$deployDir\frontend.err.log`""
  )
  Start-CmdHidden -CmdPath $frontendCmd -Name "frontend"
}

if (-not $SkipBSARec) {
  Write-Step "Start BSARec API"
  if (Test-TcpPort -HostName "127.0.0.1" -Port $bsarecPort) {
    Write-Warn "Port $bsarecPort is already in use; reusing the running BSARec API."
  } elseif ((Test-Path -LiteralPath $pythonExe) -and (Test-Path -LiteralPath $bsarecApp)) {
    $bsarecOut = Join-Path $repoRoot "bsarec-api.out.log"
    $bsarecErr = Join-Path $repoRoot "bsarec-api.err.log"
    $bsarecCmd = Join-Path $runDir "start-bsarec.cmd"
    New-CmdFile -Path $bsarecCmd -Lines @(
      "cd /d `"$bsarecRoot`"",
      "`"$pythonExe`" `"$bsarecApp`" > `"$bsarecOut`" 2> `"$bsarecErr`""
    )
    Start-CmdHidden -CmdPath $bsarecCmd -Name "BSARec API"
  } else {
    Write-Warn "Skipped BSARec API because Python or app.py was not found."
    Write-Warn "Python: $pythonExe"
    Write-Warn "App: $bsarecApp"
  }
}

Write-Step "Health checks"
Invoke-UrlCheck -Name "Backend" -Url "http://localhost:$backendPort/api/v1/auth/test" -TimeoutSec 90
Invoke-UrlCheck -Name "Frontend" -Url "http://localhost:$frontendPort/" -TimeoutSec 45
Invoke-UrlCheck -Name "Frontend API proxy" -Url "http://localhost:$frontendPort/api/v1/auth/test" -TimeoutSec 45
if (-not $SkipBSARec) {
  Invoke-UrlCheck -Name "BSARec API" -Url "http://127.0.0.1:$bsarecPort/health" -TimeoutSec 45
}

$recommendBody = '{"user_history":[1,2,3],"top_k":5,"include_job_info":true}'
try {
  Invoke-UrlCheck -Name "Prediction API" -Url "http://localhost:$backendPort/api/v1/prediction/recommend" -Method "POST" -Body $recommendBody -TimeoutSec 20
} catch {
  Write-Warn $_.Exception.Message
}

Write-Step "Service summary"
Write-Host "Frontend : http://localhost:$frontendPort"
Write-Host "Backend  : http://localhost:$backendPort"
Write-Host "BSARec   : http://127.0.0.1:$bsarecPort"
Write-Host "Logs     : deploy/backend.out.log, deploy/frontend.out.log, bsarec-api.out.log"
Write-Host ""
Write-Ok "Redeploy flow finished."
