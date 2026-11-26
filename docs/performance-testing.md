# VEYRO API 성능 테스트 가이드

이 문서는 운영 서비스와 완전히 분리된 `performance` 프로필에서 조회 API의 Baseline을 반복 측정하는 방법을 설명한다. 이번 구성은 비즈니스 로직, Repository 쿼리, 인덱스, 서버 튜닝 값을 변경하지 않는다.

## 안전 범위

- 대상은 `http://localhost:8081`의 로컬 성능 애플리케이션이다.
- 성능 DB는 `performance/docker-compose.yml`의 `veyro_performance`이며, 포트는 `3308`이다.
- 외부 API 호출, 관리자 API, 동기화/번역/FCM 발송, 모든 쓰기 API는 시나리오에 포함하지 않는다.
- `AUTH_TOKEN`, 비밀번호, 인증 헤더는 보고서 및 Git에 저장하지 않는다. 로컬 셸 환경에서만 전달한다.
- `performance/reports/`는 자동 생성물이며 Git에서 제외된다. 기존 `performance/results/`는 보존한다.

## 구성

```text
performance/
├── run-k6.sh                    # API 하나 실행 및 실행별 보고서 생성
├── run-suite.sh                 # 도메인/전체 공개 조회 API 순차 실행
├── scripts/
│   ├── common/                  # 환경값, 부하 설정, HTTP, 검증, 보고서 공통 모듈
│   ├── read-api.js              # 개별 공개/회원 조회 API 진입점
│   ├── mixed/read-traffic.js    # 대표 공개 조회 API 복합 부하
│   └── aggregate-reports.mjs    # 순차 스위트 통합 결과 생성
├── seed/
│   ├── seed-posts*.sql
│   ├── seed-team-posts.sql
│   └── seed-api-read-fixtures.sql
└── reports/                     # 실행 결과, Git 제외
```

공통 기본 부하는 기존 Baseline과 동일하다.

| 항목 | 기본값 |
| --- | --- |
| Executor | `constant-arrival-rate` |
| Rate | `50` RPS |
| Duration | `1m` |
| Pre-allocated VUs | `50` |
| Max VUs | `200` |
| p95 | `< 300ms` |
| HTTP 실패율 | `< 0.1%` |
| Checks | `100%` |
| Dropped iterations | `0` |

`RPS`, `DURATION`, `PRE_ALLOCATED_VUS`, `MAX_VUS`, `PAGE`, `BASE_URL` 환경 변수로만 실행별 조건을 바꿀 수 있다. 시나리오 코드의 기존 Threshold는 바꾸지 않는다.

## 성능 환경과 Fixture 준비

### 가장 간단한 실행

아래 한 명령이면 Docker 성능 DB 기동, 로컬 성능 서버 기동, 데이터 상태 확인·시드 적용, fixture ID 조회, 시나리오 실행까지 처리한다. 매번 `TEAM_ID` 등을 export하거나 Spring Boot를 먼저 실행할 필요가 없다.

```bash
# 기본: posts-list
./performance/run-local-baseline.sh

# 전체 공개 조회 Baseline 또는 복합 부하
./performance/run-local-baseline.sh all-read
./performance/run-local-baseline.sh mixed-read
```

`all-read`는 23개 API를 각 1분씩 순차 실행하므로 약 23분 이상 걸릴 수 있다. 빠른 확인에는 기본 `posts-list` 또는 `mixed-read`를 사용한다. 자동 기동된 서버의 로그와 PID는 Git 제외 대상인 `performance/.runtime/`에 기록된다. 회원 API는 인증 토큰이 필요하므로 기존 `run-k6.sh member` 실행 방식을 사용한다.

```bash
# 1) 성능 전용 MySQL
docker compose -f performance/docker-compose.yml up -d

# 2) 성능 프로필 애플리케이션 (별도 터미널)
./gradlew bootRun --args='--spring.profiles.active=performance'

# 3) 성능 DB에 기존 게시물/관계 시드 후, 추가 읽기 fixture 적용
mysql -h 127.0.0.1 -P 3308 -u performance -p veyro_performance < performance/seed/seed-posts.sql
mysql -h 127.0.0.1 -P 3308 -u performance -p veyro_performance < performance/seed/seed-posts-extend.sql
mysql -h 127.0.0.1 -P 3308 -u performance -p veyro_performance < performance/seed/seed-team-posts.sql
mysql -h 127.0.0.1 -P 3308 -u performance -p veyro_performance < performance/seed/seed-api-read-fixtures.sql
```

