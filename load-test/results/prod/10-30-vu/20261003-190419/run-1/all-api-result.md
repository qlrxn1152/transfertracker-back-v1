# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 160.12 ms | 181.25 ms | 234.34 ms | 8.78 | 0.00% | 1237 |
| Team Transfers List | 160.77 ms | 184.71 ms | 257.62 ms | 8.78 | 0.00% | 1237 |
| Team List | 182.29 ms | 238.98 ms | 365.16 ms | 8.78 | 0.00% | 1237 |
| Team Detail | 157.68 ms | 179.30 ms | 251.01 ms | 8.78 | 0.00% | 1237 |
| Players List | 161.84 ms | 179.91 ms | 260.58 ms | 8.78 | 0.00% | 1237 |
| Players - EPL Filter | 157.83 ms | 177.11 ms | 258.60 ms | 8.78 | 0.00% | 1237 |
| Players - Team Filter | 155.62 ms | 175.94 ms | 248.69 ms | 8.78 | 0.00% | 1237 |
| Players - EPL + Team Filter | 155.52 ms | 176.84 ms | 247.08 ms | 8.78 | 0.00% | 1237 |
