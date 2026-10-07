# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 3.57 ms | 7.35 ms | 16.04 ms | 35.74 | 0.00% | 5039 |
| Team Transfers List | 5.39 ms | 10.28 ms | 16.77 ms | 35.74 | 0.00% | 5039 |
| Team List | 8.32 ms | 14.78 ms | 20.66 ms | 35.74 | 0.00% | 5039 |
| Team Detail | 4.62 ms | 9.22 ms | 13.85 ms | 35.74 | 0.00% | 5039 |
| Players List | 6.41 ms | 9.90 ms | 13.90 ms | 35.74 | 0.00% | 5039 |
| Players - EPL Filter | 4.41 ms | 7.73 ms | 11.04 ms | 35.74 | 0.00% | 5039 |
| Players - Team Filter | 2.40 ms | 5.08 ms | 8.74 ms | 35.74 | 0.00% | 5039 |
| Players - EPL + Team Filter | 2.04 ms | 4.32 ms | 7.70 ms | 35.74 | 0.00% | 5039 |
