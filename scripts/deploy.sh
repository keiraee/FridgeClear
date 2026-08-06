#!/usr/bin/env bash
# Build locally and deploy to production (rsync + atomic swap on server).
# Usage: ./scripts/deploy.sh [all|frontend|backend]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/scripts/deploy.env"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck source=/dev/null
  source "$ENV_FILE"
fi

DEPLOY_SERVER="${DEPLOY_SERVER:?Set DEPLOY_SERVER in scripts/deploy.env}"
DEPLOY_REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/fridgeclear}"
MODE="${1:-all}"

SSH_OPTS=(-o StrictHostKeyChecking=accept-new)

use_sshpass() {
  [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]] && command -v sshpass >/dev/null 2>&1
}

run_ssh() {
  if use_sshpass; then
    SSHPASS="$DEPLOY_SSH_PASSWORD" sshpass -e ssh "${SSH_OPTS[@]}" "$DEPLOY_SERVER" "$@"
  else
    ssh "${SSH_OPTS[@]}" "$DEPLOY_SERVER" "$@"
  fi
}

run_rsync() {
  local src="$1"
  local dest="$2"
  local delete_flag="${3:-}"

  local -a rsync_args=(-avz)
  if [[ "$delete_flag" == --delete ]]; then
    rsync_args+=(--delete)
  fi

  if use_sshpass; then
    SSHPASS="$DEPLOY_SSH_PASSWORD" rsync "${rsync_args[@]}" \
      -e "sshpass -e ssh -o StrictHostKeyChecking=accept-new" \
      "$src" "$dest"
  else
    rsync "${rsync_args[@]}" -e "ssh -o StrictHostKeyChecking=accept-new" "$src" "$dest"
  fi
}

ensure_remote_update_script() {
  run_rsync "${ROOT_DIR}/scripts/remote-update.sh" "${DEPLOY_SERVER}:${DEPLOY_REMOTE_DIR}/update.sh"
  run_ssh "chmod +x ${DEPLOY_REMOTE_DIR}/update.sh"
}

build_release() {
  case "$MODE" in
    frontend)
      "${ROOT_DIR}/scripts/build-release.sh" --skip-backend
      ;;
    backend)
      "${ROOT_DIR}/scripts/build-release.sh" --skip-frontend
      ;;
    all)
      "${ROOT_DIR}/scripts/build-release.sh"
      ;;
    *)
      echo "Usage: $0 [all|frontend|backend]" >&2
      exit 1
      ;;
  esac
}

echo "==> Deploy mode: $MODE"
echo "    Server: $DEPLOY_SERVER"
echo "    Remote: $DEPLOY_REMOTE_DIR"

if ! command -v rsync >/dev/null 2>&1; then
  echo "ERROR: local rsync not found. Install with: brew install rsync" >&2
  exit 1
fi

if [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]] && ! command -v sshpass >/dev/null 2>&1; then
  echo "WARN: DEPLOY_SSH_PASSWORD is set but sshpass not found." >&2
  echo "      Install: brew install sshpass" >&2
  echo "      Or run: ssh-copy-id $DEPLOY_SERVER" >&2
  exit 1
fi

run_ssh "command -v rsync >/dev/null 2>&1 || (apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y rsync)"

build_release
ensure_remote_update_script

if [[ "$MODE" != backend ]]; then
  echo "==> Uploading frontend to dist_next/"
  run_rsync "${ROOT_DIR}/release/dist/" "${DEPLOY_SERVER}:${DEPLOY_REMOTE_DIR}/dist_next/" --delete
fi

if [[ "$MODE" != frontend ]]; then
  echo "==> Uploading app.jar.new"
  run_rsync "${ROOT_DIR}/release/app.jar" "${DEPLOY_SERVER}:${DEPLOY_REMOTE_DIR}/app.jar.new"
fi

echo "==> Applying update on server"
if [[ "$MODE" == frontend ]]; then
  run_ssh "cd ${DEPLOY_REMOTE_DIR} && rm -rf dist_prev && (mv dist dist_prev 2>/dev/null || true) && mv dist_next dist && rm -rf dist_prev && echo 'frontend ok'"
else
  run_ssh "${DEPLOY_REMOTE_DIR}/update.sh"
fi

echo "==> Deploy ($MODE) finished."
