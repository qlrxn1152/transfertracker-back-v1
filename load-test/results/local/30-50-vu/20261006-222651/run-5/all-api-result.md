# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 4.84 ms | 13.59 ms | 21.85 ms | 35.60 | 0.00% | 5011 |
| Team Transfers List | 6.44 ms | 14.41 ms | 20.67 ms | 35.60 | 0.00% | 5011 |
| Team List | 9.56 ms | 18.47 ms | 23.75 ms | 35.60 | 0.00% | 5011 |
| Team Detail | 5.44 ms | 12.64 ms | 18.76 ms | 35.60 | 0.00% | 5011 |
| Players List | 6.87 ms | 12.10 ms | 17.31 ms | 35.60 | 0.00% | 5011 |
| Players - EPL Filter | 5.07 ms | 10.33 ms | 15.58 ms | 35.60 | 0.00% | 5011 |
| Players - Team Filter | 2.84 ms | 6.95 ms | 11.59 ms | 35.60 | 0.00% | 5011 |
| Players - EPL + Team Filter | 2.40 ms | 5.74 ms | 10.37 ms | 35.60 | 0.00% | 5011 |
