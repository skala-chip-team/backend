# Backend

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
│   └── test
├── build.gradle
└── README.md
