<#
  Start the Authorization Server, Data Custodian and Third Party on the `local` profile (H2, in-memory),
  wait until each reports healthy, and print the URLs. See docs/LOCAL_TESTING.md (#254).

    scripts\local\start-all.ps1            build once (mvn install -DskipTests), then start
    scripts\local\start-all.ps1 -NoBuild   start from the last build

  Logs: .local-run\<app>.log   Stop: scripts\local\stop-all.ps1
#>
param([switch]$NoBuild, [int]$TimeoutSeconds = 240)
$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$runDir = Join-Path $root '.local-run'
New-Item -ItemType Directory -Force $runDir | Out-Null

$apps = @(
    @{ Name = 'authserver';    Module = 'openespi-authserver';    Health = 'http://localhost:9999/actuator/health' },
    @{ Name = 'datacustodian'; Module = 'openespi-datacustodian'; Health = 'http://localhost:8081/DataCustodian/actuator/health' },
    @{ Name = 'thirdparty';    Module = 'openespi-thirdparty';    Health = 'http://localhost:8082/ThirdParty/actuator/health' }
)

function Test-Healthy([string]$url) {
    try { (Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200 } catch { $false }
}

if (-not $NoBuild) {
    Write-Host 'Building all modules (mvn install -DskipTests) ...'
    Push-Location $root
    try { & mvn -q install -DskipTests; if ($LASTEXITCODE -ne 0) { throw 'Build failed' } } finally { Pop-Location }
}

foreach ($app in $apps) {
    if (Test-Healthy $app.Health) { Write-Host "$($app.Name) is already running ($($app.Health))"; continue }
    Write-Host "Starting $($app.Name) (log: .local-run\$($app.Name).log) ..."
    $log = Join-Path $runDir "$($app.Name).log"
    Start-Process -FilePath 'cmd.exe' -WindowStyle Hidden `
        -WorkingDirectory (Join-Path $root $app.Module) `
        -ArgumentList '/c', "mvn -q spring-boot:run -Dspring-boot.run.profiles=local > `"$log`" 2>&1"
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
foreach ($app in $apps) {
    Write-Host -NoNewline "Waiting for $($app.Name) "
    while (-not (Test-Healthy $app.Health)) {
        if ((Get-Date) -gt $deadline) {
            Write-Host ''
            Write-Host "FAILED: $($app.Name) not healthy after ${TimeoutSeconds}s. Last log lines:"
            Get-Content (Join-Path $runDir "$($app.Name).log") -Tail 30 -ErrorAction SilentlyContinue
            exit 1
        }
        Write-Host -NoNewline '.'
        Start-Sleep -Seconds 3
    }
    Write-Host ' up'
}

Write-Host @'

All three applications are up (local profile, in-memory H2: data resets on restart).

  Authorization Server  http://localhost:9999               admin login: /login  (admin / admin, then a passkey)
  Data Custodian        http://localhost:8081/DataCustodian  (admin / admin)
  Third Party           http://localhost:8082/ThirdParty

Next: scripts\local\smoke.ps1    Runbook: docs\LOCAL_TESTING.md    Stop: scripts\local\stop-all.ps1
'@
