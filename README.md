# 과제 A - 수강 신청 시스템

## 프로젝트 개요
본 프로젝트는 프로덕트 엔지니어 채용 과제 중 **BE-A. 수강 신청 시스템**을 구현한 백엔드 애플리케이션입니다.

크리에이터가 강의를 개설하고, 클래스메이트가 강의에 수강 신청한 뒤 결제 확정을 통해 수강을 확정하는 흐름을 제공합니다.

강의 상태 전이, 수강 신청 상태 전이, 정원 초과 방지, 동시성 제어와 같은 비즈니스 규칙을 Spring Boot 기반 REST API로 구현하는 것을 목표로 합니다.

주요 기능은 다음과 같습니다.

- 강의 생성, 수정, 모집 시작, 모집 마감
- 강의 목록 조회 및 상세 조회
- 강의 상태 전이 관리
    - `DRAFT`
    - `OPEN`
    - `CLOSED`
- 수강 신청 생성, 결제 확정, 수강 취소
- 수강 신청 상태 전이 관리
    - `PENDING`
    - `CONFIRMED`
    - `CANCELLED`
- 결제 대기 상태 자동 취소(스케줄러)
- 결제 확정 시점에 수강 확정 처리
- 강의 정원 초과 방지
- 동시 신청 상황에서 데이터 정합성 보장
- 강의별 수강생 목록 조회(크리에이터 전용)
- 내 수강 신청 목록 페이지네이션
- Redis 기반 대기열 관리(시간 남으면 구현 예정)
- Docker Compose 기반 실행 환경 제공
- 테스트 코드를 통한 핵심 비즈니스 규칙 검증


## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5.14
- Spring Data JPA
- JPA / Hibernate
- Bean Validation
- SpringDoc OpenAPI
- Flyway
- MySQL 8.4
- Gradle

### Infra / Runtime

- Docker
- Docker Compose

### Test

- JUnit 5
- Spring Boot Test
- Spring MVC Test / MockMvc
- Spring Data JPA Test
- AssertJ
- Mockito
- Testcontainers
- MySQL 8.4 기반 Repository / Integration / Concurrency Test

## 실행 방법

본 프로젝트는 Docker Compose를 통해 Spring Boot  애플리케이션과 MySQL을 함께 실행할 수 있도록 구성했습니다.
Docker Desktop을 사용하는 경우 별도 설치 없이 실행 가능합니다.

### 1. Repository clone

```bash
git clone https://github.com/devJin11/liveclass-assignment.git
cd liveclass-assignment
```

### 2. Docker Compose 기반 실행 환경

평가자가 별도의 로컬 Java, Gradle, MySQL 설치 없이 실행할 수 있도록 Docker Compose 기반 실행 환경을 제공합니다.

```bash
docker compose up --build
```

백그라운드에서 실행하려면 다음 명령어를 사용합니다.

```bash
docker compose up --build -d
```

### 3. 애플리케이션 접속
```text
http://localhost:8080
```

### 4. MySQL 접속 정보
Docker Compose 내부에서 Spring Boot 애플리케이션은 다음 주소로 MySQL에 연결합니다.
```text
jdbc:mysql://mysql:3306/enrollment_db
```

로컬 PC에서 DataGrip, DBeaver, MySQL Workbench 등으로 직접 접속할 경우에는 다음 정보를 사용합니다.
```text
Host: localhost
Port: 13306
Database: enrollment_db
Username: enrollment_user
Password: enrollment_password
Root Password: root_password
```

### 5. Swagger UI 접속 주소
```text
http://localhost:8080/swagger-ui/index.html
```

### 6. 종료
```bash
docker compose down
```

DB 데이터까지 함께 삭제하려면 다음 명령어를 사용합니다.

```bash
docker compose down -v
```

## 로컬 개발 환경

제출 및 실행 검증은 Docker Compose 기준으로 구성했습니다.

다만 로컬 개발 시에는 다음 방식으로 개발하였습니다.

- Spring Boot 애플리케이션은 IntelliJ에서 직접 실행
- MySQL 8.4는 Docker 컨테이너로 단독 실행
- Spring profile은 `local` 사용

로컬 개발용 설정 파일은 다음 파일을 사용합니다.

```text
src/main/resources/application-local.yml
```

해당 파일은 개인 로컬 환경 설정이므로 Git에 포함하지 않았습니다.

---

## 요구사항 해석 및 가정
본 프로젝트는 요구사항에서 명시된 강의 생성, 수강 신청, 결제 확정, 취소, 정원 관리 기능을 기반으로
실제 서비스에서 발생할 수 있는 상태 전이, 좌석 점유, 동시성 제어, 결제 대기 만료,
크리에이터 권한 범위, 대기열 정책을 고려하여 다음과 같이 요구사항을 해석했습니다.

---

### 1. 인증/인가

과제 요구사항에서 인증/인가는 간략히 처리 가능하다고 안내되어 있으므로, 본 프로젝트에서는 Spring Security 기반 로그인/인증을 구현하지 않습니다.

대신 API 요청 시 다음 식별자를 요청 바디 또는 요청 파라미터로 전달받아 권한을 단순 검증합니다.

- 크리에이터 권한 API: `creatorId`
- 클래스메이트 권한 API: `classmateId`

예를 들어 강의 수정, 강의 모집 시작/마감, 강의별 확정 수강생 목록 조회는 요청한 `creatorId`가 해당 강의의 크리에이터인지 검증합니다.

수강 신청 상세 조회, 결제 확정, 수강 취소는 요청한 `classmateId`가 해당 신청의 소유자인지 검증합니다.

**공통 API에서는 role 파라미터를 받아 검증합니다.** 

```text
role = CREATOR
role = CLASSMATE
```

---

### 2. Creator / Classmate 관리

본 과제의 핵심은 회원 관리가 아니라 강의와 수강 신청 흐름이므로, `creator`, `classmate`에 대한 CRUD API는 별도로 구현하지 않습니다.

다만 DB 정합성을 위해 `class_room`, `enrollment`에서 FK로 참조하므로 엔티티와 Repository는 유지합니다.

테스트 및 API 검증을 위해 Flyway seed SQL로 초기 데이터를 제공합니다.

---

### 3. 강의 생성

크리에이터는 강의를 생성할 수 있습니다.

강의 생성 시 다음 정보를 입력합니다.

- 강의 제목
- 강의 설명
- 가격
- 최대 수강 정원
- 수강 시작일
- 수강 종료일

강의는 최초 생성 시 항상 `DRAFT` 상태로 생성됩니다.

```text
최초 강의 상태 = DRAFT
```

`DRAFT` 상태는 초안 상태이므로 수강생은 해당 강의에 수강 신청할 수 없습니다.

---

### 4. 강의 상태

강의는 다음 상태를 가집니다.

| 상태 | 설명 | 수강 신청 가능 여부 |
|---|---|---|
| `DRAFT` | 초안 | 불가 |
| `OPEN` | 모집 중 | 가능 |
| `CLOSED` | 모집 마감 | 불가 |

허용되는 강의 상태 전이는 다음과 같습니다.

```text
DRAFT -> OPEN
OPEN -> CLOSED
```

허용하지 않는 상태 전이는 다음과 같습니다.

```text
DRAFT -> CLOSED
OPEN -> DRAFT
CLOSED -> OPEN
CLOSED -> DRAFT
```

크리에이터는 `DRAFT` 상태의 강의를 `OPEN` 상태로 변경하여 수강 신청을 받을 수 있습니다.

또한 정원이 모두 차지 않았더라도 크리에이터는 운영 판단에 따라 `OPEN` 상태의 강의를 `CLOSED` 상태로 변경하여 모집을 마감할 수 있습니다.

---

### 5. 강의 수정 정책

강의 수정은 `DRAFT` 상태에서만 허용합니다.

`OPEN` 또는 `CLOSED` 상태의 강의는 수정할 수 없습니다.

