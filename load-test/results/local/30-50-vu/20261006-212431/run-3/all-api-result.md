# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 4.19 ms | 13.06 ms | 19.11 ms | 35.84 | 0.00% | 5037 |
| Team Transfers List | 5.59 ms | 12.75 ms | 19.33 ms | 35.84 | 0.00% | 5037 |
| Team List | 8.30 ms | 16.24 ms | 22.40 ms | 35.84 | 0.00% | 5037 |
| Team Detail | 4.32 ms | 9.67 ms | 15.08 ms | 35.84 | 0.00% | 5037 |
| Players List | 6.39 ms | 10.63 ms | 15.89 ms | 35.84 | 0.00% | 5037 |
| Players - EPL Filter | 4.34 ms | 8.15 ms | 14.33 ms | 35.84 | 0.00% | 5037 |
| Players - Team Filter | 2.32 ms | 5.29 ms | 9.62 ms | 35.84 | 0.00% | 5037 |
| Players - EPL + Team Filter | 1.97 ms | 4.25 ms | 8.49 ms | 35.84 | 0.00% | 5037 |
