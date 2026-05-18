$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$viteBin = Join-Path $repoRoot "node_modules\vite\bin\vite.js"
$logFile = Join-Path $repoRoot "vite.run.log"

Set-Location -LiteralPath $repoRoot
& node $viteBin "--port=3000" "--host=0.0.0.0" *> $logFile
