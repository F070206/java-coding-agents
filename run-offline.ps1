$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$configFile = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $configFile)) {
    throw 'Missing .env. Copy .env.example and set local administrator credentials.'
}

foreach ($line in Get-Content -LiteralPath $configFile) {
    if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}

if (-not $env:ADMIN_USERNAME -or -not $env:ADMIN_PASSWORD -or -not $env:JWT_SECRET) {
    throw 'ADMIN_USERNAME, ADMIN_PASSWORD and JWT_SECRET must be set in .env.'
}

# A configured key may be present locally; blank model settings guarantee the deterministic adapter.
$env:LLM_BASE_URL = ''
$env:LLM_MODEL_NAME = ''
Write-Host 'Building and starting Java Coding Agent in offline mode...'
Push-Location $projectRoot
try {
    & mvn -s .mvn/settings.xml -pl coding-agent-bootstrap -am -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
    & java -jar coding-agent-bootstrap/target/coding-agent-bootstrap-0.1.0-SNAPSHOT.jar
    if ($LASTEXITCODE -ne 0) { throw 'Application stopped with an error.' }
} finally {
    Pop-Location
}
