# Backend — 지능형 공정 재조정 관제 시스템 (PPRS)

반도체 공정을 실시간으로 모니터링하고, 지연 위험이 감지되면 AI 에이전트가 재조정안을 제시하는 **사전(proactive) 대응 관제 시스템**의 백엔드 서버입니다.

단순 조회 대시보드를 넘어, ① 공정 큐의 지연 위험을 감지·그룹핑하고 ② 외부 AI 에이전트 서비스를 호출해 관점별 재조정안을 생성하며 ③ 운영자가 검토·승인한 전략을 실제 큐·스케줄에 반영하는 역할을 담당합니다.

---

## 핵심 개념 — 사전 대응 흐름

```
[AI 모델] 공정 큐 unit별 지연 추정(delay_probability) → 위험 등급(risk_level)
        │  (risk_level 이 High/Critical 인 위험 발생)
        ▼
[백엔드] 위험 그룹핑 — (구역, 공정 단계) 단위로 묶고, 현재 큐에 있는(actionable) 위험만 채택
        ▼
[AI 에이전트] /run 호출 → 위험 원인 분석 + 관점별 재조정안 + 전·후 비교 보고 생성
        ▼
[운영자] 검토 화면에서 전략 선택·승인
        ▼
[백엔드] 큐 순서(queue position) · 스케줄(시작시각·설비) 반영
```

- **트리거 기준**: 그룹 대표 위험 중 하나라도 `risk_level` 이 **High/Critical** 이면 재조정 대상(수치 임계값 아님).
- **재조정안**: 납기 우선 / 병목 최소화 / 가동률 균형 등 관점별 전략 + 시뮬레이션 지표.
- **상태머신**: 재조정 그룹 `pending → approved`(운영자 승인) / `expired`(1시간 미처리 자동 만료).

---

## 주요 기능

### 인증 · 인가
- 로그인 / 회원가입, JWT Access Token 발급 (`/api/auth`)
- Authorization 헤더 기반 인증, 토큰 만료/위변조 예외 처리
- 역할(Role) 기반 접근 제어 + **구역(District) 단위 데이터 접근 제어** (`/api/users`)

### 모니터링 · 대시보드 (`/api/monitoring`)
- 전체 현황 원자적 스냅샷(overview), 구역 상태 요약
- 공정 큐(step별), 스케줄 간트(gantt), 장비 현황·통계
- 금일 생산량 집계(최종 공정 기준), 생산완료 현황
- 위험도/작업상태 조회

### 주문 · 큐 (`/api/orders`, `/api/queues`)
- 주문 목록/상세(공정 타임라인), 납기 임박 집계
- 현재 공정 큐 현황 (읽기 전용 — 큐 변경은 재조정 반영으로만 발생)

### 재조정 (`/api/reschedule`)
- 위험 그룹핑/탐지, 오케스트레이션(`/run`)
- 재조정 그룹 목록/상세/이력 조회
- 전략 선택·승인 → 큐·스케줄 반영, 수동 재생성

### 챗봇 (`/api/chatbot`)
- 재조정 그룹 컨텍스트 기반 질의응답(외부 AI `/infer` 연동), 세션·대화 이력 관리

### 공통
- 일관된 API 응답 구조(`ApiResponse`), 전역 예외 처리, Swagger 문서 제공

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Database | PostgreSQL (cluster: 16) |
| ORM | Spring Data JPA / Hibernate (`ddl-auto: validate`) |
| Auth | Spring Security + JWT (jjwt 0.12.6) |
| API Docs | springdoc-openapi (Swagger UI) 2.6.0 |
| Build Tool | Gradle |
| AI 연동 | RestClient → 외부 AI 에이전트 서비스 |

---

## 외부 연동 — AI 에이전트 서비스

같은 네임스페이스의 `skala-chip-ai` 서비스와 REST로 연동합니다 (`ai.base-url`, 기본 `http://skala-chip-ai:8000`).

| 엔드포인트 | 용도 |
|---|---|
| `POST /predict` | 스냅샷 시각 기준 지연 위험 추정 → `tt_delay_risk` 적재 |
| `POST /run` | 위험 분석 + 재조정안 + 전·후 비교 보고(통합 패키지) 생성 |
| `POST /infer` | 챗봇 질의 추론 |
| `POST /sim/*` | 시뮬레이션 제어(start/stop/restart/status 등) |

