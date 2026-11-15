<#
  Stop the locally started Authorization Server (9999), Data Custodian (8081) and Third Party (8082)
  by the port each listens on, including the Maven process tree that launched it.
#>
foreach ($port in 9999, 8081, 8082) {
    $owners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    if (-not $owners) { Write-Host "Nothing listening on $port"; continue }
    foreach ($owner in $owners) {
        Write-Host "Stopping port $port (pid $owner)"
        & taskkill /PID $owner /T /F | Out-Null
    }
}