마지막 SQL은 `TEAM_ID`, `PLAYER_ID`, `POST_ID`, `TRANSFER_ID`로 쓸 실제 DB PK를 출력한다. 결과를 셸 변수로 넣는다.

```bash
export TEAM_ID=... PLAYER_ID=... POST_ID=... TRANSFER_ID=...
export TEAM_POST_COUNT=700000
```

`seed-team-posts.sql`과 `seed-api-read-fixtures.sql`은 삭제하지 않고 `INSERT IGNORE`/존재 조건을 사용하므로 성능 DB에서 재실행해도 중복을 만들지 않는다. 운영 DB에는 절대 실행하지 않는다.

`seed-api-read-fixtures.sql`까지 적용한 기본 성능 데이터 규모는 다음과 같다.

| 데이터 | 규모 | 용도 |
| --- | ---: | --- |
| Post | 1,000,000 | 전체 게시물 목록 Baseline |
| Team | 603 | 전체/리그별 팀 조회 |
| Player | 15,003 | 전체/키워드/리그/팀별 선수 조회 |
| TeamPlayer | 15,003 | 팀 소속 선수 조회 |
| PostTeam / PostPlayer | 각 950,000 | 팀·선수별 게시물 조회 |
| Transfer | 300,002 | 목록·검색·리그·팀·선수별 이적 조회 |

추가한 15,000명의 선수는 6개 리그에 균등 분포하며, 각 선수는 이적 이력 20건을 가진다. 게시물 조회 부하용 High/Medium/Low 팀·선수의 `700,000 / 200,000 / 50,000` 관계 분포는 그대로 유지한다.

### 회원 조회용 JWT

회원 조회 API는 실제 Security 흐름을 검증하기 위해 `AUTH_TOKEN`이 필요하다. 성능 DB에서만 테스트 회원을 만들고, 로컬 로그인으로 얻은 짧은 수명의 JWT를 셸에 넣는다.

```bash
# performance/seed/performance-member.env.example의 이름만 참고한다.
# 실제 비밀번호, JWT는 파일 또는 Git에 저장하지 않는다.
export AUTH_TOKEN='local-performance-token-only'
```

회원의 관심 팀/선수/기자 데이터는 팀마다 달라 정상 빈 배열도 허용한다. 다만 응답 구조와 인증 성공은 검증한다.

## 개별 API 실행

기존 명령은 그대로 유지된다.

```bash
./performance/run-k6.sh posts-list
TEAM_ID="$TEAM_ID" TEAM_POST_COUNT=700000 ./performance/run-k6.sh team-posts
```

추가한 시나리오는 다음과 같다.

| 도메인 | 시나리오 |
| --- | --- |
| Post | `posts-list`, `team-posts`, `post-journalist`, `post-player`, `post-detail`, `post-sources` |
| Player | `players-list`, `players-keyword`, `players-league`, `players-team`, `player-detail` |
| Team | `teams-list`, `teams-league`, `team-detail`, `team-info`, `team-players` |
| Transfer | `transfers-list`, `transfers-keyword`, `transfers-league`, `transfers-team-filter`, `team-transfers`, `player-transfers`, `transfer-detail` |
| Member (JWT 필요) | `member-me`, `member-favorite-teams`, `member-favorite-players`, `member-favorite-player-detail`, `member-followed-journalists`, `member-journalist-posts`, `member-journalists-posts` |

예시:

```bash
PLAYER_ID="$PLAYER_ID" ./performance/run-k6.sh post-player
POST_ID="$POST_ID" ./performance/run-k6.sh post-detail
TRANSFER_ID="$TRANSFER_ID" ./performance/run-k6.sh transfer-detail
JOURNALIST=FABRIZIO_ROMANO ./performance/run-k6.sh post-journalist
KEYWORD='VEYRO Performance' ./performance/run-k6.sh players-keyword
LEAGUE_CODE=EPL ./performance/run-k6.sh teams-league
AUTH_TOKEN="$AUTH_TOKEN" PLAYER_ID="$PLAYER_ID" ./performance/run-k6.sh member-favorite-player-detail
```

모든 API는 단순 200 확인을 넘어서 JSON 구조, 요청한 ID, 페이지네이션, 필터가 검증 가능한 범위에서 맞는지 확인한다. `teams-league`, `players-league`, `transfers-league`는 현재 응답 DTO가 리그 코드를 각 행에 노출하지 않으므로 구조·비어 있지 않은 결과까지만 검증한다.

