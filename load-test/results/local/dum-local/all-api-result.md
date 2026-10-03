# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 16.00 ms | 30.67 ms | 44.01 ms | 28.99 | 0.00% | 4931 |
| Team Transfers List | 9.93 ms | 24.74 ms | 36.90 ms | 28.99 | 0.00% | 4931 |
| Team List | 11.50 ms | 25.35 ms | 35.94 ms | 28.99 | 0.00% | 4931 |
| Team Detail | 7.20 ms | 17.90 ms | 27.01 ms | 28.99 | 0.00% | 4931 |
| Players List | 5.60 ms | 14.64 ms | 23.30 ms | 28.99 | 0.00% | 4931 |
| Players - EPL Filter | 4.43 ms | 11.38 ms | 20.45 ms | 28.99 | 0.00% | 4931 |
| Players - Team Filter | 3.38 ms | 8.86 ms | 15.38 ms | 28.99 | 0.00% | 4931 |
| Players - EPL + Team Filter | 2.80 ms | 7.44 ms | 13.31 ms | 28.99 | 0.00% | 4931 |
