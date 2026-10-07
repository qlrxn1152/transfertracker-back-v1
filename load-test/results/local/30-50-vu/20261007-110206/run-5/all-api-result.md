# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.11 ms | 5.67 ms | 9.98 ms | 35.89 | 0.00% | 5048 |
| Team Transfers List | 4.82 ms | 8.59 ms | 13.15 ms | 35.89 | 0.00% | 5048 |
| Team List | 7.63 ms | 13.52 ms | 19.23 ms | 35.89 | 0.00% | 5048 |
| Team Detail | 4.25 ms | 8.81 ms | 14.34 ms | 35.89 | 0.00% | 5048 |
| Players List | 6.23 ms | 9.29 ms | 13.49 ms | 35.89 | 0.00% | 5048 |
| Players - EPL Filter | 4.17 ms | 7.14 ms | 11.59 ms | 35.89 | 0.00% | 5048 |
| Players - Team Filter | 2.23 ms | 4.86 ms | 8.66 ms | 35.89 | 0.00% | 5048 |
| Players - EPL + Team Filter | 1.92 ms | 4.04 ms | 7.47 ms | 35.89 | 0.00% | 5048 |
