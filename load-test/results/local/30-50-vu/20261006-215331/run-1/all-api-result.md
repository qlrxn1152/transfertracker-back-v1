# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 5.57 ms | 17.76 ms | 26.34 ms | 35.58 | 0.00% | 5001 |
| Team Transfers List | 6.63 ms | 15.79 ms | 22.58 ms | 35.58 | 0.00% | 5001 |
| Team List | 9.43 ms | 18.36 ms | 24.25 ms | 35.58 | 0.00% | 5001 |
| Team Detail | 5.59 ms | 12.71 ms | 18.22 ms | 35.58 | 0.00% | 5001 |
| Players List | 7.16 ms | 12.50 ms | 16.90 ms | 35.58 | 0.00% | 5001 |
| Players - EPL Filter | 5.26 ms | 10.25 ms | 16.64 ms | 35.58 | 0.00% | 5001 |
| Players - Team Filter | 3.07 ms | 7.11 ms | 15.71 ms | 35.58 | 0.00% | 5001 |
| Players - EPL + Team Filter | 2.36 ms | 5.51 ms | 9.82 ms | 35.58 | 0.00% | 5001 |
