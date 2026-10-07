# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 4.95 ms | 14.51 ms | 24.51 ms | 35.77 | 0.00% | 5013 |
| Team Transfers List | 6.24 ms | 14.65 ms | 24.25 ms | 35.77 | 0.00% | 5013 |
| Team List | 9.17 ms | 18.67 ms | 28.40 ms | 35.77 | 0.00% | 5013 |
| Team Detail | 4.82 ms | 11.54 ms | 20.89 ms | 35.77 | 0.00% | 5013 |
| Players List | 6.78 ms | 11.52 ms | 18.75 ms | 35.77 | 0.00% | 5013 |
| Players - EPL Filter | 4.77 ms | 9.13 ms | 17.01 ms | 35.77 | 0.00% | 5013 |
| Players - Team Filter | 2.66 ms | 6.59 ms | 13.92 ms | 35.77 | 0.00% | 5013 |
| Players - EPL + Team Filter | 2.24 ms | 5.38 ms | 11.46 ms | 35.77 | 0.00% | 5013 |
