#!/bin/bash

# SSL 인증서 생성 스크립트
# 개발 환경용 자체 서명 인증서와 프로덕션 환경용 Let's Encrypt 인증서를 지원

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 도메인명 확인
if [ -z "$1" ]; then
    log_error "도메인명을 입력해주세요."
    echo "사용법: $0 <domain> [environment]"
    echo "예시: $0 localhost dev"
    echo "예시: $0 momeet.example.com prod"
    exit 1
fi

DOMAIN=$1
ENVIRONMENT=${2:-dev}
SSL_DIR="./nginx/ssl"
CERTBOT_DIR="./nginx/certbot"

log_info "도메인: $DOMAIN"
log_info "환경: $ENVIRONMENT"

# SSL 디렉토리 생성
mkdir -p "$SSL_DIR"
mkdir -p "$CERTBOT_DIR"

if [ "$ENVIRONMENT" = "dev" ]; then
    log_info "개발 환경용 자체 서명 인증서를 생성합니다..."
    
    # 자체 서명 인증서 생성
    openssl req -x509 -newkey rsa:4096 -keyout "$SSL_DIR/key.pem" -out "$SSL_DIR/cert.pem" \
        -days 365 -nodes \
        -subj "/C=KR/ST=Seoul/L=Seoul/O=MoMeet/OU=Development/CN=$DOMAIN" \
        -addext "subjectAltName=DNS:$DOMAIN,DNS:localhost,IP:127.0.0.1,IP:0.0.0.0"
    
    log_success "자체 서명 인증서가 생성되었습니다."
    log_info "인증서 위치: $SSL_DIR/cert.pem"
    log_info "개인키 위치: $SSL_DIR/key.pem"
    
    # 인증서 정보 출력
    log_info "인증서 정보:"
    openssl x509 -in "$SSL_DIR/cert.pem" -text -noout | grep -E "(Subject:|Issuer:|Not Before|Not After)"
    
elif [ "$ENVIRONMENT" = "prod" ]; then
    log_info "프로덕션 환경용 Let's Encrypt 인증서를 생성합니다..."
    
    # Docker Compose가 실행 중인지 확인
    if ! docker compose ps | grep -q "nginx"; then
        log_warning "nginx 컨테이너가 실행 중이지 않습니다. Docker Compose를 먼저 실행해주세요."
        exit 1
    fi
    
    # certbot을 사용한 Let's Encrypt 인증서 생성
    docker compose exec nginx certbot certonly \
        --webroot \
        --webroot-path=/var/www/certbot \
        --email admin@$DOMAIN \
        --agree-tos \
        --no-eff-email \
        --domains $DOMAIN
    
    # 인증서를 nginx SSL 디렉토리로 복사
    docker compose exec nginx cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem /etc/nginx/ssl/cert.pem
    docker compose exec nginx cp /etc/letsencrypt/live/$DOMAIN/privkey.pem /etc/nginx/ssl/key.pem
    
    # 인증서 권한 설정
    docker compose exec nginx chmod 644 /etc/nginx/ssl/cert.pem
    docker compose exec nginx chmod 600 /etc/nginx/ssl/key.pem
    
    log_success "Let's Encrypt 인증서가 생성되었습니다."
    
    # 자동 갱신 설정
    log_info "자동 갱신을 위한 cron 작업을 설정합니다..."
    echo "0 12 * * * docker compose exec nginx certbot renew --quiet && docker compose exec nginx nginx -s reload" | crontab -
    
    log_success "자동 갱신이 설정되었습니다."
    
else
    log_error "지원하지 않는 환경입니다. 'dev' 또는 'prod'를 입력해주세요."
    exit 1
fi

# nginx 설정 파일 업데이트
if [ "$ENVIRONMENT" = "prod" ]; then
    # 프로덕션 환경에서는 nginx.conf 사용
    log_info "nginx 설정을 프로덕션 모드로 업데이트합니다..."
    sed -i "s/\${DOMAIN_NAME}/$DOMAIN/g" ./nginx/nginx.conf
    log_success "nginx 설정이 업데이트되었습니다."
fi

log_success "SSL 인증서 설정이 완료되었습니다!"
log_info "nginx를 재시작하여 설정을 적용하세요: docker compose restart nginx"
