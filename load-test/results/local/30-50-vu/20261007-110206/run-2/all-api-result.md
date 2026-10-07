# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.63 ms | 7.38 ms | 23.03 ms | 36.01 | 0.00% | 5050 |
| Team Transfers List | 5.06 ms | 9.86 ms | 23.13 ms | 36.01 | 0.00% | 5050 |
| Team List | 7.56 ms | 14.42 ms | 29.31 ms | 36.01 | 0.00% | 5050 |
| Team Detail | 4.14 ms | 9.65 ms | 17.89 ms | 36.01 | 0.00% | 5050 |
| Players List | 6.33 ms | 9.95 ms | 18.75 ms | 36.01 | 0.00% | 5050 |
| Players - EPL Filter | 4.24 ms | 7.79 ms | 14.05 ms | 36.01 | 0.00% | 5050 |
| Players - Team Filter | 2.32 ms | 5.38 ms | 10.65 ms | 36.01 | 0.00% | 5050 |
| Players - EPL + Team Filter | 2.07 ms | 4.76 ms | 9.41 ms | 36.01 | 0.00% | 5050 |