이렇게 설계한 이유는 다음과 같습니다.

- 이미 모집 중인 강의의 가격, 기간, 정원이 변경되면 기존 신청자에게 영향을 줄 수 있습니다.
- 모집 마감 이후 강의 정보가 변경되면 신청 당시의 정보와 실제 강의 정보 사이의 정합성이 깨질 수 있습니다.
- 본 과제에서는 변경 이력 관리까지 요구하지 않으므로, 데이터 정합성과 구현 명확성을 위해 `DRAFT` 상태에서만 수정 가능하도록 제한했습니다.

실무 서비스에서는 강의 소개글, 안내 문구, 썸네일 등 일부 비핵심 필드만 `OPEN` 상태에서도 수정 가능하게 설계할 수 있지만, 본 프로젝트에서는 핵심 요구사항 구현에 집중하기 위해 수정 가능 상태를 `DRAFT`로 제한했습니다.

---

### 6. 강의 목록 조회 정책

강의 목록 조회는 크리에이터와 수강생 모두 사용할 수 있는 공통 API로 제공합니다.

다만 역할에 따라 조회 가능한 강의 상태를 다르게 제한합니다.

#### 크리에이터

크리에이터는 본인이 개설한 강의를 관리해야 하므로 다음 상태를 모두 필터링하여 조회할 수 있습니다.

```text
DRAFT
OPEN
CLOSED
```

크리에이터의 강의 목록 조회는 본인이 생성한 강의를 기준으로 합니다.

```text
classRoom.creatorId == 요청 creatorId
```

따라서 크리에이터는 자신이 생성한 초안 강의, 모집 중 강의, 모집 마감 강의를 모두 조회할 수 있습니다.

#### 수강생

수강생은 공개 가능한 강의만 조회할 수 있습니다.

수강생이 필터링할 수 있는 상태는 다음과 같습니다.

```text
OPEN
CLOSED
```

`DRAFT` 상태는 아직 공개 전 초안이므로 수강생 목록 조회 대상에서 제외합니다.

수강생 관점에서 각 상태의 의미는 다음과 같습니다.

| 상태 | 설명 |
|---|---|
| `OPEN` | 현재 수강 신청 가능한 강의 |
| `CLOSED` | 모집이 마감되어 신청은 불가능하지만 조회 가능한 강의 |

수강생이 `DRAFT` 상태로 필터링을 요청하면 잘못된 요청으로 처리합니다.

---


### 7. 강의 상세 조회 정책

강의 상세 조회도 크리에이터와 수강생 모두 사용할 수 있는 공통 API로 제공합니다.

다만 상세 조회 역시 역할에 따라 접근 가능한 상태를 다르게 제한합니다.

#### 크리에이터

크리에이터는 본인이 개설한 강의라면 다음 상태를 모두 상세 조회할 수 있습니다.

```text
DRAFT
OPEN
CLOSED
```

단, 다른 크리에이터가 생성한 강의의 `DRAFT` 상세 정보는 조회할 수 없습니다.

#### 수강생

수강생은 다음 상태의 강의만 상세 조회할 수 있습니다.

```text
OPEN
CLOSED
```

`DRAFT` 상태의 강의는 공개 전 초안이므로 수강생에게 노출하지 않습니다.

상세 조회 응답에는 현재 신청 인원을 포함합니다.

```text
현재 신청 인원 = PENDING 상태 신청 수 + CONFIRMED 상태 신청 수
```

---

### 8. 수강 신청과 수강 확정의 분리

본 프로젝트에서는 수강 신청과 수강 확정을 서로 다른 비즈니스 흐름으로 해석했습니다.

```text
수강 신청 = 좌석을 확보하고 결제를 대기하는 행위
수강 확정 = 결제 완료 후 수강 상태를 확정하는 행위
```

수강 신청이 성공하면 `Enrollment`가 `PENDING` 상태로 생성됩니다.

`PENDING` 상태는 단순 임시 데이터가 아니라, 좌석을 점유한 결제 대기 상태로 간주합니다.

결제 확정 API가 호출되면 `PENDING` 상태의 신청이 `CONFIRMED` 상태로 변경됩니다.

외부 결제 시스템 연동은 과제 요구사항에 포함되어 있지 않으므로, 본 프로젝트에서는 결제 확정 API를 통해 단순 상태 변경으로 대체합니다.

---

### 9. 수강 신청 상태

수강 신청은 다음 상태를 가집니다.

| 상태 | 설명 | 좌석 점유 여부 |
|---|---|---|
| `PENDING` | 신청 완료, 결제 대기 | 점유 |
| `CONFIRMED` | 결제 완료, 수강 확정 | 점유 |
| `CANCELLED` | 취소됨 | 미점유 |

기본 상태 전이는 다음과 같습니다.

```text
PENDING -> CONFIRMED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
CANCELLED -> PENDING
```
CANCELLED -> PENDING 전이는 일반적인 복구가 아니라, 사용자의 명시적인 재신청 요청에 의해서만 허용됩니다.

각 상태의 의미는 다음과 같습니다.

- `PENDING`: 수강 신청은 완료되었지만 아직 결제 확정 전인 상태입니다. 이 상태에서도 좌석은 점유합니다.
- `CONFIRMED`: 결제가 완료되어 최종 수강이 확정된 상태입니다.
- `CANCELLED`: 신청이 취소되어 좌석을 점유하지 않는 상태입니다.

---

### 10. 정원 관리 기준

강의의 현재 신청 인원은 좌석을 점유하는 신청 상태를 기준으로 계산합니다.

```text
enrollment_count는 좌석을 점유하는 신청 수를 의미한다.
즉, PENDING + CONFIRMED 상태의 Enrollment 수와 동일해야 한다.
현재 신청 인원(enrollment_count) = PENDING 상태 신청 수 + CONFIRMED 상태 확정 수
```

`CANCELLED` 상태는 좌석을 점유하지 않으므로 정원 계산에서 제외합니다.

정원이 초과된 경우 일반 수강 신청은 실패합니다.

---

### 11. 수강 신청 동시성 제어(조건부 원자적 UPDATE 사용)

수강 신청은 동시에 여러 사용자가 요청할 수 있으므로, 좌석에 대해 경쟁 조건이 발생할 수 있습니다.

예를 들어 정원이 10명이고 현재 신청 인원이 9명인 상황에서 두 사용자가 동시에 신청하면, 단순 조회 후 저장 방식으로는 11명이 신청되는 문제가 발생할 수 있습니다.

이를 방지하기 위해 수강 신청 시 다음 방식으로 동시성을 제어합니다.

```text
enrollment_count = PENDING 상태 신청 수 + CONFIRMED 상태 신청 수
데이터 정합성과 동시성 제어를 위해 수강 신청 시 class_room row를 원자적으로 조건부 UPDATE합니다.

UPDATE class_room
SET enrollment_count = enrollment_count + 1
WHERE class_room_id = ?
  AND status = 'OPEN'
  AND enrollment_count < capacity;
```



수강 신청 처리 흐름은 다음과 같습니다.

```text
1. 수강 신청 요청
2. 클래스메이트 존재 여부 확인
3. class_room.enrollment_count 조건부 증가 시도
4. 증가 실패 시 강의 없음, 모집 중 아님, 정원 초과 중 하나로 예외 처리
5. 동일 강의/동일 클래스메이트 Enrollment를 FOR UPDATE로 조회
6. 기존 신청이 PENDING 또는 CONFIRMED이면 중복 신청 예외
7. 기존 신청이 CANCELLED이면 기존 row를 PENDING 상태로 재활성화
8. 기존 신청이 없으면 신규 Enrollment 생성
9. 트랜잭션 커밋
```

동시 요청 상황에서 모든 트랜잭션이 먼저 `class_room` row에 대해 조건부 UPDATE를 수행하도록 락 획득 순서를 통일했습니다.

