# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.47 ms | 6.32 ms | 18.78 ms | 35.87 | 0.00% | 5048 |
| Team Transfers List | 5.07 ms | 9.17 ms | 15.84 ms | 35.87 | 0.00% | 5048 |
| Team List | 7.80 ms | 13.66 ms | 18.62 ms | 35.87 | 0.00% | 5048 |
| Team Detail | 4.36 ms | 8.49 ms | 13.36 ms | 35.87 | 0.00% | 5048 |
| Players List | 6.15 ms | 9.12 ms | 12.16 ms | 35.87 | 0.00% | 5048 |
| Players - EPL Filter | 4.14 ms | 6.89 ms | 9.38 ms | 35.87 | 0.00% | 5048 |
| Players - Team Filter | 2.19 ms | 4.55 ms | 7.08 ms | 35.87 | 0.00% | 5048 |
| Players - EPL + Team Filter | 1.87 ms | 3.83 ms | 6.30 ms | 35.87 | 0.00% | 5048 |
