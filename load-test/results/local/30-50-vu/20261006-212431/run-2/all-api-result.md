# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 6.26 ms | 18.22 ms | 25.77 ms | 35.51 | 0.00% | 4996 |
| Team Transfers List | 7.11 ms | 16.20 ms | 23.91 ms | 35.51 | 0.00% | 4996 |
| Team List | 10.18 ms | 20.10 ms | 27.02 ms | 35.51 | 0.00% | 4996 |
| Team Detail | 5.19 ms | 12.92 ms | 23.47 ms | 35.51 | 0.00% | 4996 |
| Players List | 7.31 ms | 14.01 ms | 21.52 ms | 35.51 | 0.00% | 4996 |
| Players - EPL Filter | 5.37 ms | 11.60 ms | 17.65 ms | 35.51 | 0.00% | 4996 |
| Players - Team Filter | 3.00 ms | 7.70 ms | 14.93 ms | 35.51 | 0.00% | 4996 |
| Players - EPL + Team Filter | 2.46 ms | 6.37 ms | 13.74 ms | 35.51 | 0.00% | 4996 |
