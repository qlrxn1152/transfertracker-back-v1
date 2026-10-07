# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.81 ms | 10.46 ms | 15.83 ms | 35.84 | 0.00% | 5038 |
| Team Transfers List | 5.52 ms | 11.37 ms | 16.66 ms | 35.84 | 0.00% | 5038 |
| Team List | 8.27 ms | 15.52 ms | 21.67 ms | 35.84 | 0.00% | 5038 |
| Team Detail | 4.75 ms | 10.29 ms | 15.89 ms | 35.84 | 0.00% | 5038 |
| Players List | 6.54 ms | 10.30 ms | 15.80 ms | 35.84 | 0.00% | 5038 |
| Players - EPL Filter | 4.44 ms | 7.92 ms | 12.09 ms | 35.84 | 0.00% | 5038 |
| Players - Team Filter | 2.40 ms | 5.38 ms | 9.43 ms | 35.84 | 0.00% | 5038 |
| Players - EPL + Team Filter | 2.01 ms | 4.51 ms | 7.91 ms | 35.84 | 0.00% | 5038 |
