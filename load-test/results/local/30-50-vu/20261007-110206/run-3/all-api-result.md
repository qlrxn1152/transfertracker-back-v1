# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.71 ms | 7.89 ms | 21.12 ms | 35.95 | 0.00% | 5042 |
| Team Transfers List | 5.15 ms | 9.85 ms | 15.89 ms | 35.95 | 0.00% | 5042 |
| Team List | 8.06 ms | 14.61 ms | 23.15 ms | 35.95 | 0.00% | 5042 |
| Team Detail | 4.71 ms | 10.03 ms | 20.33 ms | 35.95 | 0.00% | 5042 |
| Players List | 6.46 ms | 10.05 ms | 15.96 ms | 35.95 | 0.00% | 5042 |
| Players - EPL Filter | 4.44 ms | 7.66 ms | 12.91 ms | 35.95 | 0.00% | 5042 |
| Players - Team Filter | 2.31 ms | 4.94 ms | 8.18 ms | 35.95 | 0.00% | 5042 |
| Players - EPL + Team Filter | 1.98 ms | 4.10 ms | 7.58 ms | 35.95 | 0.00% | 5042 |
