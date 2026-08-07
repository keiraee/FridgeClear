#!/usr/bin/env bash
# Server-side update helper (installed to /opt/fridgeclear/update.sh).
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/fridgeclear}"
cd "$APP_DIR"

if [[ -d dist_next ]]; then
  rm -rf dist_prev
  [[ -d dist ]] && mv dist dist_prev
  mv dist_next dist
  rm -rf dist_prev
  echo "==> frontend updated"
fi

if [[ -f app.jar.new ]]; then
  if [[ -f app.jar ]]; then
    cp -a app.jar "app.jar.bak.$(date +%Y%m%d%H%M%S)"
  fi
  mv app.jar.new app.jar
  if command -v systemctl >/dev/null 2>&1; then
    systemctl restart fridgeclear
  else
    echo "WARN: systemctl not found; restart app.jar manually" >&2
  fi
  echo "==> backend restarting..."
  ready=false
  for _ in $(seq 1 90); do
    if ss -tln 2>/dev/null | grep -q ':8080 ' || netstat -tln 2>/dev/null | grep -q ':8080 '; then
      sleep 2
      ready=true
      echo "==> backend restarted"
      break
    fi
    sleep 1
  done
  if [[ "$ready" != true ]]; then
    echo "ERROR: backend did not listen on 8080 within 90s" >&2
    echo "       Check: journalctl -u fridgeclear -n 50" >&2
    exit 1
  fi
fi

echo "==> update done"
