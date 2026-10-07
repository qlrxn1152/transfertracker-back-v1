# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 4.19 ms | 7.62 ms | 28.58 ms | 35.83 | 0.00% | 5032 |
| Team Transfers List | 5.46 ms | 11.16 ms | 24.99 ms | 35.83 | 0.00% | 5032 |
| Team List | 8.12 ms | 15.06 ms | 29.38 ms | 35.83 | 0.00% | 5032 |
| Team Detail | 4.62 ms | 10.17 ms | 19.85 ms | 35.83 | 0.00% | 5032 |
| Players List | 6.47 ms | 10.44 ms | 16.92 ms | 35.83 | 0.00% | 5032 |
| Players - EPL Filter | 4.63 ms | 8.27 ms | 16.84 ms | 35.83 | 0.00% | 5032 |
| Players - Team Filter | 2.59 ms | 5.63 ms | 12.26 ms | 35.83 | 0.00% | 5032 |
| Players - EPL + Team Filter | 2.21 ms | 4.95 ms | 9.84 ms | 35.83 | 0.00% | 5032 |
