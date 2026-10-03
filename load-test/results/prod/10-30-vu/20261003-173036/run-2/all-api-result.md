# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 156.88 ms | 179.80 ms | 217.04 ms | 8.88 | 0.00% | 1251 |
| Team Transfers List | 157.54 ms | 182.15 ms | 327.99 ms | 8.88 | 0.00% | 1251 |
| Team List | 176.79 ms | 212.49 ms | 327.79 ms | 8.88 | 0.00% | 1251 |
| Team Detail | 153.57 ms | 176.07 ms | 221.94 ms | 8.88 | 0.00% | 1251 |
| Players List | 156.88 ms | 180.00 ms | 197.68 ms | 8.88 | 0.00% | 1251 |
| Players - EPL Filter | 153.57 ms | 176.62 ms | 190.73 ms | 8.88 | 0.00% | 1251 |
| Players - Team Filter | 152.36 ms | 174.98 ms | 206.02 ms | 8.88 | 0.00% | 1251 |
| Players - EPL + Team Filter | 152.11 ms | 174.58 ms | 200.64 ms | 8.88 | 0.00% | 1251 |
