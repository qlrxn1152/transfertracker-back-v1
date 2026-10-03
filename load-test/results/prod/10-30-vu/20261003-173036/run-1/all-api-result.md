# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 158.03 ms | 181.28 ms | 265.65 ms | 8.81 | 0.00% | 1251 |
| Team Transfers List | 156.89 ms | 180.70 ms | 245.56 ms | 8.81 | 0.00% | 1251 |
| Team List | 178.64 ms | 226.52 ms | 364.81 ms | 8.81 | 0.00% | 1251 |
| Team Detail | 154.84 ms | 177.00 ms | 248.89 ms | 8.81 | 0.00% | 1251 |
| Players List | 157.61 ms | 179.54 ms | 213.17 ms | 8.81 | 0.00% | 1251 |
| Players - EPL Filter | 155.21 ms | 176.97 ms | 252.06 ms | 8.81 | 0.00% | 1251 |
| Players - Team Filter | 153.71 ms | 176.29 ms | 252.10 ms | 8.81 | 0.00% | 1251 |
| Players - EPL + Team Filter | 152.63 ms | 174.87 ms | 218.48 ms | 8.81 | 0.00% | 1251 |
