# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 5.92 ms | 18.00 ms | 26.06 ms | 35.63 | 0.00% | 5003 |
| Team Transfers List | 6.80 ms | 15.14 ms | 21.77 ms | 35.63 | 0.00% | 5003 |
| Team List | 9.58 ms | 18.66 ms | 24.84 ms | 35.63 | 0.00% | 5003 |
| Team Detail | 5.33 ms | 12.74 ms | 17.60 ms | 35.63 | 0.00% | 5003 |
| Players List | 7.04 ms | 12.31 ms | 16.10 ms | 35.63 | 0.00% | 5003 |
| Players - EPL Filter | 5.12 ms | 10.24 ms | 14.52 ms | 35.63 | 0.00% | 5003 |
| Players - Team Filter | 2.87 ms | 7.36 ms | 10.89 ms | 35.63 | 0.00% | 5003 |
| Players - EPL + Team Filter | 2.35 ms | 5.95 ms | 10.46 ms | 35.63 | 0.00% | 5003 |