## 도메인 순차 실행과 복합 부하

```bash
# 해당 도메인의 모든 읽기 API를 순차 실행
TEAM_ID="$TEAM_ID" PLAYER_ID="$PLAYER_ID" POST_ID="$POST_ID" TRANSFER_ID="$TRANSFER_ID" \
  ./performance/run-k6.sh post

TEAM_ID="$TEAM_ID" PLAYER_ID="$PLAYER_ID" ./performance/run-k6.sh player
TEAM_ID="$TEAM_ID" ./performance/run-k6.sh team
TEAM_ID="$TEAM_ID" PLAYER_ID="$PLAYER_ID" TRANSFER_ID="$TRANSFER_ID" ./performance/run-k6.sh transfer

# JWT가 필요한 회원 API만 별도 실행
AUTH_TOKEN="$AUTH_TOKEN" PLAYER_ID="$PLAYER_ID" ./performance/run-k6.sh member

# 회원 API를 제외한 모든 공개 조회 API Baseline
TEAM_ID="$TEAM_ID" TEAM_POST_COUNT=700000 PLAYER_ID="$PLAYER_ID" POST_ID="$POST_ID" TRANSFER_ID="$TRANSFER_ID" \
  ./performance/run-k6.sh all-read

# 대표 공개 조회 5개를 iteration마다 순환하는 복합 부하
./performance/run-k6.sh mixed-read
```

순차 스위트는 특정 API가 Threshold를 통과하지 못하거나 실행 오류가 나도 나머지 API를 끝까지 측정한다. 모든 자식 결과가 끝난 뒤 실패가 하나라도 있으면 스위트 종료 코드는 `1`이다. 이는 CI·쉘 호출자가 실패를 인지하게 하면서도 Baseline 수집을 중단하지 않기 위한 동작이다.

## 실행별 보고서와 비교

`run-k6.sh`는 시작 시점의 Asia/Seoul 날짜·시·분·초로 디렉터리를 원자적으로 만든다. 같은 초에 실행되면 `-01`, `-02`를 붙인다.

```text
performance/reports/
└── 2026-11-26/
    └── 14-30-15/
        ├── report.html
        ├── summary.json
        └── metadata.json
```

- `report.html`: 사람이 보는 기존 HTML 성능 보고서다.
- `summary.json`: k6 원본 summary로 평균·분위수·RPS·실패율·Checks·Dropped Iterations·VU·Threshold 데이터를 포함한다.
- `metadata.json`: 실행 시각, 환경, Base URL, Git SHA/branch/dirty, k6 버전, 실제 VU·rate·duration, Threshold 결과와 k6 종료 코드를 담는다.

순차 스위트는 개별 실행 파일을 그대로 보존하면서 스위트 루트에 `aggregate.json`, `aggregate.md`, `manifest.tsv`를 추가한다. Aggregate에는 API별 평균, p95, p99, RPS, HTTP 실패율, Dropped Iterations, Threshold 결과, 종료 코드를 실제 artifact에서만 읽어 넣는다. 생성되지 못한 값은 임의 숫자 대신 `null` 또는 `N/A`다.

Git SHA는 **테스트를 시작한 저장소의 상태**다. 현재 실행 중인 Spring Boot 프로세스의 빌드 SHA를 의미하지는 않는다. `dirty=true`는 커밋하지 않은 파일이 있었다는 뜻이다.

## 실패 처리와 수동 검증

- Threshold 실패는 k6의 종료 코드 `99`를 유지하고, Metadata에 `threshold_failed`로 기록한다.
- 서버 미실행·잘못된 fixture ID·JWT 누락 등으로 k6 summary가 생성되지 않아도 Metadata를 남긴다.
- `report.html`/`summary.json`이 없을 때는 terminal의 k6 오류와 `metadata.json`의 `result.status`, `exitCode`를 확인한다.
- 검증 전에 실제 부하를 발생시키지 않으려면 아래 정적 검사를 쓴다.

```bash
bash -n performance/run-k6.sh performance/run-suite.sh
node --check performance/scripts/aggregate-reports.mjs
k6 inspect -e API_CASE=posts-list performance/scripts/read-api.js
```

`k6 inspect`는 HTTP 요청을 실행하지 않고 k6 모듈·옵션 초기화를 확인하는 용도다. 실제 성능 수치는 반드시 성능 DB와 성능 프로필 애플리케이션에서만 측정한다.