> `/run`·`/infer` 는 LLM 호출이라 read-timeout 을 120초로 길게 둡니다. AI 호출은 트랜잭션 밖에서 수행하고 결과 저장만 짧은 트랜잭션으로 처리합니다.

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/skala_chip` | DB 접속 URL |
| `DB_USERNAME` | `admin` | DB 사용자 |
| `DB_PASSWORD` | `admin1234` | DB 비밀번호 |
| `JPA_DDL_AUTO` | `validate` | 스키마는 DB/AI 측이 소유 → 검증만 |
| `SHOW_SQL` | `false` | 디버깅 시 `true` |
| `AI_BASE_URL` | `http://skala-chip-ai:8000` | AI 에이전트 서비스 주소 |
| `AI_CONNECT_TIMEOUT_MS` | `3000` | AI 연결 타임아웃 |
| `AI_READ_TIMEOUT_MS` | `120000` | AI 응답 타임아웃(LLM) |
| `JWT_SECRET` | (dev 기본값) | **운영에선 반드시 주입** (HS256, 32바이트+) |
| `JWT` 만료 | `86400000` (24시간) | Access Token 만료(ms) |

> ⚠️ **계정 주의**: 앱 기본값은 `admin/admin1234`, 아래 `docker-compose.yml` 기본값은 `skala/skala` 로 **서로 다릅니다.** docker-compose 로 DB 를 띄웠다면 앱 실행 시 `DB_USERNAME=skala`, `DB_PASSWORD=skala` 를 함께 설정하거나, 두 기본값을 한쪽으로 통일하세요.

---

## 실행 방법

### 1) 로컬 — Docker 로 DB + 앱 실행
```bash
# DB 기동 (docker-compose.yml 기본값: skala/skala)
docker compose up -d postgres

# 앱 실행 (DB 계정을 docker-compose 기본값에 맞춤)
export DB_USERNAME=skala
export DB_PASSWORD=skala
./gradlew bootRun
```

### 2) Swagger
```
http://localhost:8080/swagger-ui/index.html
```

---

## 팀 공용 PostgreSQL 서버 구성

팀원이 같은 DB 를 보게 하려면 한 명의 PC 가 아니라 고정 IP 서버 또는 클라우드 DB 에 PostgreSQL 을 올리는 것이 안정적입니다.

### 1. 서버에서 PostgreSQL 실행
`docker-compose.yml` 위치에서 `.env` 작성:
```bash
POSTGRES_DB=skala_chip
POSTGRES_USER=skala
POSTGRES_PASSWORD=강한_비밀번호로_변경
```
```bash
docker compose up -d postgres
```

### 2. 방화벽
팀원 IP 에서만 `5432` 허용 (전체 공개 금지):
```bash
sudo ufw allow from 팀원_IP to any port 5432 proto tcp
```

### 3. 팀원 로컬 앱 접속 설정
```bash
export DB_URL=jdbc:postgresql://서버_IP:5432/skala_chip
export DB_USERNAME=skala
export DB_PASSWORD=서버_DB_비밀번호
```
IntelliJ 는 Run Configuration → Environment variables 에 동일하게 입력.

### 4. 운영 권장
- 기본 비밀번호(`skala`) 사용 금지.
- 가능하면 AWS RDS / Supabase / Neon 등 관리형 PostgreSQL 사용.
- 직접 운영 시 `5432` 는 팀원 IP 또는 VPN 에서만 접근 허용.
- 서버 DB 환경에서는 `JWT_SECRET` 도 환경변수로 주입.

---

## 프로젝트 구조

```bash
backend
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.skala.chip
│   │   │       ├── auth          # 로그인 · JWT 발급/검증
│   │   │       ├── user          # 사용자 · 역할 · 구역 권한(DistrictAccessGuard)
│   │   │       ├── monitoring    # 대시보드 · 공정/설비/큐/스케줄 조회, SimClock
│   │   │       ├── order         # 주문 조회
│   │   │       ├── queue         # 공정 큐 조회
│   │   │       ├── reschedule    # 위험 그룹핑 · AI 오케스트레이션 · 재조정 · 스케줄러
│   │   │       ├── chatbot       # 재조정 컨텍스트 챗봇(AI /infer 연동)
│   │   │       ├── config        # Security · Swagger · 스케줄러 · AI 클라이언트 설정
│   │   │       ├── common        # 공통 응답(ApiResponse)
│   │   │       └── exception     # 전역 예외 · 에러 코드
│   │   └── resources
│   │       └── application.yml
├── docker-compose.yml
├── build.gradle
└── README.md
```

---

## 백그라운드 스케줄러

| 스케줄러 | 주기 | 역할 |
|---|---|---|
| 재조정 트리거 | `fixedDelay 60s` | 최신 위험 그룹핑 → 신규 High/Critical 그룹 재조정안 자동 생성 |
| 그룹 만료 | `fixedRate 5m` | 1시간 미처리 `pending` 그룹 자동 `expired` 처리 |
| 시뮬 속도 자동 조절 | `fixedDelay 5s` | 위험 상황에 따라 시뮬레이션 realtime↔fast 자동 전환 |

> 스케줄러 전용 스레드풀(3) 로 작업 간 간섭을 막습니다.
