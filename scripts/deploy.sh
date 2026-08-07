#!/usr/bin/env bash
# Build locally and deploy to production (rsync + atomic swap on server).
# Usage: ./scripts/deploy.sh [all|frontend|backend] [--skip-build]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/scripts/deploy.env"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck source=/dev/null
  source "$ENV_FILE"
fi

DEPLOY_SERVER="${DEPLOY_SERVER:?Set DEPLOY_SERVER in scripts/deploy.env}"
DEPLOY_REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/fridgeclear}"
MODE="all"
SKIP_BUILD=false
MAX_RETRIES="${DEPLOY_MAX_RETRIES:-3}"
RETRY_DELAY="${DEPLOY_RETRY_DELAY_SEC:-3}"

for arg in "$@"; do
  case "$arg" in
    all|frontend|backend) MODE="$arg" ;;
    --skip-build) SKIP_BUILD=true ;;
    -h|--help)
      cat <<'EOF'
Usage: ./scripts/deploy.sh [all|frontend|backend] [--skip-build]

  all        Build jar + dist, upload both (default)
  frontend   Build dist only
  backend    Build jar only
  --skip-build  Reuse existing release/ (retry upload after a failed deploy)

Env (scripts/deploy.env):
  DEPLOY_SERVER           e.g. root@1.2.3.4
  DEPLOY_REMOTE_DIR       default /opt/fridgeclear
  DEPLOY_SSH_PASSWORD     optional; prefer SSH key (leave empty)
  DEPLOY_SSH_IDENTITY_FILE  optional path to private key
  DEPLOY_MAX_RETRIES      default 3
  DEPLOY_RETRY_DELAY_SEC  default 3

Stable setup (recommended):
  ssh-keygen -t ed25519 -f ~/.ssh/fridgeclear_deploy -N ""
  ssh-copy-id -i ~/.ssh/fridgeclear_deploy.pub root@YOUR_HOST
  DEPLOY_SERVER=root@YOUR_HOST
  DEPLOY_SSH_IDENTITY_FILE=~/.ssh/fridgeclear_deploy
  DEPLOY_SSH_PASSWORD=
EOF
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      exit 1
      ;;
  esac
done

SSH_OPTS=(-o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 -o ServerAliveInterval=30)

auth_mode() {
  if [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]] && command -v sshpass >/dev/null 2>&1; then
    echo "sshpass"
  else
    echo "ssh-key"
  fi
}

use_sshpass() {
  [[ "$(auth_mode)" == "sshpass" ]]
}

run_ssh() {
  local -a cmd=(ssh "${SSH_OPTS[@]}")
  if [[ -n "${DEPLOY_SSH_IDENTITY_FILE:-}" ]]; then
    cmd+=(-i "${DEPLOY_SSH_IDENTITY_FILE/#\~/$HOME}")
  fi
  if use_sshpass; then
    SSHPASS="$DEPLOY_SSH_PASSWORD" sshpass -e "${cmd[@]}" "$DEPLOY_SERVER" "$@"
  else
    "${cmd[@]}" "$DEPLOY_SERVER" "$@"
  fi
}

run_rsync() {
  local src="$1"
  local dest="$2"
  local delete_flag="${3:-}"

  local -a rsync_args=(-avz --timeout=120)
  if [[ "$delete_flag" == --delete ]]; then
    rsync_args+=(--delete)
  fi

  local identity_arg=""
  if [[ -n "${DEPLOY_SSH_IDENTITY_FILE:-}" ]]; then
    identity_arg="-i ${DEPLOY_SSH_IDENTITY_FILE/#\~/$HOME}"
  fi

  local ssh_transport="ssh -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 -o ServerAliveInterval=30 ${identity_arg}"

  if use_sshpass; then
    SSHPASS="$DEPLOY_SSH_PASSWORD" rsync "${rsync_args[@]}" \
      -e "sshpass -e ${ssh_transport}" \
      "$src" "$dest"
  else
    rsync "${rsync_args[@]}" -e "$ssh_transport" "$src" "$dest"
  fi
}

with_retry() {
  local desc="$1"
  shift
  local attempt=1
  while (( attempt <= MAX_RETRIES )); do
    if "$@"; then
      return 0
    fi
    if (( attempt < MAX_RETRIES )); then
      echo "WARN: ${desc} failed (${attempt}/${MAX_RETRIES}), retry in ${RETRY_DELAY}s..." >&2
      sleep "$RETRY_DELAY"
    fi
    attempt=$((attempt + 1))
  done
  echo "ERROR: ${desc} failed after ${MAX_RETRIES} attempts" >&2
  return 1
}

