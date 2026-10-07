# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.16 ms | 5.80 ms | 10.31 ms | 35.85 | 0.00% | 5051 |
| Team Transfers List | 5.03 ms | 9.11 ms | 13.80 ms | 35.85 | 0.00% | 5051 |
| Team List | 7.65 ms | 12.73 ms | 18.18 ms | 35.85 | 0.00% | 5051 |
| Team Detail | 4.54 ms | 9.27 ms | 13.42 ms | 35.85 | 0.00% | 5051 |
| Players List | 6.40 ms | 9.77 ms | 13.08 ms | 35.85 | 0.00% | 5051 |
| Players - EPL Filter | 4.40 ms | 7.65 ms | 10.80 ms | 35.85 | 0.00% | 5051 |
| Players - Team Filter | 2.28 ms | 5.06 ms | 7.95 ms | 35.85 | 0.00% | 5051 |
| Players - EPL + Team Filter | 1.95 ms | 4.12 ms | 6.75 ms | 35.85 | 0.00% | 5051 |
