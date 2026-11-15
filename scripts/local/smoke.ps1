<#
  Non-interactive check of the three locally running applications (start them with start-all.ps1).
  Uses only the DEV ONLY credentials of the `local` profile (docs/LOCAL_TESTING.md). Exit code 0 = all PASS.
  Requires PowerShell 7+.
#>
$AS = 'http://localhost:9999'
$DC = 'http://localhost:8081/DataCustodian'
$TP = 'http://localhost:8082/ThirdParty'
$script:pass = 0
$script:fail = 0

function Check([string]$description, $expected, $actual) {
    if ("$actual" -eq "$expected") { Write-Host "PASS  $description"; $script:pass++ }
    else { Write-Host "FAIL  $description (expected $expected, got $actual)"; $script:fail++ }
}

function Basic([string]$id, [string]$secret) {
    @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${id}:${secret}")) }
}

function Bearer([string]$token) { @{ Authorization = "Bearer $token" } }

function Status([string]$uri, [hashtable]$headers = @{}, [string]$method = 'GET', $body = $null) {
    try {
        (Invoke-WebRequest -Uri $uri -Method $method -Headers $headers -Body $body -UseBasicParsing `
            -SkipHttpErrorCheck -MaximumRedirection 0 -ErrorAction Stop).StatusCode
    } catch { 'no response' }
}

function Token([string]$id, [string]$secret, [string]$scope) {
    try {
        (Invoke-RestMethod -Uri "$AS/oauth2/token" -Method Post -Headers (Basic $id $secret) `
            -Body @{ grant_type = 'client_credentials'; scope = $scope }).access_token
    } catch { $null }
}

Write-Host '== Health'
Check 'AS health' 200 (Status "$AS/actuator/health")
Check 'DC health' 200 (Status "$DC/actuator/health")
Check 'TP health' 200 (Status "$TP/actuator/health")

Write-Host '== Client credentials tokens (AS)'
$dcAdmin   = Token 'data_custodian_admin' 'dc-secret' 'DataCustodian_Admin_Access'
$tpClient  = Token 'third_party' 'tp-secret' 'ThirdParty_Admin_Access'
$asAdmin   = Token 'espi_admin' 'local-admin-secret' 'authserver.admin'
$registrar = Token 'espi_registrar' 'local-registrar-secret' 'client.create'
Check 'data_custodian_admin -> DataCustodian_Admin_Access' $true ([bool]$dcAdmin)
Check 'third_party -> ThirdParty_Admin_Access (client access token)' $true ([bool]$tpClient)
Check 'espi_admin -> authserver.admin' $true ([bool]$asAdmin)
Check 'espi_registrar -> client.create (RFC 7591 initial access token)' $true ([bool]$registrar)
Check 'third_party cannot get a customer FB scope with client_credentials' 400 `
    (Status "$AS/oauth2/token" (Basic 'third_party' 'tp-secret') 'POST' `
        @{ grant_type = 'client_credentials'; scope = 'FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13' })

Write-Host '== Introspection'
$introspection = try {
    Invoke-RestMethod -Uri "$AS/oauth2/introspect" -Method Post -Headers (Basic 'datacustodian' 'datacustodian-secret') `
        -Body @{ token = $dcAdmin }
} catch { $null }
Check 'DC introspection client sees the DC admin token as active' $true ([bool]$introspection.active)

Write-Host '== Data Custodian ESPI feeds (DataCustodian_Admin_Access)'
foreach ($resource in 'ApplicationInformation', 'Authorization', 'UsagePoint', 'MeterReading', 'ReadingType',
                      'IntervalBlock', 'UsageSummary', 'ElectricPowerQualitySummary', 'Customer', 'CustomerAccount') {
    Check "GET $resource feed" 200 (Status "$DC/espi/1_1/resource/$resource" (Bearer $dcAdmin))
}
Check 'GET Authorization feed with the Third Party client access token' 200 `
    (Status "$DC/espi/1_1/resource/Authorization" (Bearer $tpClient))
Check 'GET UsagePoint feed without a token' 401 (Status "$DC/espi/1_1/resource/UsagePoint")

Write-Host '== Admin actuator endpoints'
Check 'AS /actuator/info (authserver.admin)' 200 (Status "$AS/actuator/info" (Bearer $asAdmin))
Check 'DC /actuator/info (DataCustodian_Admin_Access)' 200 (Status "$DC/actuator/info" (Bearer $dcAdmin))
Check 'TP /actuator/info (ThirdParty_Admin_Access)' 200 (Status "$TP/actuator/info" (Bearer $tpClient))
Check 'AS /actuator/info without a token' 401 (Status "$AS/actuator/info")

Write-Host ''
Write-Host "$($script:pass) passed, $($script:fail) failed"
exit ([int]($script:fail -gt 0))
