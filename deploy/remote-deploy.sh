
#!/usr/bin/env bash
set -Eeuo pipefail

S3_SOURCE="${1:?S3 배포 경로가 필요합니다.}"

APP_DIR="/opt/spoilock/app"
APP_JAR="$APP_DIR/spoilock.jar"
BACKUP_JAR="$APP_DIR/spoilock-previous.jar"
ENV_FILE="/opt/spoilock/.env"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

echo "SPOILOCK 백엔드 배포 시작"

command -v aws >/dev/null
command -v curl >/dev/null

test -f "$APP_JAR"
test -f "$ENV_FILE"

# 신규 JAR 다운로드
aws s3 cp \
  "$S3_SOURCE/app.jar" \
  "$WORK_DIR/spoilock.jar"

test -s "$WORK_DIR/spoilock.jar"

# 이전 JAR 백업
cp -p "$APP_JAR" "$BACKUP_JAR"

rollback() {
  echo "배포 실패 - 이전 JAR 복원"
  systemctl stop spoilock || true
  cp -p "$BACKUP_JAR" "$APP_JAR"
  systemctl start spoilock || true
}

# 신규 버전 적용
systemctl stop spoilock

install -o ec2-user -g ec2-user -m 0644 \
  "$WORK_DIR/spoilock.jar" "$APP_JAR"

if ! systemctl start spoilock; then
  rollback
  exit 1
fi

# 기동 상태 확인 (최대 약 120초)
READY=false

for i in $(seq 1 40); do
  if curl --fail --silent --max-time 3 \
    http://127.0.0.1:8080/health \
    | grep -qx 'OK'; then
    READY=true
    break
  fi

  if ! systemctl is-active --quiet spoilock; then
    break
  fi

  sleep 3
done

if [ "$READY" != "true" ]; then
  rollback
  exit 1
fi

# ========================================
# React 프론트엔드 배포
# ========================================

FRONTEND_DIR="/var/www/spoilock"

echo "React 배포 시작"

mkdir -p "$WORK_DIR/frontend"

aws s3 cp \
  "$S3_SOURCE/frontend.tar.gz" \
  "$WORK_DIR/frontend.tar.gz"

tar -xzf "$WORK_DIR/frontend.tar.gz" \
  -C "$WORK_DIR/frontend"

test -f "$WORK_DIR/frontend/index.html"

mkdir -p "$FRONTEND_DIR"

cp -a "$WORK_DIR/frontend/." "$FRONTEND_DIR/"

chmod -R a+rX "$FRONTEND_DIR"

echo "React 배포 완료"

echo "SPOILOCK 백엔드 배포 성공!"
