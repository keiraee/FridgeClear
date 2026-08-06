#!/usr/bin/env bash
# Build a deployable release folder for 1Panel / bare-metal hosting.
# Usage: ./scripts/build-release.sh [--skip-backend] [--skip-frontend] [--archive] [--no-proxy]
set -euo pipefail

# Maven / npm download proxy (override via env or use --no-proxy to disable)
PROXY_HTTP="${PROXY_HTTP:-http://127.0.0.1:7897}"
PROXY_ALL="${PROXY_ALL:-socks5://127.0.0.1:7897}"
ENABLE_PROXY=true

apply_proxy() {
  if [[ "$ENABLE_PROXY" != true ]]; then
    return
  fi
  export http_proxy="$PROXY_HTTP" https_proxy="$PROXY_HTTP"
  export HTTP_PROXY="$PROXY_HTTP" HTTPS_PROXY="$PROXY_HTTP"
  export all_proxy="$PROXY_ALL" ALL_PROXY="$PROXY_ALL"
  echo "    Proxy: $PROXY_HTTP"
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_DIR="${ROOT_DIR}/release"
JAR_NAME="FridgeClear-0.0.1-SNAPSHOT.jar"
SKIP_BACKEND=false
SKIP_FRONTEND=false
CREATE_ARCHIVE=false

for arg in "$@"; do
  case "$arg" in
    --skip-backend) SKIP_BACKEND=true ;;
    --skip-frontend) SKIP_FRONTEND=true ;;
    --archive) CREATE_ARCHIVE=true ;;
    --no-proxy) ENABLE_PROXY=false ;;
    -h|--help)
      cat <<'EOF'
Usage: ./scripts/build-release.sh [options]

Options:
  --skip-backend   Reuse existing target/*.jar
  --skip-frontend  Reuse existing web/dist
  --archive        Also create release/fridgeclear-release.tar.gz
  --no-proxy       Do not set http(s)_proxy for Maven/npm
  -h, --help       Show this help

Proxy (when enabled, default on):
  PROXY_HTTP=http://127.0.0.1:7897 PROXY_ALL=socks5://127.0.0.1:7897

Output:
  release/
    app.jar
    .env
    dist/
    data/source/HowToCook/   (if present locally)
EOF
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      exit 1
      ;;
  esac
done

cd "$ROOT_DIR"

echo "==> FridgeClear release build"
echo "    Root: $ROOT_DIR"
apply_proxy

if [[ "$SKIP_BACKEND" == false ]]; then
  echo "==> Building backend (Maven)"
  chmod +x ./mvnw
  ./mvnw -DskipTests package
else
  echo "==> Skipping backend build"
fi

JAR_PATH="${ROOT_DIR}/target/${JAR_NAME}"
if [[ ! -f "$JAR_PATH" ]]; then
  echo "ERROR: JAR not found: $JAR_PATH" >&2
  exit 1
fi

if [[ "$SKIP_FRONTEND" == false ]]; then
  echo "==> Building frontend (Vite)"
  pushd web >/dev/null
  if [[ -f package-lock.json ]]; then
    npm ci
  else
    npm install
  fi
  npm run build
  popd >/dev/null
else
  echo "==> Skipping frontend build"
fi

if [[ ! -f "${ROOT_DIR}/web/dist/index.html" ]]; then
  echo "ERROR: Frontend build missing: web/dist/index.html" >&2
  exit 1
fi

echo "==> Assembling release directory"
rm -rf "$RELEASE_DIR"
mkdir -p "${RELEASE_DIR}/dist"

cp "$JAR_PATH" "${RELEASE_DIR}/app.jar"
cp -R "${ROOT_DIR}/web/dist/." "${RELEASE_DIR}/dist/"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  cp "${ROOT_DIR}/.env" "${RELEASE_DIR}/.env"
  echo "    Copied .env"
else
  cp "${ROOT_DIR}/.env.example" "${RELEASE_DIR}/.env"
  echo "WARN: .env not found; copied .env.example — edit release/.env before deploy" >&2
fi

HOWTOCOOK_SRC="${ROOT_DIR}/data/source/HowToCook"
if [[ -d "$HOWTOCOOK_SRC" ]]; then
  mkdir -p "${RELEASE_DIR}/data/source"
  cp -R "$HOWTOCOOK_SRC" "${RELEASE_DIR}/data/source/"
  echo "    Copied HowToCook data"
else
  echo "WARN: ${HOWTOCOOK_SRC} not found — recipe images will be missing until you add dishes/" >&2
fi

cat > "${RELEASE_DIR}/DEPLOY.txt" <<'EOF'
FridgeClear release package

Upload this entire folder to the server, e.g. /opt/fridgeclear/

Run backend:
  cd /opt/fridgeclear
  java -jar app.jar

Or use systemd with WorkingDirectory=/opt/fridgeclear

Website (1Panel OpenResty):
  - Static root: /opt/fridgeclear/dist
  - Reverse proxy /api/ -> http://127.0.0.1:8080/api/
  - Required proxy headers (otherwise IP logs show 127.0.0.1):
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
  - proxy_read_timeout 300s (for AI meal plan)

Ensure RDS/security group allows the server IP if using cloud MySQL.
EOF

if [[ "$CREATE_ARCHIVE" == true ]]; then
  ARCHIVE_PATH="${RELEASE_DIR}/fridgeclear-release.tar.gz"
  echo "==> Creating archive"
  tar -czf "$ARCHIVE_PATH" -C "$RELEASE_DIR" \
    --exclude='fridgeclear-release.tar.gz' \
    app.jar .env dist data DEPLOY.txt 2>/dev/null || \
  tar -czf "$ARCHIVE_PATH" -C "$RELEASE_DIR" \
    --exclude='fridgeclear-release.tar.gz' \
    app.jar .env dist DEPLOY.txt
  echo "    Archive: $ARCHIVE_PATH"
fi

echo
echo "Done. Release ready at: $RELEASE_DIR"
echo "  app.jar"
echo "  .env"
echo "  dist/"
if [[ -d "${RELEASE_DIR}/data" ]]; then
  echo "  data/source/HowToCook/"
fi
echo
echo "Upload to server, then: java -jar app.jar  (from that directory)"
