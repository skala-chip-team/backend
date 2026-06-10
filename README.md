# Backend readme

사내 공정 대시보드 백엔드 서버입니다.

공정 현황, 설비 상태, 작업 이력, 사용자 인증 및 권한 관리를 위한 API를 제공합니다.  
사내 사용자가 공정 데이터를 안정적으로 조회하고, 권한에 따라 필요한 기능에 접근할 수 있도록 백엔드 로직을 구성합니다.

---

## 프로젝트 개요

본 프로젝트는 사내 공정 관리 및 모니터링을 위한 대시보드 시스템의 백엔드입니다.

주요 목적은 다음과 같습니다.

- 공정 데이터 조회 및 관리
- 설비 상태 모니터링
- 작업 이력 관리
- JWT 기반 사용자 인증
- 사용자 권한(Role) 기반 접근 제어
- 일관된 API 응답 구조 제공

---

## 주요 기능

### 인증 및 인가

- 로그인 API
- JWT Access Token 발급
- Authorization Header 기반 인증 처리
- 사용자 권한(Role) 기반 접근 제어
- 인증 실패 및 토큰 만료 예외 처리

### 공정 관리

- 공정 현황 조회
- 공정 상세 정보 조회
- 공정 진행 상태 관리
- 공정별 작업 이력 조회

### 설비 관리

- 설비 상태 조회
- 설비별 가동 상태 관리
- 설비 이상 상태 확인

### 대시보드 데이터

- 공정 현황 요약 데이터 제공
- 설비 상태 요약 데이터 제공
- 작업 진행률 및 상태 정보 제공

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| Database | Postgre |
| Auth | JWT |
| ORM | JPA |
| API Docs | Swagger |
| Build Tool | Gradle |

---

## 팀 공용 PostgreSQL 서버 구성

팀원이 같은 DB를 보게 하려면 한 명의 PC가 아니라 고정 IP를 가진 서버 또는 클라우드 DB에 PostgreSQL을 올리는 방식이 안정적입니다.

### 1. 서버에서 PostgreSQL 실행

서버에 이 저장소를 받은 뒤, `docker-compose.yml`이 있는 위치에서 `.env` 파일을 만듭니다.

```bash
POSTGRES_DB=skala_chip
POSTGRES_USER=skala
POSTGRES_PASSWORD=강한_비밀번호로_변경
```

현재 `docker-compose.yml`은 기본값으로 `5432` 포트를 외부에 공개합니다.

```bash
docker compose up -d postgres
```

### 2. 서버 방화벽 열기

팀원 IP에서만 `5432` 포트 접근을 허용하세요. 모든 인터넷에 DB 포트를 공개하는 것은 피해야 합니다.

예시:

```bash
# Ubuntu ufw 예시
sudo ufw allow from 팀원_IP to any port 5432 proto tcp
```

### 3. 팀원 로컬 앱 접속 설정

팀원은 각자 로컬에서 아래 환경변수를 설정한 뒤 백엔드를 실행합니다.

```bash
export DB_URL=jdbc:postgresql://서버_IP:5432/skala_chip
export DB_USERNAME=skala
export DB_PASSWORD=서버_DB_비밀번호
```

IntelliJ에서는 Run Configuration의 Environment variables에 같은 값을 넣으면 됩니다.

### 4. 운영 시 권장 사항

- DB 비밀번호는 `skala` 같은 기본값을 사용하지 마세요.
- 가능하면 AWS RDS, Supabase, Neon 같은 관리형 PostgreSQL을 쓰는 것이 가장 안전합니다.
- 직접 서버에 Docker로 올린다면 `5432`는 팀원 IP 또는 VPN에서만 접근 가능하게 제한하세요.
- 서버 DB를 쓰는 환경에서는 `JWT_SECRET`도 환경변수로 주입하세요.

---

## 프로젝트 구조(미정) - 그러나 해당 구조를 기반으로

```bash
backend
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.example.backend
│   │   │       ├── controller
│   │   │       ├── service
│   │   │       ├── repository
│   │   │       ├── domain
│   │   │       ├── dto
│   │   │       ├── config
│   │   │       ├── security
│   │   │       └── exception
│   │   └── resources
│   │       ├── application.yml
│   │       └── application-local.yml
├── build.gradle
└── README.md
