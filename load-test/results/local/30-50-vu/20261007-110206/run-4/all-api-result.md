# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.15 ms | 5.88 ms | 10.53 ms | 35.93 | 0.00% | 5056 |
| Team Transfers List | 4.72 ms | 8.38 ms | 13.71 ms | 35.93 | 0.00% | 5056 |
| Team List | 7.53 ms | 12.87 ms | 17.67 ms | 35.93 | 0.00% | 5056 |
| Team Detail | 4.07 ms | 8.40 ms | 12.97 ms | 35.93 | 0.00% | 5056 |
| Players List | 6.10 ms | 9.42 ms | 12.66 ms | 35.93 | 0.00% | 5056 |
| Players - EPL Filter | 4.11 ms | 6.96 ms | 10.22 ms | 35.93 | 0.00% | 5056 |
| Players - Team Filter | 2.16 ms | 4.56 ms | 7.93 ms | 35.93 | 0.00% | 5056 |
| Players - EPL + Team Filter | 1.89 ms | 3.83 ms | 7.08 ms | 35.93 | 0.00% | 5056 |
