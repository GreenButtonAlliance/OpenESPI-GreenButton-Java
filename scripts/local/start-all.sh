#!/usr/bin/env bash
#
# Start the Authorization Server, Data Custodian and Third Party on the `local` profile (H2, in-memory),
# wait until each reports healthy, and print the URLs. See docs/LOCAL_TESTING.md (#254).
#
#   scripts/local/start-all.sh             build once (mvn install -DskipTests), then start
#   scripts/local/start-all.sh --no-build  start from the last build
#
# Logs: .local-run/<app>.log   Stop: scripts/local/stop-all.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUN_DIR="$ROOT/.local-run"
TIMEOUT_SECONDS="${ESPI_START_TIMEOUT:-240}"
mkdir -p "$RUN_DIR"

# name | module | health URL
APPS=(
  "authserver|openespi-authserver|http://localhost:9999/actuator/health"
  "datacustodian|openespi-datacustodian|http://localhost:8081/DataCustodian/actuator/health"
  "thirdparty|openespi-thirdparty|http://localhost:8082/ThirdParty/actuator/health"
)

if [[ "${1:-}" != "--no-build" ]]; then
  echo "Building all modules (mvn install -DskipTests) ..."
  (cd "$ROOT" && mvn -q install -DskipTests)
fi

for app in "${APPS[@]}"; do
  IFS='|' read -r name module health <<< "$app"
  if curl -fs -o /dev/null "$health"; then
    echo "$name is already running ($health)"
    continue
  fi
  echo "Starting $name (log: .local-run/$name.log) ..."
  (cd "$ROOT/$module" && nohup mvn -q spring-boot:run -Dspring-boot.run.profiles=local \
      > "$RUN_DIR/$name.log" 2>&1 &)
done

deadline=$((SECONDS + TIMEOUT_SECONDS))
for app in "${APPS[@]}"; do
  IFS='|' read -r name module health <<< "$app"
  printf "Waiting for %s " "$name"
  until curl -fs -o /dev/null "$health"; do
    if (( SECONDS > deadline )); then
      echo; echo "FAILED: $name not healthy after ${TIMEOUT_SECONDS}s. Last log lines:"
      tail -n 30 "$RUN_DIR/$name.log" || true
      exit 1
    fi
    printf "."
    sleep 3
  done
  echo " up"
done

cat <<EOF

All three applications are up (local profile, in-memory H2: data resets on restart).

  Authorization Server  http://localhost:9999               admin login: /login  (admin / admin, then a passkey)
  Data Custodian        http://localhost:8081/DataCustodian  (admin / admin)
  Third Party           http://localhost:8082/ThirdParty

Next: scripts/local/smoke.sh    Runbook: docs/LOCAL_TESTING.md    Stop: scripts/local/stop-all.sh
EOF
