# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 5.20 ms | 15.56 ms | 23.69 ms | 35.79 | 0.00% | 5018 |
| Team Transfers List | 6.32 ms | 14.72 ms | 22.23 ms | 35.79 | 0.00% | 5018 |
| Team List | 9.05 ms | 18.53 ms | 24.45 ms | 35.79 | 0.00% | 5018 |
| Team Detail | 4.74 ms | 12.04 ms | 20.25 ms | 35.79 | 0.00% | 5018 |
| Players List | 6.79 ms | 11.55 ms | 18.18 ms | 35.79 | 0.00% | 5018 |
| Players - EPL Filter | 4.79 ms | 9.68 ms | 17.56 ms | 35.79 | 0.00% | 5018 |
| Players - Team Filter | 2.64 ms | 6.61 ms | 12.83 ms | 35.79 | 0.00% | 5018 |
| Players - EPL + Team Filter | 2.23 ms | 5.16 ms | 10.85 ms | 35.79 | 0.00% | 5018 |
