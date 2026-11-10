![TransferTracker Architecture](docs/images/architecture.png)

## 데이터베이스 구조

![TransferTracker ERD](images/erd.png)

### 설계 원칙

- 선수의 현재 소속팀은 TeamPlayer에서 관리한다.
- 선수의 이적 이력은 Transfer에서 관리한다.
- TransferPost와 Team은 TransferPostTeam을 통해 다대다 관계를 형성한다.
- 데이터베이스 스키마 변경은 Flyway로 관리한다.