# Team11 Backend Ground Rules 📋

> 팀원 모두가 일관되고 효율적으로 개발할 수 있도록 하는 기본 규칙들입니다.

## 목차

- [📝 요구사항 명세서 작성 규칙](#-요구사항-명세서-작성-규칙)
- [💻 Git 컨벤션](#-git-컨벤션)
- [🔧 개발자 초기 설정](#-개발자-초기-설정)

---

## 📝 요구사항 명세서 작성 규칙

### 언제 작성하나요?

**새로운 기능 개발 전 반드시 작성**해야 합니다.

#### ✅ 작성이 필요한 경우

- 새로운 API 엔드포인트 추가
- 새로운 화면/페이지 추가
- 기존 기능의 주요 변경 (비즈니스 로직 수정)
- 외부 시스템 연동
- 데이터베이스 스키마 변경

#### ❌ 작성하지 않아도 되는 경우

- 단순 버그 수정
- 코드 리팩토링 (기능 변경 없음)
- 스타일/UI 미세 조정
- 문서 업데이트

### 작성 워크플로우

1. 기능요구사항 작성 (담당 개발자)
2. 팀 리뷰 및 피드백 (전체 팀원)
3. 요구사항 승인 (팀장/시니어)
4. 개발 시작
5. 완료 후 요구사항 대비 검수


### 파일 관리 규칙

```
docs/
├── requirements/
│   ├── [기능명]_requirements.md
│   └── ...
├── REQUIREMENTS_TEMPLATE.md
└── ...

예시:
- docs/requirements/user_auth_requirements.md
- docs/requirements/location_search_requirements.md
- docs/requirements/realtime_chat_requirements.md
```

---

## 💻 Git 컨벤션

### 브랜치 전략

```
main (production)
│
├── develop (integration)
│   ├── feature/[기능명] (개발)
│   └── hotfix/[수정사항] (긴급 수정)
└── release/YYYY-MM-DD (주간 스냅샷)
```

### 커밋 메시지 규칙

**Conventional Commits** 형식을 따릅니다.

```
<타입>(<범위>): <설명>

[선택적 본문]

[꼬릿말]
```

#### 타입 (Type)

- `feat`: 새로운 기능
- `fix`: 버그 수정  
- `docs`: 문서 변경
- `style`: 코드 formatting (기능 변경 없음)
- `refactor`: 코드 리팩토링
- `test`: 테스트 추가/수정
- `chore`: 빌드/패키지 매니저 설정 등

#### 예시

```bash
feat(auth): add OAuth2 login functionality
fix(api): resolve null pointer in user service
docs: update API documentation
refactor(user): simplify validation logic
```

#### 꼬릿말

```
Close #123
```

## 🔄 전체 워크플로우

### 📅 주간 개발 사이클

```
1. 배정받은 기능 개발 (개별 조원)
   ↓
2. 주차별 develop 브랜치에 PR 머지
   ↓
3. 프로젝트 진척 미팅 (오류 해결 및 보고서 작성)
   ↓
4. 릴리스 스냅샷 브랜치 생성 (release/YYYY-MM-DD)
   ↓
5. 스냅샷 → main PR 생성
   ↓
6. 현업 멘토님 코드 피드백
   ↓
7. 피드백 반영 및 상호작용
   ↓
8. 다음 주차 준비
```

### 👥 역할별 워크플로우

#### 🔧 조원 (Feature 개발)

```bash
# 1. develop에서 feature 브랜치 생성 및 개발
git pull origin develop
git checkout -b feature/user-authentication
# 개발 진행...
git push -u origin feature/user-authentication

# 2. develop으로 PR 생성 및 머지
# GitHub에서 feature/user-authentication → develop PR 생성

# 3. 최신 변경사항 받기
git pull origin develop
```

#### 👑 테크리더 (Release 관리)

```bash
# 1. develop 브랜치 생성 (최초 1번만)
git checkout main
git checkout -b develop
git push -u origin develop

# 2. 주간 릴리스 스냅샷 브랜치 생성
git fetch origin
git checkout origin/develop                 # 멘토 피드백 기준 시점
git checkout -b release/2025-01-20          # 스냅샷 브랜치
git push -u origin release/2025-01-20

# 3. release → main PR 생성 (GitHub에서)
# 멘토님 코드리뷰 진행, main 머지 대기

# 4. main 머지 이후 태깅
git checkout main
git pull origin main
git tag weekly-2025-01-20
git push origin main --tags
```

### 💬 멘토 피드백 프로세스

#### 1. 피드백 받기

- **GitHub PR**에서 멘토님 코드 피드백 수신
- **각 코멘트**를 꼼꼼히 읽고 이해

#### 2. 상호작용하기

- **피드백 코멘트**에 이해한 내용 답글 작성
- **질문사항**이 있으면 적극적으로 문의
- **개선 방향** 토론 및 합의

#### 3. 피드백 반영하기

```bash
# 1. local/remote 버전 차이 해결
git fetch origin
git pull origin develop

# 2. develop에서 피드백 반영 브랜치 생성
git checkout develop
git checkout -b feedback/mentor-suggestions-0120
# 피드백 내용 반영...
git push -u origin feedback/mentor-suggestions-0120

# 3. develop으로 PR 생성 및 머지
# GitHub에서 feedback/mentor-suggestions-0120 → develop PR 생성
```

#### 4. 피드백 완료 보고

- **원 피드백 코멘트**에 반영 내용 댓글 작성
- **PR 링크**와 함께 수정 사항 요약
- **"반영 완료"** 상태 표시

### 📋 Pull Request 규칙

- **PR 생성 전** 요구사항 명세서 작성 완료
- **최소 1명 이상** 코드 리뷰 필수
- **테스트 통과** 후 머지
- **충돌 해결** 후 머지

---

## 🔧 개발자 초기 설정

### 1. 개발 환경 요구사항

#### 필수 설치 프로그램

```bash
- Java 21 (OpenJDK 또는 Oracle JDK)
- Git 2.40+
- IDE (IntelliJ IDEA 권장)
```

### 2. 프로젝트 클론 및 설정

```bash
# 1. 저장소 클론
git clone https://github.com/[organization]/Team11_BE.git
cd Team11_BE

# 2. 브랜치 전략 설정
git checkout develop
git checkout -b feature/[기능명]

# 3. 커밋 템플릿 설정
git config commit.template .gitmessage
```

### 3. 로컬 개발 환경 구성

#### 환경변수 설정

```bash
# .env.local 파일 생성 (예시)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=team11_dev
DB_USER=team11
DB_PASS=password

JWT_SECRET=your-secret-key
```

#### 데이터베이스 설정
1. PostgreSQL 설치 및 실행(17.5+ 권장)

2. 데이터베이스 및 사용자 생성

```sql
CREATE DATABASE team11_dev;
CREATE USER team11 WITH ENCRYPTED PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE team11_dev TO team11;
```

3. PostGIS 확장 설치 (위치 기반 기능 사용 시)

```sql
CREATE EXTENSION postgis;
```

4. 시군구 영역 데이터 import

```bash
psql -U [username] -d team11_dev -f sql/sigungu_schema.sql
pgdump -U [username] -d team11_dev -f sql/sigungu_data.sql
```

### 4. 개발 도구 설정

#### IDE 설정 (IntelliJ IDEA)

1. **Code Style 설정**
   - File → Settings → Editor → Code Style → Java
   - 프로젝트의 `.editorconfig` 파일 적용

2. **Live Template 설정**
   - 자주 사용하는 코드 스니펫 등록
   - Spring Boot 관련 템플릿 추가

3. **Database 연결**
   - Database Navigator로 로컬 PostgreSQL 연결