#!/usr/bin/env bash
#
# Non-interactive check of the three locally running applications (start them with start-all.sh).
# Uses only the DEV ONLY credentials of the `local` profile (docs/LOCAL_TESTING.md). Exit code 0 = all PASS.
set -uo pipefail

AS=http://localhost:9999
DC=http://localhost:8081/DataCustodian
TP=http://localhost:8082/ThirdParty
pass=0
fail=0

check() { # check <description> <expected HTTP status> <actual HTTP status>
  if [[ "$3" == "$2" ]]; then
    echo "PASS  $1"; pass=$((pass + 1))
  else
    echo "FAIL  $1 (expected HTTP $2, got $3)"; fail=$((fail + 1))
  fi
}

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

token() { # token <client_id> <secret> <scope>  -> prints the access token, or nothing
  curl -s -u "$1:$2" -d grant_type=client_credentials --data-urlencode "scope=$3" "$AS/oauth2/token" \
    | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
}

echo "== Health"
check "AS health" 200 "$(status "$AS/actuator/health")"
check "DC health" 200 "$(status "$DC/actuator/health")"
check "TP health" 200 "$(status "$TP/actuator/health")"

echo "== Client credentials tokens (AS)"
DC_ADMIN=$(token data_custodian_admin dc-secret DataCustodian_Admin_Access)
TP_CLIENT=$(token third_party tp-secret ThirdParty_Admin_Access)
AS_ADMIN=$(token espi_admin local-admin-secret authserver.admin)
REGISTRAR=$(token espi_registrar local-registrar-secret client.create)
check "data_custodian_admin -> DataCustodian_Admin_Access" yes "$([[ -n "$DC_ADMIN" ]] && echo yes || echo no)"
check "third_party -> ThirdParty_Admin_Access (client access token)" yes "$([[ -n "$TP_CLIENT" ]] && echo yes || echo no)"
check "espi_admin -> authserver.admin" yes "$([[ -n "$AS_ADMIN" ]] && echo yes || echo no)"
check "espi_registrar -> client.create (RFC 7591 initial access token)" yes "$([[ -n "$REGISTRAR" ]] && echo yes || echo no)"
check "third_party cannot get a customer FB scope with client_credentials" 400 \
  "$(status -u third_party:tp-secret -d grant_type=client_credentials \
      --data-urlencode 'scope=FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13' "$AS/oauth2/token")"

echo "== Introspection"
active=$(curl -s -u datacustodian:datacustodian-secret -d "token=$DC_ADMIN" "$AS/oauth2/introspect")
check "DC introspection client sees the DC admin token as active" yes \
  "$([[ "$active" == *'"active":true'* ]] && echo yes || echo no)"

echo "== Data Custodian ESPI feeds (DataCustodian_Admin_Access)"
for resource in ApplicationInformation Authorization UsagePoint MeterReading ReadingType IntervalBlock \
                UsageSummary ElectricPowerQualitySummary Customer CustomerAccount; do
  check "GET $resource feed" 200 "$(status -H "Authorization: Bearer $DC_ADMIN" "$DC/espi/1_1/resource/$resource")"
done
check "GET Authorization feed with the Third Party client access token" 200 \
  "$(status -H "Authorization: Bearer $TP_CLIENT" "$DC/espi/1_1/resource/Authorization")"
check "GET UsagePoint feed without a token" 401 "$(status "$DC/espi/1_1/resource/UsagePoint")"

echo "== Admin actuator endpoints"
check "AS /actuator/info (authserver.admin)" 200 "$(status -H "Authorization: Bearer $AS_ADMIN" "$AS/actuator/info")"
check "DC /actuator/info (DataCustodian_Admin_Access)" 200 "$(status -H "Authorization: Bearer $DC_ADMIN" "$DC/actuator/info")"
check "TP /actuator/info (ThirdParty_Admin_Access)" 200 "$(status -H "Authorization: Bearer $TP_CLIENT" "$TP/actuator/info")"
check "AS /actuator/info without a token" 401 "$(status "$AS/actuator/info")"

echo
echo "$pass passed, $fail failed"
(( fail == 0 ))
