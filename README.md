# 과제 A - 수강 신청 시스템

## 프로젝트 개요
본 프로젝트는 프로덕트 엔지니어 채용 과제 중 BE-A. 수강 신청 시스템을 구현한 백엔드 애플리케이션입니다.

크리에이터가 강의를 개설하고, 클래스메이트가 강의에 수강 신청한 뒤 결제 확정을 통해 수강을 확정하는 흐름을 제공합니다. 강의 상태, 수강 신청 상태, 정원 초과 방지, 동시성 제어와 같은 실무적인 비즈니스 규칙을 Spring Boot 기반 REST API로 구현합니다.

주요 목표는 다음과 같습니다.

- 강의 생성, 조회, 상태 변경을 포함한 강의 관리 기능 구현
- 수강 신청, 결제 확정, 취소를 포함한 신청 상태 전이 구현
- 강의 정원 초과 방지 및 동시 신청 상황에서의 데이터 정합성 보장
- 선택 구현(수강 취소 시 취소 가능 기간 제한 / 대기열 기능 / 강의별 수강생 목록 조회 / 신청 내역 페이지네이션)
- Docker Compose 기반의 일관된 로컬 실행 환경 제공
- 테스트 코드를 통한 핵심 비즈니스 규칙 검증

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5.14
- JPA/Hibernate
- Spring Data JPA
- MySQL 8.4
- Gradle

### Infra / Runtime

- Docker
- Docker Compose

### Test

- JUnit 5
- Spring Boot Test
- AssertJ
- MySQL 기반 통합 테스트 또는 Testcontainers 사용 예정

## 실행 방법

본 프로젝트는 Docker Compose를 통해 Spring Boot  애플리케이션과 MySQL을 함께 실행할 수 있도록 구성했습니다.
Docker Desktop을 사용하는 경우 별도 설치 없이 실행 가능합니다.

### 1. Repository clone

```bash
git clone https://github.com/devJin11/liveclass-assignment.git
cd liveclass-assignment
```

### 2. Docker Compose 실행
```bash
docker compose up --build
```

### 3. 애플리케이션 접속
```text
http://localhost:8080
```

### 4. MySQL 접속 정보
```text
Host: localhost
Port: 13306
Database: enrollment_db
Username: enrollment_user
Password: enrollment_password
Root Password: root_password
```

## 요구사항 해석 및 가정

