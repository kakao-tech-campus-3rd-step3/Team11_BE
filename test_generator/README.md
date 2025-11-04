# momeet-test-maker

테스트 데이터 생성기 프로젝트입니다.

## 시작하기

### 1. 의존성 설치

프로젝트 루트 디렉토리에서 다음 명령어를 실행하여 의존성을 설치합니다:

```bash
uv sync
```

### 2. 환경 변수 설정

`.env-example` 파일을 참고하여 `.env` 파일을 생성하고 환경 변수를 설정합니다:

```bash
cp .env-example .env
```

`.env` 파일을 열어 다음 값들을 설정하세요:

- `API_SERVER_URL`: API 서버 URL
- `ADMIN_EMAIL`: 관리자 이메일 (예: "admin@test.com")
- `ADMIN_PASSWORD`: 관리자 비밀번호 (예: "testpass1212!")

### 3. 실행

다음 명령어로 테스트 데이터 생성기를 실행합니다:

```bash
uv run main.py
```

이 명령어는 테스트 데이터를 생성하는 예제를 실행합니다.

## 프로젝트 구조

- `main.py`: 메인 실행 파일
- `generator/`: 테스트 데이터 생성 모듈
  - `member.py`: 회원 데이터 생성
  - `meetup.py`: 모임 데이터 생성

## 주의사항

- Python 3.13 이상이 필요합니다.
- 환경 변수 파일(`.env`)이 제대로 설정되어 있어야 합니다.