이를 통해 `enrollment` 조회 잠금과 `class_room` 갱신 잠금이 교차하면서 발생할 수 있는 MySQL InnoDB 데드락 가능성을 줄이고, 동시에 여러 요청이 들어와도 최종 좌석 점유 수가 정원을 초과하지 않도록 보장합니다.

중복 신청 등으로 비즈니스 예외가 발생하는 경우 전체 트랜잭션이 rollback되므로, 앞서 수행된 `enrollment_count` 증가도 함께 rollback됩니다.

---

### 12. 결제 대기 만료 정책

`PENDING` 상태는 좌석을 점유하는 상태입니다.

따라서 사용자가 수강 신청만 하고 결제를 완료하지 않으면 좌석이 계속 점유되는 문제가 발생할 수 있습니다.

이를 방지하기 위해 본 프로젝트에서는 결제 대기 만료 정책을 적용합니다.

```text
수강 신청 후 10분 이내 결제 확정이 되지 않으면 자동 취소합니다.
```

수강 신청이 생성될 때 결제 만료 시각을 함께 저장합니다.

```text
paymentExpiredAt = enrollmentCreatedAt + 10분
```

1분 단위 스케줄러가 만료된 `PENDING` 신청을 조회하여 `CANCELLED` 상태로 변경합니다.

```text
실행 주기: 1분
대상: status = PENDING and paymentExpiredAt <= now
처리: PENDING -> CANCELLED
```
자동 취소된 신청은 좌석을 더 이상 점유하지 않으므로 enrollmentCount가 감소합니다.

운영 환경에서 여러 애플리케이션 인스턴스가 동시에 실행되는 경우에는 스케줄러 중복 실행을 막기 위해 분산 락이 필요할 수 있습니다. 본 과제에서는 단일 애플리케이션 인스턴스 실행을 기준으로 구현합니다.

---

### 13. 결제 확정

수강생은 `PENDING` 상태의 수강 신청을 결제 확정할 수 있습니다.

결제 확정 시 상태는 다음과 같이 변경됩니다.

```text
PENDING -> CONFIRMED
```

결제 확정 시 검증 규칙은 다음과 같습니다.

- 신청 내역이 존재해야 합니다.
- 요청한 수강생이 해당 신청의 소유자여야 합니다.
- 신청 상태가 `PENDING`이어야 합니다.
- 결제 대기 만료 시간이 지나지 않아야 합니다.
- 이미 `CONFIRMED` 또는 `CANCELLED` 상태인 신청은 결제 확정할 수 없습니다.
- 결제 확정은 paymentExpiredAt 이전까지만 가능하며, paymentExpiredAt 시각이 도래하면 만료된 것으로 간주합니다.

결제 확정 시점에는 정원 검사를 다시 수행하지 않습니다.

그 이유는 수강 신청 시점에 이미 `PENDING` 상태로 좌석을 점유했기 때문입니다.

---

### 14. 수강 취소

수강생은 본인의 수강 신청을 취소할 수 있습니다.

취소 정책은 신청 상태에 따라 다르게 적용합니다.

#### PENDING 상태 취소

`PENDING` 상태는 결제 전 상태이므로 언제든 취소할 수 있습니다.

```text
PENDING -> CANCELLED
```

사용자가 직접 취소하지 않더라도 결제 대기 시간이 10분을 초과하면 스케줄러에 의해 자동 취소됩니다.

#### CONFIRMED 상태 취소

`CONFIRMED` 상태는 결제 완료 상태이므로 결제 확정 후 7일 이내에만 취소할 수 있습니다.

```text
CONFIRMED -> CANCELLED
```

취소 가능 기간은 다음 기준으로 판단합니다.

```text
현재 시각 <= confirmedAt + 7일
```

결제 확정 후 7일이 지난 신청은 취소할 수 없습니다.

#### CANCELLED 상태 취소

이미 `CANCELLED` 상태인 신청은 다시 취소할 수 없습니다.

---

### 15. 재신청 정책

동일한 수강생은 동일한 강의에 대해 동시에 여러 개의 활성 신청을 가질 수 없습니다.

활성 신청은 다음 상태를 의미합니다.

```text
PENDING
CONFIRMED
```

따라서 동일 수강생이 같은 강의에 대해 `PENDING` 또는 `CONFIRMED` 상태의 신청을 이미 가지고 있다면 중복 신청은 불가능합니다.

다만 기존 신청이 `CANCELLED` 상태라면 좌석을 점유하지 않으므로, 정원이 남아 있는 경우 다시 신청할 수 있습니다.


구현 단순화를 위해 동일 강의와 동일 수강생 조합에 대해서는 하나의 Enrollment row를 유지하고, 기존 상태가 `CANCELLED`인 경우 해당 row를 다시 `PENDING` 상태로 변경하는 방식으로 재신청을 처리합니다.

```text
CANCELLED -> PENDING
```

이 전이는 일반적인 상태 복구가 아니라, 사용자의 명시적인 재신청 요청에 의해서만 허용됩니다.


---

### 17. 크리에이터의 강의별 수강생 목록 조회

크리에이터는 본인이 개설한 강의의 수강생 목록을 조회할 수 있습니다.

단, 다른 크리에이터가 개설한 강의의 수강생 목록은 조회할 수 없습니다.

검증 규칙은 다음과 같습니다.

```text
classRoom.creatorId == 요청 creatorId
```

일치하지 않는 경우 `FORBIDDEN` 예외를 반환합니다.

수강생 목록 조회 대상은 기본적으로 `PENDING`, `CONFIRMED`, `CANCELLED` 상태를 모두 포함할 수 있습니다.

다만 실제 수강 확정자만 보고 싶은 경우를 위해 `status` 필터를 제공합니다.

예시:

```text
GET /api/class-rooms/{classRoomId}/enrollments?creatorId=1&status=CONFIRMED&page=0&size=20
```

---

### 18. 내 수강 신청 목록 조회

수강생은 본인의 수강 신청 목록을 조회할 수 있습니다.

```text
GET /api/enrollments/me?classmateId=1&page=0&size=20
```

조회 결과에는 다음 상태의 신청이 포함될 수 있습니다.

- `PENDING`
- `CONFIRMED`
- `CANCELLED`

응답에는 신청한 강의 정보와 신청 상태를 함께 제공합니다.

---

### 19. 페이지네이션 정책

모든 목록 조회 API는 페이지 번호 기반 페이지네이션을 사용합니다.

적용 대상은 다음과 같습니다.

- 강의 목록 조회
- 내 수강 신청 목록 조회
- 강의별 수강생 목록 조회

요청 파라미터는 다음 형식을 사용합니다.

```text
page=0&size=20
```

기본값은 다음과 같습니다.

```text
page = 0
size = 20
```

최대 size는 과도한 조회를 방지하기 위해 제한할 수 있습니다.

```text
max size = 50
```

---



## 설계 결정과 이유

### 1. Class 대신 ClassRoom 도메인명 사용

Java에서 `Class`는 이미 존재하는 타입 이름이며 도메인 클래스명으로 사용하기 부적절합니다.

따라서 본 프로젝트에서는 강의를 의미하는 도메인명으로 `ClassRoom`을 사용했습니다.

---

### 2. DRAFT 강의는 수강생에게 노출하지 않음

`DRAFT` 상태는 크리에이터가 아직 공개하지 않은 초안입니다.

따라서 수강생은 `DRAFT` 상태의 강의를 목록 조회하거나 상세 조회할 수 없습니다.

크리에이터는 본인이 개설한 강의를 관리해야 하므로 `DRAFT`, `OPEN`, `CLOSED` 상태를 모두 조회할 수 있습니다.

---

### 3. PENDING을 좌석 점유 상태로 해석

본 프로젝트에서는 `PENDING` 상태를 단순 임시 신청이 아니라 좌석을 점유한 결제 대기 상태로 해석했습니다.

