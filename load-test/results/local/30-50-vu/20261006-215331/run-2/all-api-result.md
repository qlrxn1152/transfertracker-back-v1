# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 5.62 ms | 15.13 ms | 25.73 ms | 35.60 | 0.00% | 5006 |
| Team Transfers List | 6.68 ms | 15.39 ms | 22.48 ms | 35.60 | 0.00% | 5006 |
| Team List | 9.85 ms | 19.31 ms | 27.39 ms | 35.60 | 0.00% | 5006 |
| Team Detail | 5.03 ms | 12.37 ms | 22.35 ms | 35.60 | 0.00% | 5006 |
| Players List | 6.73 ms | 12.81 ms | 20.08 ms | 35.60 | 0.00% | 5006 |
| Players - EPL Filter | 4.96 ms | 10.56 ms | 17.97 ms | 35.60 | 0.00% | 5006 |
| Players - Team Filter | 2.96 ms | 7.62 ms | 14.70 ms | 35.60 | 0.00% | 5006 |
| Players - EPL + Team Filter | 2.50 ms | 6.47 ms | 14.36 ms | 35.60 | 0.00% | 5006 |
