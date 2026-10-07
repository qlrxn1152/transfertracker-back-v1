# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 5.73 ms | 17.20 ms | 33.22 ms | 35.47 | 0.00% | 4995 |
| Team Transfers List | 7.33 ms | 18.57 ms | 39.67 ms | 35.47 | 0.00% | 4995 |
| Team List | 10.03 ms | 21.63 ms | 35.60 ms | 35.47 | 0.00% | 4995 |
| Team Detail | 5.60 ms | 14.59 ms | 25.44 ms | 35.47 | 0.00% | 4995 |
| Players List | 7.04 ms | 14.09 ms | 23.24 ms | 35.47 | 0.00% | 4995 |
| Players - EPL Filter | 5.20 ms | 11.07 ms | 19.82 ms | 35.47 | 0.00% | 4995 |
| Players - Team Filter | 3.03 ms | 8.22 ms | 17.50 ms | 35.47 | 0.00% | 4995 |
| Players - EPL + Team Filter | 2.44 ms | 6.10 ms | 13.15 ms | 35.47 | 0.00% | 4995 |
