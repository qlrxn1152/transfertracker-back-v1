# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.02 ms | 5.40 ms | 9.81 ms | 35.97 | 0.00% | 5060 |
| Team Transfers List | 4.59 ms | 7.87 ms | 11.85 ms | 35.97 | 0.00% | 5060 |
| Team List | 7.58 ms | 12.14 ms | 17.62 ms | 35.97 | 0.00% | 5060 |
| Team Detail | 3.86 ms | 7.44 ms | 11.13 ms | 35.97 | 0.00% | 5060 |
| Players List | 5.89 ms | 8.36 ms | 10.94 ms | 35.97 | 0.00% | 5060 |
| Players - EPL Filter | 3.84 ms | 6.05 ms | 8.47 ms | 35.97 | 0.00% | 5060 |
| Players - Team Filter | 1.91 ms | 3.53 ms | 5.89 ms | 35.97 | 0.00% | 5060 |
| Players - EPL + Team Filter | 1.70 ms | 3.16 ms | 4.94 ms | 35.97 | 0.00% | 5060 |
