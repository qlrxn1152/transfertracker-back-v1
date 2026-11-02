# ADR-001: 외부 API 호출과 데이터베이스 트랜잭션 분리

- **상태:** 검토 중
- **대상:** TransferTracker Backend
- **관련 기술:** Spring Boot, Spring Transaction, Spring Data JPA

## 1. 배경 (Context)

TransferTracker는 API-Football, X API 등 외부 서비스에서 데이터를 수집하고 MySQL에 저장합니다.

외부 API 호출은 네트워크 상태와 외부 서버의 응답 시간에 따라 지연될 수 있으므로, 데이터베이스 트랜잭션의 범위를 신중하게 설정할 필요가 있습니다.

## 2. 문제 (Problem)

외부 API 호출과 데이터베이스 저장 작업을 하나의 트랜잭션으로 묶으면, 외부 서비스의 응답 지연으로 인해 트랜잭션이 불필요하게 오래 유지될 수 있습니다.

특히 데이터베이스 커넥션이나 잠금의 점유 시간이 길어질 가능성이 있습니다. 실제 점유 여부와 시점은 JDBC 커넥션 획득 시점 및 실행된 SQL에 따라 달라집니다.

## 3. 결정 (Decision)

외부 API 호출 및 전체 실행 흐름을 조정하는 `OrchestrationService`와 데이터베이스 트랜잭션을 담당하는 `TxService`를 분리합니다.

```text
Controller
    ↓
OrchestrationService
    ├── 외부 API 요청
    ├── 응답 데이터 처리
    └── TxService
            ↓
        Repository
            ↓
          MySQL
```

## 4. 기대 효과 (Consequences)

- 외부 API 호출을 DB 트랜잭션 범위 밖에서 수행할 수 있습니다.
- 데이터 저장 작업의 트랜잭션 경계를 명확히 관리할 수 있습니다.
- 외부 API 처리와 영속성 로직을 개별적으로 테스트할 수 있습니다.

## 5. 트레이드오프 (Trade-offs)

- 서비스 계층이 추가되어 클래스 수가 증가합니다.
- 외부 API 호출 성공 후 DB 저장이 실패하면 별도의 재시도 또는 복구 정책이 필요할 수 있습니다.
- 여러 트랜잭션에 걸친 작업은 단일 트랜잭션만으로 원자성을 보장할 수 없습니다.

## 6. 검증 계획 (Validation)

- 실제 동기화 서비스의 `@Transactional` 적용 위치 확인
- 외부 API 호출 시 활성 트랜잭션 여부 검증
- DB 저장 실패 시 롤백 동작 확인
- 외부 API 실패 시 데이터 저장 여부 확인