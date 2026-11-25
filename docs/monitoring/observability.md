# VEYRO Observability

## 구성

로컬 및 성능 환경의 Spring Boot 애플리케이션은 호스트에서 실행된다. 운영 환경은 Railway의 공개 HTTPS 도메인에서 Prometheus가 `/actuator/prometheus`를 수집한다.

- **Metrics**: Micrometer → `/actuator/prometheus` → Prometheus → Grafana
- **Logs (local/performance)**: Logback file (`logs/veyro-local.log`, `logs/veyro-performance.log`) → Alloy → Loki → Grafana
- **Logs (prod)**: Railway Log Explorer. Railway는 현재 log drain을 제공하지 않으므로, 별도 Loki 호스팅 또는 애플리케이션 내부 forwarder를 도입하기 전까지는 Grafana Loki로 운영 로그를 중복 전송하지 않는다.
- **Alerting**: Grafana Alerting rule provisioning. 연락 채널은 환경변수로 별도 설정한다.

`requestId`, `memberId`, `postId`, JWT, API key, FCM token은 Prometheus/Loki label로 사용하지 않는다. 전자는 로그 본문 검색에만 남기며, 후자는 어떤 로그에도 남기지 않는다.

## 실행

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

Grafana는 `http://localhost:3000`, Prometheus는 `http://localhost:9090`에서만 호스트에 열려 있다. Loki와 Alloy는 Docker 네트워크 안에서만 통신한다.

Grafana provisioning은 기존 `grafana_data` named volume을 삭제하지 않는다. 기존 UI 대시보드와 provisioned 대시보드가 모두 남을 수 있으므로, 새 대시보드를 검증한 뒤 기존 대시보드 삭제 여부는 UI에서 직접 결정한다.

## 대시보드

- `VEYRO - JVM Monitoring`
- `VEYRO - Service Monitoring`
- `VEYRO - Business Monitoring`
- `VEYRO - External API Monitoring`
- `VEYRO - Log Monitoring`

Prometheus 기반 대시보드는 `environment` 변수로 `local`, `performance`, `prod`를 분리한다. `veyro.post.sync.status.refresh.success=0` 또는 `last_success_epoch_seconds`가 오래된 경우에는 0건을 정상 상태로 해석하지 않는다.

## 환경별 확인 방법

| 환경 | 애플리케이션 실행 | Grafana에서 보기 | 로그 확인 |
| --- | --- | --- | --- |
| local | `./gradlew bootRun --args='--spring.profiles.active=local'` | 각 VEYRO 대시보드 상단 **Environment**에서 `local` 선택 | **VEYRO - Log Monitoring**에서 `local` 선택 |
| performance | `./gradlew bootRun --args='--spring.profiles.active=performance'` | **Environment**에서 `performance` 선택 | **VEYRO - Log Monitoring**에서 `performance` 선택 |
| prod | Railway가 `prod` 프로필로 실행 | **Environment**에서 `prod` 선택 | Railway 프로젝트 → `transfertracker-api` → **Observability / Logs** |

운영 메트릭을 처음 보려면 Railway에 이 변경을 배포한 뒤 Prometheus가 한 번 수집될 때까지 최대 15초 기다린다. `VEYRO - Service Monitoring`의 **Server Status**가 `UP`이면 연결이 완료된 것이다.

## 로그 검색 예시

Grafana의 **VEYRO - Log Monitoring**에서 환경을 선택한 뒤 텍스트 검색을 사용한다.

- requestId: `requestId=<값>`
- memberId: `memberId=<값>`
- HTTP access: `HTTP 요청 처리완료`
- 오류: Level `ERROR` 또는 `ERROR` 텍스트

Alloy는 현재 활성 로그 파일만 tail한다. `logs/archive/*.gz`는 수집하지 않으므로 재시작이나 rolling 시 압축된 과거 로그가 다시 적재되지 않는다. Loki 보관 기간은 14일이며 compactor가 만료 데이터를 삭제한다.

## Grafana Alerting 적용

경보 규칙 정의는 `monitoring/grafana/provisioning/alerting/veyro-alert-rules.yml`에 보관한다. 기존 `grafana_data` volume을 사용하는 로컬 환경에서는 과거 UI 규칙의 GUID와 provisioning rule이 충돌할 수 있으므로, 이 파일은 기본 Compose 실행에서 자동 mount하지 않는다. Grafana UI의 **Alerting → Alert rules → Import**로 검증 환경에 먼저 가져온 뒤 적용한다. 이 방식은 기존 규칙이나 Grafana volume을 삭제·덮어쓰지 않는다.

## Discord 알림 채널 설정

Webhook URL은 저장소에 저장하지 않는다. Grafana UI에서 **Alerting → Contact points → New contact point → Discord**를 선택하고 `DISCORD_WEBHOOK_URL` 값으로 만든 Webhook URL을 입력한다. 운영용 contact point를 만든 뒤 notification policy에서 `severity=critical`을 별도 라우팅한다.

로컬/성능 실험 중 불필요한 외부 알림을 피하려면 contact point를 연결하지 않거나, policy에서 `environment=prod` 라벨만 전송 대상으로 둔다.