preflight() {
  echo "==> Preflight"

  if ! command -v rsync >/dev/null 2>&1; then
    echo "ERROR: local rsync not found (brew install rsync)" >&2
    exit 1
  fi

  if [[ -n "${DEPLOY_SSH_PASSWORD:-}" ]] && ! command -v sshpass >/dev/null 2>&1; then
    echo "ERROR: DEPLOY_SSH_PASSWORD is set but sshpass not installed." >&2
    echo "       brew install hudochenkov/sshpass/sshpass" >&2
    echo "       Or use SSH key: ssh-copy-id ${DEPLOY_SERVER}" >&2
    exit 1
  fi

  echo "    Auth: $(auth_mode)"
  echo "    Server: ${DEPLOY_SERVER}"
  echo "    Remote: ${DEPLOY_REMOTE_DIR}"

  with_retry "SSH connectivity" run_ssh "echo ok" >/dev/null

  run_ssh "mkdir -p '${DEPLOY_REMOTE_DIR}/dist_next'"

  with_retry "Remote rsync availability" run_ssh \
    "command -v rsync >/dev/null 2>&1 || (apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y rsync)" \
    >/dev/null

  echo "    Preflight OK"
}

build_release() {
  if [[ "$SKIP_BUILD" == true ]]; then
    echo "==> Skipping build (--skip-build)"
    [[ -f "${ROOT_DIR}/release/app.jar" ]] || [[ "$MODE" == frontend ]] || {
      echo "ERROR: release/app.jar missing; run without --skip-build" >&2
      exit 1
    }
    [[ -d "${ROOT_DIR}/release/dist" ]] || [[ "$MODE" == backend ]] || {
      echo "ERROR: release/dist missing; run without --skip-build" >&2
      exit 1
    }
    return
  fi

  case "$MODE" in
    frontend) "${ROOT_DIR}/scripts/build-release.sh" --skip-backend ;;
    backend) "${ROOT_DIR}/scripts/build-release.sh" --skip-frontend ;;
    all) "${ROOT_DIR}/scripts/build-release.sh" ;;
  esac
}

ensure_remote_update_script() {
  with_retry "Upload update.sh" run_rsync \
    "${ROOT_DIR}/scripts/remote-update.sh" \
    "${DEPLOY_SERVER}:${DEPLOY_REMOTE_DIR}/update.sh"
  run_ssh "chmod +x '${DEPLOY_REMOTE_DIR}/update.sh'"
}

post_deploy_verify() {
  echo "==> Post-deploy verify"
  if ! with_retry "Backend health check" run_ssh bash -s <<EOF
set -euo pipefail
for _ in \$(seq 1 90); do
  code=\$(curl -sS -o /dev/null -w '%{http_code}' \\
    -X POST http://127.0.0.1:8080/api/v1/telemetry/access \\
    -H 'Content-Type: application/json' \\
    -d '{"clientId":"deploy-verify","deviceType":"DESKTOP","pagePath":"/"}' || true)
  if [[ "\$code" == "200" ]]; then
    echo "telemetry ok"
    exit 0
  fi
  sleep 1
done
echo "backend not ready (last HTTP \$code)" >&2
exit 1
EOF
  then
    echo "WARN: Health check failed — deploy files may be OK; check: journalctl -u fridgeclear -n 80" >&2
    return 1
  fi
  echo "    Verify OK"
}

preflight
build_release
ensure_remote_update_script

if [[ "$MODE" != backend ]]; then
  echo "==> Uploading frontend to dist_next/"
  with_retry "Frontend rsync" run_rsync \
    "${ROOT_DIR}/release/dist/" \
    "${DEPLOY_SERVER}:${DEPLOY_REMOTE_DIR}/dist_next/" \
    --delete
fi

if [[ "$MODE" != frontend ]]; then
  echo "==> Uploading app.jar.new"
  with_retry "Backend rsync" run_rsync \
    "${ROOT_DIR}/release/app.jar" \
    "${DEPLOY_SERVER}:${DEPLOY_REMOTE_DIR}/app.jar.new"
fi

echo "==> Applying update on server"
if [[ "$MODE" == frontend ]]; then
  with_retry "Frontend swap" run_ssh \
    "cd '${DEPLOY_REMOTE_DIR}' && rm -rf dist_prev && (mv dist dist_prev 2>/dev/null || true) && mv dist_next dist && rm -rf dist_prev && echo 'frontend ok'"
else
  with_retry "Remote update" run_ssh "'${DEPLOY_REMOTE_DIR}/update.sh'"
fi

if [[ "$MODE" != frontend ]]; then
  post_deploy_verify || true
fi

echo "==> Deploy (${MODE}) finished."
