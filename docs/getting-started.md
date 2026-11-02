# 로컬 개발 환경 가이드

TransferTracker 백엔드를 로컬 환경에서 실행하는 방법입니다.

## 1. 사전 요구사항

- Java 21
- Docker 및 Docker Compose
- Git

MySQL 8.4는 Docker 컨테이너로 실행합니다.

## 2. 프로젝트 클론

```bash
git clone https://github.com/qlrxn1152/transfertracker-back-v1.git
cd transfertracker-back-v1
```

## 3. Docker 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다.

```dotenv
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=transfer_user
MYSQL_PASSWORD=your_db_password
```

`.env`는 Git에 커밋하지 않습니다.

## 4. Spring Boot 로컬 설정

`src/main/resources/application-local.properties` 파일을 생성합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/transfertracker
spring.datasource.username=${MYSQL_USER}
spring.datasource.password=${MYSQL_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
```

Spring Boot 실행 환경에 다음 변수도 설정해야 합니다.

| 환경변수 | 용도 |
|---|---|
| `MYSQL_USER` | DB 사용자 |
| `MYSQL_PASSWORD` | DB 비밀번호 |
| `API_FOOTBALL_KEY` | API-Football 인증 |
| `X_API_BEARER_TOKEN` | X API 인증 |
| `OPENAI_API_KEY` | OpenAI API 인증 |
| `JWT_SECRET` | JWT 서명용 Base64 인코딩 키 |

`JWT_SECRET`은 HS256에 사용할 수 있는 최소 32바이트 이상의 무작위 키를 Base64로 인코딩하여 설정합니다.

실제 비밀 키는 저장소에 커밋하지 않습니다.

## 5. 애플리케이션 실행

**MySQL 실행**

```bash
docker compose up -d
docker compose ps
```

MySQL은 `localhost:3307` 포트로 접근할 수 있습니다.

**Spring Boot 실행**

IntelliJ에서 필요한 환경변수를 지정하거나, 터미널에서 환경변수를 설정한 후 실행합니다.

macOS / Linux에서는 `.env`의 DB 접속 변수를 다음과 같이 셸 환경에 적용할 수 있습니다.

```bash
set -a
source .env
set +a
```

외부 API 키와 JWT Secret도 별도로 설정한 뒤 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

애플리케이션 실행 시 Flyway가 DB 마이그레이션을 적용합니다.

## 6. 실행 확인

```bash
curl -i http://localhost:8080/actuator/health
```

정상 실행 시 HTTP 200 응답을 확인할 수 있습니다.

## 7. 개발 환경 종료

```bash
docker compose down
```

MySQL 데이터는 Docker Volume에 유지됩니다.

`docker compose down -v`는 데이터 볼륨까지 삭제하므로 주의해야 합니다.