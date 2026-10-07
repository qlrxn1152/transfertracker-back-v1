# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 5.52 ms | 17.39 ms | 26.25 ms | 35.57 | 0.00% | 5013 |
| Team Transfers List | 6.50 ms | 15.51 ms | 22.99 ms | 35.57 | 0.00% | 5013 |
| Team List | 9.38 ms | 19.01 ms | 27.26 ms | 35.57 | 0.00% | 5013 |
| Team Detail | 5.11 ms | 14.12 ms | 22.95 ms | 35.57 | 0.00% | 5013 |
| Players List | 6.95 ms | 12.76 ms | 20.23 ms | 35.57 | 0.00% | 5013 |
| Players - EPL Filter | 4.95 ms | 10.54 ms | 17.42 ms | 35.57 | 0.00% | 5013 |
| Players - Team Filter | 2.73 ms | 7.11 ms | 12.68 ms | 35.57 | 0.00% | 5013 |
| Players - EPL + Team Filter | 2.26 ms | 5.61 ms | 10.49 ms | 35.57 | 0.00% | 5013 |
