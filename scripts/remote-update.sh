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
  systemctl restart fridgeclear
  echo "==> backend restarting..."
  for _ in $(seq 1 90); do
    if ss -tln 2>/dev/null | grep -q ':8080 '; then
      sleep 2
      echo "==> backend restarted"
      break
    fi
    sleep 1
  done
fi

echo "==> update done"
