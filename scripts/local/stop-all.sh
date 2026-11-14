#!/usr/bin/env bash
#
# Stop the locally started Authorization Server (9999), Data Custodian (8081) and Third Party (8082)
# by the port each listens on. Works on Linux/macOS (lsof) and Git Bash on Windows (netstat/taskkill).
set -uo pipefail

for port in 9999 8081 8082; do
  if command -v lsof > /dev/null 2>&1; then
    pids=$(lsof -ti "tcp:$port" -sTCP:LISTEN 2>/dev/null || true)
    if [[ -n "$pids" ]]; then
      echo "Stopping port $port (pid $pids)"
      kill $pids 2>/dev/null || true
    else
      echo "Nothing listening on $port"
    fi
  else
    pids=$(netstat -ano 2>/dev/null | awk -v p=":$port" '$2 ~ p"$" && $4 == "LISTENING" {print $5}' | sort -u)
    if [[ -n "$pids" ]]; then
      for pid in $pids; do
        echo "Stopping port $port (pid $pid)"
        taskkill //PID "$pid" //T //F > /dev/null 2>&1 || true
      done
    else
      echo "Nothing listening on $port"
    fi
  fi
done
