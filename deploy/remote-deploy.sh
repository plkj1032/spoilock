
#!/usr/bin/env bash
set -Eeuo pipefail

# GitHub Actions가 전달한 S3 배포 경로
S3_SOURCE="${1:?S3 배포 경로가 필요합니다.}"

BACKEND_DIR="/home/ec2-user/spoilock/backend"
FRONTEND_DIR="/var/www/spoilock"

# 임시 작업 디렉터리
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

echo "================================="
echo "SPOILOCK 자동 배포 시작"
echo "================================="

# 필수 실행 환경 확인
command -v aws >/dev/null || {
    echo "AWS CLI가 설치되지 않았습니다."
    exit 1
}

test -f "$BACKEND_DIR/app.jar"
test -f "/home/ec2-user/spoilock/.env"

# 1. S3에서 최신 빌드 파일 다운로드
echo "[1/5] S3 파일 다운로드"

aws s3 cp \
    "$S3_SOURCE/app.jar" \
    "$WORK_DIR/app.jar"

aws s3 cp \
    "$S3_SOURCE/frontend.tar.gz" \
    "$WORK_DIR/frontend.tar.gz"

test -s "$WORK_DIR/app.jar"
test -s "$WORK_DIR/frontend.tar.gz"

mkdir -p "$WORK_DIR/frontend"

tar -xzf "$WORK_DIR/frontend.tar.gz" \
    -C "$WORK_DIR/frontend"

test -f "$WORK_DIR/frontend/index.html"

# 2. 기존 JAR 백업
echo "[2/5] 기존 Spring Boot 백업"

cp -p \
    "$BACKEND_DIR/app.jar" \
    "$BACKEND_DIR/app-previous.jar"

# 3. Spring Boot 교체
echo "[3/5] Spring Boot 배포"

systemctl stop spoilock

install -o ec2-user -g ec2-user -m 0644 \
    "$WORK_DIR/app.jar" \
    "$BACKEND_DIR/app.jar"

# 실패 시 기존 JAR 복원
rollback() {
    echo "Spring Boot 배포 실패 - 이전 JAR 복원"

    systemctl stop spoilock || true

    cp -p \
        "$BACKEND_DIR/app-previous.jar" \
        "$BACKEND_DIR/app.jar"

    systemctl start spoilock || true
}

if ! systemctl start spoilock; then
    rollback
    exit 1
fi

# API 준비 상태 확인
READY=false

for i in $(seq 1 40); do
    if curl --fail --silent --max-time 3 \
        http://127.0.0.1:8080/api/dramas \
        >/dev/null; then
        READY=true
        break
    fi

    sleep 3
done

if [ "$READY" != "true" ]; then
    rollback
    exit 1
fi

# 4. React 정적 파일 배포
echo "[4/5] React 배포"

mkdir -p "$FRONTEND_DIR"

cp -a \
    "$WORK_DIR/frontend/." \
    "$FRONTEND_DIR/"

nginx -t
systemctl reload nginx

# 5. 최종 확인
echo "[5/5] 배포 결과 확인"

curl --fail --silent \
    http://127.0.0.1/api/dramas \
    >/dev/null

echo "================================="
echo "SPOILOCK 배포 성공!"
echo "================================="

