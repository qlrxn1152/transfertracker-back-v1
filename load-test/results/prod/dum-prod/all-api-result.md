# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 158.69 ms | 181.91 ms | 259.82 ms | 6.25 | 0.00% | 575 |
| Team Transfers List | 158.15 ms | 180.87 ms | 250.87 ms | 6.25 | 0.00% | 575 |
| Team List | 184.06 ms | 246.78 ms | 395.31 ms | 6.25 | 0.00% | 575 |
| Team Detail | 155.30 ms | 177.60 ms | 250.91 ms | 6.25 | 0.00% | 575 |
| Players List | 158.34 ms | 181.64 ms | 258.00 ms | 6.25 | 0.00% | 575 |
| Players - EPL Filter | 156.45 ms | 178.63 ms | 255.69 ms | 6.25 | 0.00% | 575 |
| Players - Team Filter | 153.51 ms | 175.91 ms | 192.00 ms | 6.25 | 0.00% | 575 |
| Players - EPL + Team Filter | 155.17 ms | 177.30 ms | 257.85 ms | 6.25 | 0.00% | 575 |
