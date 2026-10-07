# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.41 ms | 6.51 ms | 13.67 ms | 35.85 | 0.00% | 5044 |
| Team Transfers List | 4.99 ms | 9.34 ms | 15.38 ms | 35.85 | 0.00% | 5044 |
| Team List | 7.84 ms | 14.07 ms | 19.05 ms | 35.85 | 0.00% | 5044 |
| Team Detail | 4.32 ms | 9.33 ms | 14.67 ms | 35.85 | 0.00% | 5044 |
| Players List | 6.30 ms | 9.58 ms | 14.20 ms | 35.85 | 0.00% | 5044 |
| Players - EPL Filter | 4.28 ms | 7.46 ms | 11.20 ms | 35.85 | 0.00% | 5044 |
| Players - Team Filter | 2.30 ms | 5.09 ms | 8.49 ms | 35.85 | 0.00% | 5044 |
| Players - EPL + Team Filter | 2.04 ms | 4.45 ms | 8.50 ms | 35.85 | 0.00% | 5044 |
