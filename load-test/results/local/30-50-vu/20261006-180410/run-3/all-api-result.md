# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.26 ms | 6.17 ms | 13.08 ms | 35.84 | 0.00% | 5045 |
| Team Transfers List | 4.98 ms | 9.30 ms | 14.56 ms | 35.84 | 0.00% | 5045 |
| Team List | 7.77 ms | 13.35 ms | 18.63 ms | 35.84 | 0.00% | 5045 |
| Team Detail | 4.32 ms | 8.96 ms | 12.94 ms | 35.84 | 0.00% | 5045 |
| Players List | 6.24 ms | 9.69 ms | 13.32 ms | 35.84 | 0.00% | 5045 |
| Players - EPL Filter | 4.23 ms | 7.34 ms | 10.57 ms | 35.84 | 0.00% | 5045 |
| Players - Team Filter | 2.24 ms | 4.85 ms | 7.42 ms | 35.84 | 0.00% | 5045 |
| Players - EPL + Team Filter | 1.94 ms | 4.15 ms | 6.52 ms | 35.84 | 0.00% | 5045 |
