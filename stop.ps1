$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$jarPath = Join-Path $projectRoot 'coding-agent-bootstrap\target\coding-agent-bootstrap-0.1.0-SNAPSHOT.jar'
$port = 8080
$configFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $configFile) {
    foreach ($line in Get-Content -LiteralPath $configFile) {
        if ($line -match '^\s*SERVER_PORT=(\d+)\s*$') {
            $port = [int]$Matches[1]
        }
    }
}

$listeners = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -eq 0) {
    Write-Host "No application is listening on port $port."
    exit 0
}

$legacyJar = 'coding-agent-bootstrap/target/coding-agent-bootstrap-0.1.0-SNAPSHOT.jar'
foreach ($processId in ($listeners | Select-Object -ExpandProperty OwningProcess -Unique)) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId"
    if ($null -eq $process -or $process.Name -ne 'java.exe') { continue }
    $commandLine = $process.CommandLine
    if ($commandLine -notlike "* -jar `"$jarPath`"*" -and
        $commandLine -notlike "* -jar $jarPath*" -and
        $commandLine -notlike "* -jar $legacyJar*") {
        continue
    }
    Stop-Process -Id $processId -ErrorAction Stop
    Write-Host "Stopped Java Coding Agent (PID $processId) on port $port."
    exit 0
}

throw "Port $port is in use, but the listener is not recognized as this project. Nothing was stopped."
