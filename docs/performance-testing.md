# VEYRO 성능 테스트 실행 및 보고서 보관

## 현재 시나리오

현재 저장소에는 게시물 목록 API를 대상으로 하는 `posts-list` 시나리오가 있다.

- 스크립트: `performance/scripts/posts-list.js`
- 대상 기본 URL: `http://localhost:8081`
- 실행 프로필: `performance`
- executor: `constant-arrival-rate`
- 테스트 조건과 threshold는 스크립트에서 관리하며, 실행 래퍼는 이를 변경하지 않는다.

성능 전용 MySQL은 `performance/docker-compose.yml`로 실행할 수 있다.

```bash
docker compose -f performance/docker-compose.yml up -d
./gradlew bootRun --args='--spring.profiles.active=performance'
```

## 실행

프로젝트 루트에서 실행한다.

```bash
./performance/run-k6.sh
```

기본 시나리오는 `posts-list`다. 다른 Base URL이 필요할 때만 환경 변수로 덮어쓴다.

```bash
BASE_URL=http://localhost:8081 ./performance/run-k6.sh posts-list
```

기존의 직접 실행도 유지된다.

```bash
k6 run performance/scripts/posts-list.js
```

다만 직접 실행은 기존과 동일하게 `performance/results/posts-afterIndex-report.html`만 생성한다. 실행 이력을 보존하려면 항상 `run-k6.sh`를 사용한다.

## 결과 구조

래퍼는 시작 시점의 Asia/Seoul 시간을 기준으로 새 디렉터리를 원자적으로 만든다. 같은 초에 시작해도 `-01`, `-02` 식별자를 붙여 기존 결과를 덮어쓰지 않는다.

```text
performance/reports/
└── 2026-11-25/
    └── 17-25-40/
        ├── report.html
        ├── summary.json
        └── metadata.json
```

- `report.html`: 기존 `k6-simple-report.js` 디자인으로 생성하는 읽기용 결과
- `summary.json`: k6가 전달한 원본 summary. HTTP 요청, 응답 시간 분포, 실패율, checks, dropped iterations, VU 및 threshold 데이터를 포함한다.
- `metadata.json`: 실행 시각, 환경, Base URL, Git 상태, k6 버전, 실제 시나리오 설정, threshold 통과 여부, k6 종료 코드와 실행 상태

Git Commit SHA는 테스트를 시작한 시점의 **저장소 상태**이며, 실행 중인 Spring Boot 프로세스가 어떤 소스로 빌드됐는지를 보장하지는 않는다. `dirty=true`는 커밋되지 않은 변경이 있었음을 뜻한다. Git 정보를 읽지 못하면 임의 값 대신 `null`을 저장한다.

## 실패 결과

threshold 실패는 k6 종료 코드 `99`를 유지하고 `metadata.json`에 `threshold_failed`로 기록한다. 대상 서버 미실행이나 k6 실행 오류처럼 summary 자체를 만들 수 없는 경우에도 `metadata.json`을 남기고 원래 종료 코드를 반환한다. 가능한 경우 HTML·Summary·Metadata 모두 보존한다.

## 비교와 보관

HTML은 사람이 확인하고, `summary.json`과 `metadata.json`은 실행 간 수치 비교에 사용한다. 자동 생성되는 `performance/reports/`는 Git에서 제외된다. 기존 `performance/results/` 보고서와 성능 최적화 자료는 삭제·이동·untrack하지 않는다.
