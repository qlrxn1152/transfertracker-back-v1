# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.02 ms | 5.67 ms | 10.71 ms | 35.92 | 0.00% | 5056 |
| Team Transfers List | 4.67 ms | 8.42 ms | 15.04 ms | 35.92 | 0.00% | 5056 |
| Team List | 7.46 ms | 12.89 ms | 18.53 ms | 35.92 | 0.00% | 5056 |
| Team Detail | 3.94 ms | 8.03 ms | 13.77 ms | 35.92 | 0.00% | 5056 |
| Players List | 5.98 ms | 9.07 ms | 12.62 ms | 35.92 | 0.00% | 5056 |
| Players - EPL Filter | 4.00 ms | 6.97 ms | 10.06 ms | 35.92 | 0.00% | 5056 |
| Players - Team Filter | 2.06 ms | 4.38 ms | 7.61 ms | 35.92 | 0.00% | 5056 |
| Players - EPL + Team Filter | 1.83 ms | 3.85 ms | 7.34 ms | 35.92 | 0.00% | 5056 |
