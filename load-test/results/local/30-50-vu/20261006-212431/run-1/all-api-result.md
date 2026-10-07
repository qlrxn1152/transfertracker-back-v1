# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 4.58 ms | 13.90 ms | 19.67 ms | 35.77 | 0.00% | 5030 |
| Team Transfers List | 5.89 ms | 13.47 ms | 19.67 ms | 35.77 | 0.00% | 5030 |
| Team List | 9.01 ms | 17.30 ms | 25.68 ms | 35.77 | 0.00% | 5030 |
| Team Detail | 4.70 ms | 10.18 ms | 17.13 ms | 35.77 | 0.00% | 5030 |
| Players List | 6.46 ms | 10.37 ms | 15.29 ms | 35.77 | 0.00% | 5030 |
| Players - EPL Filter | 4.54 ms | 8.42 ms | 11.85 ms | 35.77 | 0.00% | 5030 |
| Players - Team Filter | 2.49 ms | 5.54 ms | 9.57 ms | 35.77 | 0.00% | 5030 |
| Players - EPL + Team Filter | 2.11 ms | 4.73 ms | 7.92 ms | 35.77 | 0.00% | 5030 |
