# API 성능 테스트 결과

| API | AVG | P95 ( 95% )  | P99 ( 99% ) | RPS (1초당 완료한 요청 수 )| 실패율 | 요청 수 |
|:---|---:|---:|---:|---:|---:|---:|
| Transfers List | 157.01 ms | 170.38 ms | 266.50 ms | 8.95 | 0.00% | 1257 |
| Team Transfers List | 154.86 ms | 172.87 ms | 237.22 ms | 8.95 | 0.00% | 1257 |
| Team List | 174.81 ms | 205.44 ms | 305.18 ms | 8.95 | 0.00% | 1257 |
| Team Detail | 152.35 ms | 161.59 ms | 230.04 ms | 8.95 | 0.00% | 1257 |
| Players List | 155.91 ms | 166.42 ms | 234.01 ms | 8.95 | 0.00% | 1257 |
| Players - EPL Filter | 152.73 ms | 163.21 ms | 224.60 ms | 8.95 | 0.00% | 1257 |
| Players - Team Filter | 150.69 ms | 159.88 ms | 189.41 ms | 8.95 | 0.00% | 1257 |
| Players - EPL + Team Filter | 150.77 ms | 157.73 ms | 198.33 ms | 8.95 | 0.00% | 1257 |