이렇게 설계한 이유는 다음과 같습니다.

- 사용자가 수강 신청에 성공했다면 좌석은 확보되었다고 보는 것이 자연스럽습니다.
- `PENDING`을 정원에 포함하지 않으면 정원보다 많은 사용자가 결제 대기 상태가 될 수 있습니다.
- 결제 확정 시점에 정원 초과로 실패하는 흐름은 사용자 경험상 부자연스럽습니다.
- 동시성 제어 지점을 수강 신청 시점으로 집중시킬 수 있습니다.

---

### 4. 정원 초과 방지를 위한 조건부 원자적 UPDATE 사용

수강 신청은 동시에 여러 사용자가 요청할 수 있는 기능입니다.

예를 들어 정원이 10명이고 현재 좌석 점유 수가 9명인 상황에서 두 사용자가 동시에 신청하면, 단순히 현재 신청 수를 조회한 뒤 `Enrollment`를 저장하는 방식으로는 두 요청이 모두 성공하여 정원을 초과할 수 있습니다.

본 프로젝트에서는 이를 방지하기 위해 `class_room` 테이블에 `enrollment_count` 컬럼을 두고, 해당 값을 현재 좌석 점유 수로 관리합니다.

```text
enrollment_count = PENDING 상태 신청 수 + CONFIRMED 상태 신청 수
```
PENDING은 결제 대기 상태이지만 이미 좌석을 확보한 상태로 해석하므로 정원 계산에 포함합니다. CANCELLED 상태는 좌석을 점유하지 않으므로 정원 계산에서 제외합니다.

수강 신청 시에는 다음과 같은 조건부 UPDATE를 실행합니다.

```text
UPDATE class_room
SET enrollment_count = enrollment_count + 1
WHERE class_room_id = ?
  AND status = 'OPEN'
  AND enrollment_count < capacity;
```
이 UPDATE는 DB row 단위로 원자적으로 실행됩니다.

따라서 동시에 여러 사용자가 마지막 좌석을 신청하더라도 enrollment_count < capacity 조건을 만족한 요청만 성공합니다.

처리 결과는 affected row 수로 판단합니다.



---

### 5. PENDING 자동 취소 스케줄러 도입

`PENDING` 상태가 좌석을 점유하기 때문에 결제를 완료하지 않은 신청이 계속 남아 있으면 다른 사용자가 신청할 수 없습니다.

이를 방지하기 위해 결제 대기 시간을 10분으로 제한하고, 1분 단위 스케줄러가 만료된 `PENDING` 신청을 자동으로 `CANCELLED` 상태로 변경합니다.

---

### 6. 조회 API는 DTO Projection 사용

목록 조회와 상세 조회는 엔티티를 직접 반환하지 않고 DTO Projection으로 조회합니다.

이렇게 설계한 이유는 다음과 같습니다.

- 응답에 필요한 필드만 조회할 수 있습니다.
- 불필요한 엔티티 로딩을 줄일 수 있습니다.
- Lazy Loading으로 인한 N+1 문제를 방지할 수 있습니다.
- API 응답 구조와 엔티티 구조를 분리할 수 있습니다.
- 물론, 본 과제에서는 단순한 응답 필드 구조로, Lazy Loading으로 인한 N+1 문제 상황은 발생하지 않습니다.

---

## 기능 요구사항 명세

### 강의 관리

| 기능 | 설명 |
|---|---|
| 강의 생성 | 크리에이터가 강의를 생성합니다. 생성 시 상태는 `DRAFT`입니다. |
| 강의 수정 | `DRAFT` 상태에서만 강의 정보를 수정할 수 있습니다. |
| 강의 모집 시작 | 크리에이터가 `DRAFT` 상태의 강의를 `OPEN` 상태로 변경합니다. |
| 강의 모집 마감 | 크리에이터가 `OPEN` 상태의 강의를 `CLOSED` 상태로 변경합니다. |
| 강의 목록 조회 | 역할별 상태 필터와 페이지네이션을 지원합니다. |
| 강의 상세 조회 | 현재 신청 인원을 포함한 강의 상세 정보를 조회합니다. |

---

### 수강 신청 관리

| 기능 | 설명 |
|---|---|
| 수강 신청 | `OPEN` 상태의 강의에 신청합니다. 성공 시 `PENDING` 상태가 됩니다. |
| 결제 확정 | `PENDING` 상태의 신청을 `CONFIRMED` 상태로 변경합니다. |
| 수강 취소 | `PENDING` 또는 `CONFIRMED` 상태의 신청을 `CANCELLED` 상태로 변경합니다. |
| 자동 취소 | 결제 대기 시간이 10분을 초과한 `PENDING` 신청을 스케줄러가 자동 취소합니다. |
| 내 신청 목록 조회 | 수강생 본인의 신청 목록을 페이지네이션으로 조회합니다. |

---

## API 목록 및 예시

### API 전체 목록

### 강의 API

| 기능            | Method | URL | 설명 |
|---------------|---|---|---|
| 강의 등록         | `POST` | `/api/class-rooms` | 크리에이터가 강의를 생성합니다. 생성된 강의는 `DRAFT` 상태입니다. |
| 강의 수정         | `PUT` | `/api/class-rooms/{classRoomId}` | `DRAFT` 상태의 강의만 수정할 수 있습니다. |
| 강의 모집 시작      | `PATCH` | `/api/class-rooms/{classRoomId}/open` | `DRAFT` 상태의 강의를 `OPEN` 상태로 변경합니다. |
| 강의 모집 마감      | `PATCH` | `/api/class-rooms/{classRoomId}/close` | `OPEN` 상태의 강의를 `CLOSED` 상태로 변경합니다. |
| 강의 목록 조회      | `GET` | `/api/class-rooms` | 역할별로 조회 가능한 강의 목록을 페이지네이션으로 조회합니다. |
| 강의 상세 조회      | `GET` | `/api/class-rooms/{classRoomId}` | 강의 상세 정보와 현재 신청 인원을 조회합니다. |
| 강의별 수강생 목록 조회 | `GET` | `/api/class-rooms/{classRoomId}/enrollments` | 크리에이터가 본인 강의의 수강생 목록을 조회합니다. |

---

### 수강 신청 API

| 기능 | Method | URL | 설명 |
|---|---|---|---|
| 수강 신청 | `POST` | `/api/class-rooms/{classRoomId}/enrollments` | 수강생이 `OPEN` 상태의 강의에 신청합니다. 성공 시 `PENDING` 상태가 됩니다. |
| 결제 확정 | `PATCH` | `/api/enrollments/{enrollmentId}/confirm` | `PENDING` 상태의 신청을 `CONFIRMED` 상태로 변경합니다. |
| 수강 취소 | `PATCH` | `/api/enrollments/{enrollmentId}/cancel` | `PENDING` 또는 `CONFIRMED` 상태의 신청을 `CANCELLED` 상태로 변경합니다. |
| 내 수강 신청 목록 조회 | `GET` | `/api/enrollments/me` | 수강생 본인의 신청 목록을 페이지네이션으로 조회합니다. |
| 수강 신청 상세 조회 | `GET` | `/api/enrollments/{enrollmentId}` | 수강생 본인의 신청 상세 정보를 조회합니다. |
---

### 1. 강의 등록

### Request

```http
POST /api/class-rooms
Content-Type: application/json
```

```json
{
  "creatorId": 1,
  "title": "Spring Boot 입문",
  "description": "Spring Boot 기반 REST API 개발 강의",
  "price": 50000,
  "capacity": 30,
  "startAt": "2026-05-01T00:00:00",
  "endAt": "2026-06-01T00:00:00"
}
```

### Response

```http
201 Created
Location: /api/class-rooms/1
```

```text
body 없음.
```

---

### 2. 강의 수정

### Request

```http
PUT /api/class-rooms/{classRoomId}
Content-Type: application/json
```

```json
{
  "creatorId": 1,
  "title": "Spring Boot 실전 입문",
  "description": "Spring Boot 기반 REST API 실전 강의",
  "price": 60000,
  "capacity": 25,
  "startAt": "2026-05-01T00:00:00",
  "endAt": "2026-06-01T00:00:00"
}
```

### Response

```text
204 No Content
```

---

### 3. 강의 모집 시작

### Request

```http
PATCH /api/class-rooms/{classRoomId}/open
Content-Type: application/json
```

```json
{
  "creatorId": 1
}
```

### Response

```text
204 No Content
```

---

### 4. 강의 모집 마감

### Request

```http
PATCH /api/class-rooms/{classRoomId}/close
Content-Type: application/json
```

```text
{
  "creatorId": 1
}
```

### Response

```text
204 No Content
```

---

### 5. 크리에이터 강의 목록 조회

크리에이터는 본인이 생성한 강의를 `DRAFT`, `OPEN`, `CLOSED` 상태로 필터링하여 조회할 수 있습니다.

### Request

```http
GET /api/class-rooms?role=CREATOR&creatorId=1&status=DRAFT&page=0&size=20
```

### Response

```json
{
  "content": [
    {
      "classRoomId": 1,
      "creatorId": 1,
      "creatorName": "creator1",
      "title": "Spring Boot 입문",
      "price": 50000,
      "capacity": 30,
      "enrollmentCount": 0,
      "status": "DRAFT",
      "startAt": "2026-05-01T00:00:00",
      "endAt": "2026-06-01T00:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 6. 수강생 강의 목록 조회

수강생은 `OPEN`, `CLOSED` 상태의 강의만 필터링하여 조회할 수 있습니다.

### Request

```http
GET /api/class-rooms?role=CLASSMATE&status=OPEN&page=0&size=20
```

### Response

```json
{
  "content": [
    {
      "classRoomId": 1,
      "creatorId": 1,
      "creatorName": "creator1",
      "title": "Spring Boot 입문",
      "price": 50000,
      "capacity": 30,
      "enrollmentCount": 0,
      "status": "DRAFT",
      "startAt": "2026-05-01T00:00:00",
      "endAt": "2026-06-01T00:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 7. 강의 상세 조회

### Request

```http
GET /api/class-rooms/1?role=CREATOR&creatorId=1
```

또는 수강생 조회:

```http
GET /api/class-rooms/1?role=CLASSMATE
```

### Response

```json
{
  "classRoomId": 1,
  "creatorId": 1,
  "creatorName": "creator_1",
  "title": "Spring Boot 입문",
  "description": "Spring Boot 기반 REST API 개발 강의",
  "price": 51000,
  "capacity": 30,
  "enrollmentCount": 0,
  "status": "CLOSED",
  "startAt": "2026-05-01T00:00:00",
  "endAt": "2026-06-01T00:00:00",
  "createdAt": "2026-04-28T04:20:13",
  "updatedAt": "2026-04-28T04:25:21"
}
```

---

### 8. 수강 신청

### Request

```http
POST /api/class-rooms/1/enrollments
Content-Type: application/json
```

```json
{
  "classmateId": 10
}
```

### Response

```http
201 Created
Location: /api/enrollments/1
```

```json
{
  "enrollmentId": 2,
  "classRoomId": 2,
  "classmateId": 9,
  "status": "PENDING",
  "paymentExpiredAt": "2026-04-28T04:56:32",
  "confirmedAt": null,
  "cancelledAt": null,
  "createdAt": "2026-04-28T04:46:32"
}
```

---

### 9. 결제 확정

### Request

```http
PATCH /api/enrollments/1/confirm
Content-Type: application/json
```

```json
{
  "classmateId": 10
}
```

### Response

```json
{
  "enrollmentId": 2,
  "classRoomId": 2,
  "classmateId": 9,
  "status": "CONFIRMED",
  "paymentExpiredAt": "2026-04-28T04:56:32",
  "confirmedAt": "2026-04-28T04:47:28",
  "cancelledAt": null,
  "createdAt": "2026-04-28T04:46:32"
}
```

---

### 10. 수강 취소

### Request

```http
PATCH /api/enrollments/1/cancel
Content-Type: application/json
```

```json
{
  "classmateId": 10
}
```

### Response

```json
{
  "enrollmentId": 2,
  "classRoomId": 2,
  "classmateId": 9,
  "status": "CANCELLED",
  "paymentExpiredAt": "2026-04-28T04:56:32",
  "confirmedAt": "2026-04-28T04:47:28",
  "cancelledAt": "2026-04-28T04:48:10",
  "createdAt": "2026-04-28T04:46:32"
}
```

---

### 11. 내 수강 신청 목록 조회

### Request

```http
GET /api/enrollments/me?classmateId=10&page=0&size=20
```

### Response

```json
{
  "content": [
    {
      "enrollmentId": 1,
      "classRoomId": 1,
      "classRoomTitle": "Spring Boot 입문",
      "price": 50000,
      "status": "CONFIRMED",
      "createdAt": "2026-04-26T10:00:00",
      "confirmedAt": "2026-04-26T10:05:00",
      "cancelledAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 12. 강의별 수강생 목록 조회

크리에이터는 본인이 개설한 강의의 수강생 목록만 조회할 수 있습니다.

### Request

```http
GET /api/class-rooms/2/enrollments?creatorId=1&status=CANCELLED&page=0&size=20
```

### Response

```json
{
  "content": [
    {
      "enrollmentId": 2,
      "classmateId": 9,
      "classmateName": "classmate_9",
      "status": "CANCELLED",
      "paymentExpiredAt": "2026-04-28T04:56:32",
      "confirmedAt": "2026-04-28T04:47:28",
      "cancelledAt": "2026-04-28T04:48:10",
      "createdAt": "2026-04-28T04:46:32"
    },
    {
      "enrollmentId": 1,
      "classmateId": 10,
      "classmateName": "classmate_10",
      "status": "CANCELLED",
      "paymentExpiredAt": "2026-04-28T04:48:06",
      "confirmedAt": null,
      "cancelledAt": "2026-04-28T04:49:00",
      "createdAt": "2026-04-28T04:38:06"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

---


---

## 데이터 모델 설명

### 1. DB 스키마, DDL

DB 스키마와 DDL은 본 프로젝트의 resources/db/migration/ *.sql 파일을 제공하여 확인할 수 있습니다.

### 2. ERD

ERD cloud URL

다음 URL을 통해 ERD를 확인할 수 있습니다.
```text
https://www.erdcloud.com/d/39WesCmjRoB5w2Gpf
```

본 프로젝트는 수강 신청 시스템의 핵심 도메인을 다음 4개 엔티티로 구성합니다.

- `Creator`: 강의를 개설하는 크리에이터
- `Classmate`: 강의에 수강 신청하는 클래스메이트
- `ClassRoom`: 크리에이터가 개설한 강의
- `Enrollment`: 클래스메이트의 수강 신청 내역

`Creator`와 `Classmate`는 본 과제에서 회원가입/로그인 기능의 대상이 아니며, `class_room`, `enrollment`의 FK 참조와 테스트 데이터 구성을 위해 사용합니다.

---

## Creator

크리에이터 정보를 저장하는 엔티티입니다.

본 과제에서는 크리에이터 CRUD API를 별도로 제공하지 않고, Flyway seed 데이터를 통해 초기 데이터를 제공합니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | 크리에이터 ID |
| `name` | `String` | 크리에이터 이름 |
| `email` | `String` | 크리에이터 이메일 |
| `status` | `CreatorStatus` | 크리에이터 상태 |
| `createdAt` | `LocalDateTime` | 생성 시간 |
| `updatedAt` | `LocalDateTime` | 수정 시간 |

크리에이터 상태는 다음 enum으로 관리합니다.

```java
public enum CreatorStatus {
  ACTIVE,
  DELETED
}
```

---

## Classmate

클래스메이트 정보를 저장하는 엔티티입니다.

본 과제에서는 클래스메이트 CRUD API를 별도로 제공하지 않고, Flyway seed 데이터를 통해 초기 데이터를 제공합니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | 클래스메이트 ID |
| `name` | `String` | 클래스메이트 이름 |
| `email` | `String` | 클래스메이트 이메일 |
| `status` | `ClassmateStatus` | 클래스메이트 상태 |
| `createdAt` | `LocalDateTime` | 생성 시간 |
| `updatedAt` | `LocalDateTime` | 수정 시간 |

클래스메이트 상태는 다음 enum으로 관리합니다.

```java
public enum ClassmateStatus {
  ACTIVE,
  DELETED
}
```

---

## ClassRoom

강의 정보를 저장하는 엔티티입니다.

크리에이터가 강의를 생성하면 최초 상태는 `DRAFT`가 됩니다. 이후 크리에이터가 모집 시작 API를 호출하면 `OPEN` 상태가 되며, `OPEN` 상태의 강의에만 수강 신청할 수 있습니다.


| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | 강의 ID |
| `creator` | `Creator` | 강의를 생성한 크리에이터 |
| `title` | `String` | 강의 제목 |
| `description` | `String` | 강의 설명 |
| `price` | `Long` | 강의 가격 |
| `capacity` | `int` | 최대 수강 정원 |
| `enrollmentCount` | `int` | 현재 좌석 점유 수 |
| `status` | `ClassRoomStatus` | 강의 상태 |
| `startAt` | `LocalDateTime` | 수강 시작 일시 |
| `endAt` | `LocalDateTime` | 수강 종료 일시 |
| `createdAt` | `LocalDateTime` | 생성 시간 |
| `updatedAt` | `LocalDateTime` | 수정 시간 |

강의 상태는 다음 enum으로 관리합니다.

```java
public enum ClassRoomStatus {
    DRAFT,
    OPEN,
    CLOSED
}
```

---

## Enrollment

수강 신청 정보를 저장하는 엔티티입니다.

클래스메이트가 강의에 수강 신청하면 `PENDING` 상태의 `Enrollment`가 생성됩니다. `PENDING` 상태는 결제 대기 상태이지만 좌석을 점유합니다. 결제 확정 시 `CONFIRMED` 상태가 되고, 수강 취소 또는 결제 대기 만료 시 `CANCELLED` 상태가 됩니다.


| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` | 수강 신청 ID |
| `classRoom` | `ClassRoom` | 신청한 강의 |
| `classmate` | `Classmate` | 신청한 클래스메이트 |
| `status` | `EnrollmentStatus` | 신청 상태 |
| `paymentExpiredAt` | `LocalDateTime` | 결제 대기 만료 시각 |
| `confirmedAt` | `LocalDateTime` | 결제 확정 시각 |
| `cancelledAt` | `LocalDateTime` | 취소 시각 |
| `cancelReason` | `CancelReason` | 취소 사유 |
| `createdAt` | `LocalDateTime` | 생성 시간 |
| `updatedAt` | `LocalDateTime` | 수정 시간 |

신청 상태는 다음 enum으로 관리합니다.

```java
public enum EnrollmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
```

취소 사유는 다음 enum으로 관리합니다.

```java
public enum CancelReason {
  USER_CANCELLED,
  PAYMENT_EXPIRED
}
```

## 주요 제약 조건

### 동일 강의 중복 신청 방지

동일한 클래스메이트는 동일한 강의에 대해 여러 개의 활성 신청을 가질 수 없습니다.

활성 신청은 다음 상태를 의미합니다.

```text
PENDING
CONFIRMED
```

DB에서는 다음 unique 제약으로 동일 강의와 동일 클래스메이트 조합을 하나의 row로 제한합니다.

```text
UNIQUE (class_room_id, classmate_id)
```

이 제약에 따라 동일 클래스메이트가 같은 강의에 다시 신청하는 경우에는 새 row를 생성하지 않고, 기존 `CANCELLED` 상태의 row를 `PENDING` 상태로 재활성화합니다.

---

### 정원 초과 방지

`ClassRoom.capacity`는 최대 수강 정원을 의미합니다.

`ClassRoom.enrollmentCount`는 현재 좌석 점유 수를 의미하며, 다음 조건을 만족해야 합니다.

```text
0 <= enrollmentCount <= capacity
```

수강 신청 시 `enrollmentCount < capacity` 조건을 포함한 UPDATE를 사용하여 정원을 초과하지 않도록 보장합니다.

---

### 결제 대기 만료

`PENDING` 상태는 좌석을 점유하므로, 결제가 장시간 완료되지 않으면 다른 사용자가 수강 신청할 수 없는 문제가 발생할 수 있습니다.

이를 방지하기 위해 수강 신청 생성 시 결제 만료 시각을 저장합니다.

```text
paymentExpiredAt = 신청 시각 + 10분
```

1분 단위 스케줄러가 다음 조건의 신청을 자동 취소합니다.

```text
status = PENDING
paymentExpiredAt <= now
```

자동 취소된 신청은 `CANCELLED` 상태가 되며, `cancelReason`은 `PAYMENT_EXPIRED`로 저장됩니다.

---

### 수강 취소 가능 기간

`PENDING` 상태의 신청은 결제 전 상태이므로 언제든 직접 취소할 수 있습니다.

`CONFIRMED` 상태의 신청은 결제 확정 후 7일 이내에만 취소할 수 있습니다.

```text
현재 시각 <= confirmedAt + 7일
```

취소 가능 기간이 지난 `CONFIRMED` 신청은 취소할 수 없습니다.

---

## 테이블 관계 요약

```text
Creator 1 : N ClassRoom
ClassRoom 1 : N Enrollment
Classmate 1 : N Enrollment
```

관계 설명은 다음과 같습니다.

- 하나의 `Creator`는 여러 개의 `ClassRoom`을 생성할 수 있습니다.
- 하나의 `ClassRoom`에는 여러 개의 `Enrollment`가 생성될 수 있습니다.
- 하나의 `Classmate`는 여러 강의에 대해 `Enrollment`를 가질 수 있습니다.
- 단, 동일한 `Classmate`는 동일한 `ClassRoom`에 대해 하나의 `Enrollment` row만 가질 수 있습니다.

---

## 주요 인덱스

조회 성능과 스케줄러 처리를 위해 다음 인덱스를 사용합니다.

| 인덱스 | 목적 |
|---|---|
| `uk_creator_email` | 크리에이터 이메일 중복 방지 |
| `uk_classmate_email` | 클래스메이트 이메일 중복 방지 |
| `idx_class_room_creator_status_created_at` | 크리에이터의 강의 목록 조회 |
| `idx_class_room_status_created_at` | 수강생의 공개 강의 목록 조회 |
| `uk_enrollment_class_room_classmate` | 동일 강의 중복 신청 방지 |
| `idx_enrollment_classmate_created_at` | 내 수강 신청 목록 조회 |
| `idx_enrollment_class_room_status_created_at` | 강의별 수강생 목록 조회 |
| `idx_enrollment_status_payment_expired_at` | 결제 대기 만료 스케줄러 조회 |

---











---
## 테스트

본 프로젝트는 핵심 비즈니스 규칙과 데이터 정합성을 검증하기 위해 계층별 테스트를 구성했습니다.

테스트는 단순 성공 케이스뿐 아니라 상태 전이 실패, 권한 검증, Validation 실패, Repository Query 검증, MySQL 기반 동시성 제어까지 포함합니다.

---

### 테스트 실행 방법

전체 테스트는 다음 명령어로 실행할 수 있습니다.

```bash
./gradlew clean test
```

Windows 환경에서는 다음 명령어를 사용할 수 있습니다.

```bash
gradlew.bat clean test
```

특정 테스트만 실행할 수도 있습니다.

```bash
./gradlew test --tests "*ControllerTest"
./gradlew test --tests "*RepositoryTest"
./gradlew test --tests "*IntegrationTest"
./gradlew test --tests "*ConcurrencyTest"
```

Windows 환경에서는 다음과 같이 실행합니다.

```bash
gradlew.bat test --tests "*ControllerTest"
gradlew.bat test --tests "*RepositoryTest"
gradlew.bat test --tests "*IntegrationTest"
gradlew.bat test --tests "*ConcurrencyTest"
```

---

### 테스트 환경

테스트는 실제 운영 DB와 유사한 환경에서 검증하기 위해 H2가 아닌 MySQL 8.4 Testcontainers를 사용합니다.

Repository 테스트와 Service 통합 테스트는 각각 독립된 MySQL Testcontainer를 사용합니다.

| 테스트 구분 | 테스트 환경 |
|---|---|
| Controller Test | `@WebMvcTest`, `MockMvc`, Mock Service |
| Entity Test | Spring Context 없이 JUnit 5, AssertJ, Mockito |
| Repository Test | `@DataJpaTest`, MySQL Testcontainers |
| Service Integration Test | `@SpringBootTest`, MySQL Testcontainers |
| Concurrency Test | `@SpringBootTest`, MySQL Testcontainers, ExecutorService, CountDownLatch |

Repository 테스트에서는 `@AutoConfigureTestDatabase(replace = NONE)` 설정을 사용하여 Spring Boot가 테스트 DB를 H2로 대체하지 않도록 했습니다.

또한 JPA Auditing 필드인 `createdAt`, `updatedAt`이 Repository slice test에서도 정상 동작하도록 `JpaAuditingConfig`를 테스트 컨텍스트에 명시적으로 import했습니다.

Testcontainers는 테스트 클래스 묶음 실행 시 컨테이너 수명주기와 Spring TestContext 캐시가 충돌하지 않도록 singleton container 방식으로 구성했습니다.

테스트 간 데이터 간섭을 방지하기 위해 각 테스트 실행 전 다음 테이블을 초기화합니다.

```text
enrollment
class_room
```

`creator`, `classmate` 테이블은 Flyway seed 데이터를 사용하므로 테스트 초기화 대상에서 제외합니다.

---

### 테스트 전략

테스트 전략은 다음 기준으로 나누었습니다.

| 계층 | 목적 |
|---|---|
| Entity Test | 도메인 상태 전이와 비즈니스 규칙을 빠르게 검증 |
| Controller Test | HTTP 요청/응답, Validation, 예외 응답 구조 검증 |
| Repository Test | JPQL, Native Query, DTO Projection, 조건부 UPDATE 검증 |
| Service Integration Test | 실제 DB 기반 전체 비즈니스 흐름 검증 |
| Concurrency Test | 정원 초과 방지와 중복 신청 방지의 동시성 검증 |

---

### Controller Test

Controller 테스트는 `@WebMvcTest`와 `MockMvc`를 사용합니다.

Service 계층은 Mock 객체로 대체하고, Controller 계층의 책임만 검증합니다.

주요 검증 항목은 다음과 같습니다.

- 정상 요청 시 HTTP status code 검증
- 생성 API의 `Location` 응답 헤더 검증
- Bean Validation 실패 시 400 응답 검증
- JSON 타입 오류 발생 시 400 타입 오류 응답 검증
- Service 계층 예외 발생 시 전역 예외 응답 구조 검증
- 페이지네이션 응답에서 필요한 필드만 노출되는지 검증

강의 목록 조회 API는 Spring Data `Page`를 그대로 응답하지 않고 `PageResponse`로 변환합니다.

따라서 테스트에서는 다음 필드만 응답되는지 검증합니다.

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

또한 `PageImpl` 기본 직렬화 필드가 외부 API 응답에 노출되지 않는지 검증합니다.

```text
pageable
sort
first
last
number
numberOfElements
empty
```

---

### Entity Test

Entity 테스트는 Spring Context 없이 JUnit 5, AssertJ, Mockito만 사용합니다.

이를 통해 도메인 객체의 상태 전이 규칙을 빠르게 검증합니다.

#### ClassRoom Entity Test

검증 항목은 다음과 같습니다.

- 강의 생성 시 최초 상태는 `DRAFT`
- 강의 생성 시 신청 인원은 0명
- `DRAFT` 상태의 강의는 `OPEN` 상태로 변경 가능
- `DRAFT`가 아닌 강의는 모집 시작 불가
- `OPEN` 상태의 강의는 `CLOSED` 상태로 변경 가능
- `OPEN`이 아닌 강의는 모집 마감 불가
- `DRAFT` 상태의 강의만 수정 가능
- 강의 시작 시각이 종료 시각보다 이전이 아니면 수정 불가
- 요청한 `creatorId`가 강의 소유자인지 검증

#### Enrollment Entity Test

검증 항목은 다음과 같습니다.

- 수강 신청 생성 시 `PENDING` 상태
- 수강 신청 생성 시 결제 만료 시각은 신청 시각 기준 10분 뒤
- 결제 만료 전 `PENDING` 신청은 `CONFIRMED` 상태로 확정 가능
- `PENDING`이 아닌 신청은 결제 확정 불가
- 결제 가능 시간이 만료된 신청은 결제 확정 불가
- `PENDING` 신청은 사용자 취소 가능
- `CONFIRMED` 신청은 확정 후 7일 이내 취소 가능
- 확정 후 7일이 지나면 취소 불가
- 이미 취소된 신청은 다시 취소 불가
- 결제 만료 시간이 지난 `PENDING` 신청은 자동 취소 가능
- 결제 만료 시간이 지나지 않은 신청은 자동 취소 불가
- `CANCELLED` 상태의 신청은 재신청 시 `PENDING` 상태로 복구 가능
- `CANCELLED` 상태가 아니면 재신청 불가
- 요청한 `classmateId`가 신청 소유자인지 검증

---

### Repository Test

Repository 테스트는 `@DataJpaTest`와 MySQL Testcontainers를 사용합니다.

H2가 아닌 MySQL에서 테스트하는 이유는 다음과 같습니다.

- MySQL SQL 문법과 실제 실행 환경에 가까운 조건에서 검증
- `SELECT ... FOR UPDATE` 기반 잠금 쿼리 검증
- Native Query 기반 조건부 UPDATE 검증
- Flyway migration과 seed data 적용 검증
- 실제 DB 제약조건과 인덱스 동작 검증

#### ClassRoomRepository Test

검증 항목은 다음과 같습니다.

- 크리에이터의 강의 목록 DTO Projection 조회
- 수강생의 공개 강의 목록 DTO Projection 조회
- 강의 상세 DTO Projection 조회
- 강의별 수강 신청 목록 DTO Projection 조회
- `OPEN` 상태이고 정원이 남아 있으면 `enrollment_count` 증가 성공
- 정원이 가득 차면 `enrollment_count` 증가 실패

#### EnrollmentRepository Test

검증 항목은 다음과 같습니다.

- 강의 ID와 클래스메이트 ID로 수강 신청 조회
- 동일 강의/동일 클래스메이트 신청 조회 시 `FOR UPDATE` 잠금 사용
- 내 수강 신청 목록 DTO Projection 조회
- 결제 만료된 `PENDING` 신청 목록 조회
- 수강 신청 상세 DTO Projection 조회

---

### Service Integration Test

Service 통합 테스트는 `@SpringBootTest`와 MySQL Testcontainers를 사용합니다.

Controller를 거치지 않고 Service 계층을 직접 호출하여 실제 DB와 트랜잭션 기반 비즈니스 흐름을 검증합니다.

#### ClassRoomService Integration Test

검증 항목은 다음과 같습니다.

- 강의 생성 시 `DRAFT` 상태로 저장
- 존재하지 않는 크리에이터로 강의 생성 시 404 예외
- 강의 소유자는 `DRAFT` 상태의 강의 수정 가능
- 강의 소유자가 아니면 수정 불가
- 강의 소유자는 `DRAFT` 강의를 `OPEN` 상태로 변경 가능
- `OPEN` 강의는 `CLOSED` 상태로 변경 가능
- 크리에이터는 본인 강의의 `DRAFT` 상세 조회 가능
- 수강생은 `DRAFT` 강의 상세 조회 불가
- 수강생 강의 목록 조회에서 `DRAFT` 상태 필터는 허용되지 않음
- 크리에이터 강의 목록 조회에서 존재하지 않는 `creatorId`는 404 예외

#### EnrollmentService Integration Test

검증 항목은 다음과 같습니다.

- `OPEN` 강의에 수강 신청하면 `PENDING` 신청 생성
- 수강 신청 성공 시 `enrollment_count` 증가
- 존재하지 않는 클래스메이트로 신청 시 404 예외
- 존재하지 않는 강의에 신청 시 404 예외
- `OPEN` 상태가 아닌 강의에는 신청 불가
- 정원이 가득 찬 강의에는 신청 불가
- 이미 좌석을 점유 중인 신청이 있으면 중복 신청 불가
- `CANCELLED` 신청이 있으면 같은 row를 재사용하여 재신청
- 본인 신청이고 결제 가능 시간이 지나지 않았으면 결제 확정 가능
- 다른 클래스메이트의 신청은 결제 확정 불가
- 신청 취소 시 `CANCELLED` 상태로 변경되고 `enrollment_count` 감소
- 내 수강 신청 목록 조회 시 존재하지 않는 클래스메이트는 404 예외
- 수강 신청 상세 조회 시 소유자가 아니면 403 예외
- 결제 만료된 `PENDING` 신청은 자동 취소되고 `enrollment_count` 감소

---

### Concurrency Test

동시성 테스트는 실제 MySQL Testcontainers 환경에서 `ExecutorService`와 `CountDownLatch`를 사용하여 여러 요청을 동시에 발생시킵니다.

수강 신청은 다음 쿼리를 통해 정원 초과를 방지합니다.

```text
UPDATE class_room
SET enrollment_count = enrollment_count + 1
WHERE class_room_id = ?
  AND status = 'OPEN'
  AND enrollment_count < capacity;
```

동시성 테스트에서는 다음 시나리오를 검증합니다.

#### 정원 초과 방지

정원 5명인 강의에 10명의 클래스메이트가 동시에 신청하더라도 성공자는 5명만 발생해야 합니다.

검증 항목은 다음과 같습니다.

- 성공 요청 수는 5건
- 실패 요청 수는 5건
- `class_room.enrollment_count`는 5
- `enrollment` row 수는 5

#### 동일 클래스메이트 중복 신청 방지

동일 클래스메이트가 같은 강의에 동시에 10번 신청하더라도 하나의 활성 신청만 생성되어야 합니다.

검증 항목은 다음과 같습니다.

- 성공 요청 수는 1건
- 실패 요청 수는 9건
- `class_room.enrollment_count`는 1
- `enrollment` row 수는 1
- 실패 요청은 중복 신청 예외로 처리

---

### 테스트로 검증한 주요 비즈니스 규칙

본 테스트 코드는 다음 핵심 비즈니스 규칙을 검증합니다.

#### 강의 상태 규칙

```text
DRAFT -> OPEN
OPEN -> CLOSED
```

- `DRAFT` 상태에서만 강의 수정 가능
- 수강생은 `DRAFT` 강의 조회 불가
- `OPEN` 상태 강의만 수강 신청 가능

#### 수강 신청 상태 규칙

```text
PENDING -> CONFIRMED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
CANCELLED -> PENDING
```

- `PENDING`, `CONFIRMED`는 좌석 점유 상태
- `CANCELLED`는 좌석 미점유 상태
- `CANCELLED` 신청은 재신청 가능
- 이미 좌석을 점유 중인 신청은 중복 신청 불가

#### 정원 정합성 규칙

```text
enrollment_count = PENDING 상태 신청 수 + CONFIRMED 상태 신청 수
```

- 신청 성공 시 `enrollment_count + 1`
- 취소 또는 결제 만료 자동 취소 시 `enrollment_count - 1`
- 정원 초과 시 신청 실패
- 동시 신청 상황에서도 정원 초과 불가

#### 결제 대기 만료 규칙

```text
paymentExpiredAt = 신청 시각 + 10분
```

- 결제 만료 전에는 결제 확정 가능
- 결제 만료 시각이 도래하면 결제 확정 불가
- 만료된 `PENDING` 신청은 자동 취소 대상
- 자동 취소 시 좌석 점유 수 감소

#### 권한 검증 규칙

- 크리에이터는 본인 강의만 수정, 모집 시작, 모집 마감, 수강생 목록 조회 가능
- 클래스메이트는 본인 수강 신청만 상세 조회, 결제 확정, 취소 가능
- 권한이 없는 요청은 403 예외로 처리

---

### 테스트 실행 결과

최종적으로 다음 명령어 기준 전체 테스트 통과를 확인했습니다.

```bash
./gradlew clean test
```

Windows 환경에서는 다음 명령어 기준 전체 테스트 통과를 확인했습니다.

```bash
gradlew.bat clean test
```

테스트 성공 시 Gradle 출력은 다음과 같습니다.

```text
BUILD SUCCESSFUL
```

---

### 테스트 관련 제약사항

- 테스트는 Docker Desktop이 실행 중인 환경을 전제로 합니다.
- Repository / Integration / Concurrency 테스트는 MySQL Testcontainers를 사용하므로 최초 실행 시 MySQL Docker image pull 시간이 발생할 수 있습니다.
- `creator`, `classmate` 테스트 데이터는 Flyway seed data에 의존합니다.
- `enrollment`, `class_room` 데이터는 테스트 간 격리를 위해 각 테스트 전에 초기화합니다.
- 외부 결제 시스템 연동은 없으므로 결제 확정은 상태 변경 로직으로만 검증합니다.
- Redis 대기열 기능은 별도 선택 구현 영역이며, 현재 핵심 테스트 범위는 DB 기반 강의/수강 신청 정합성 검증에 집중합니다.

---

## 미구현 / 제약사항

- 실제 회원가입 및 로그인 기능은 구현하지 않았습니다.
- Spring Security 기반 인증/인가는 적용하지 않았습니다.
- 요청의 `creatorId`, `classmateId` 값을 통해 사용자를 식별합니다.
- 실제 외부 결제 시스템 연동은 구현하지 않았습니다.
- 결제 확정은 외부 결제 승인 결과를 검증하지 않고 신청 상태를 CONFIRMED로 변경하는 방식으로 대체했습니다.
- 운영 환경에서 여러 인스턴스가 스케줄러를 동시에 실행하는 경우에는 분산 락이 필요하지만, 본 프로젝트는 단일 인스턴스 실행을 기준으로 구현했습니다.
- Redis 대기열은 시간상 제약이 있어 구현하지 않았습니다.
- 동일 수강생이 같은 강의에 최초 신청을 동시에 여러 번 요청하는 극단적인 경우 DB의 unique 제약이 최종 중복 방어선으로 동작합니다. 이 경우 애플리케이션에서는 중복 신청 예외로 변환하여 처리합니다.

---

## AI 활용 범위

본 과제 수행 과정에서 AI 도구를 다음 범위로 활용했습니다.

- 요구사항 분석 및 기능 명세 정리
- README 구조 초안 작성 및 최종 검토
- 테스트 케이스 도출
- 테스트 코드 설계
- 코드 검증

AI가 생성한 내용을 그대로 제출하지 않고, 프로젝트 요구사항과 구현 코드에 맞게 직접 수정하고 검증했습니다.

최종 설계, 구현, 테스트, 예외 처리 방식은 직접 판단하여 반영했습니다.














