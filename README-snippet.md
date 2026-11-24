## 9. 트러블슈팅 및 성능 최적화

### 01. 게시물 100만 건 조회 API 성능 최적화

MySQL 실행계획 분석을 통해 Full Table Scan 및 Filesort를 확인하고, InnoDB의 Index Extension을 활용한 단일 인덱스를 적용했습니다.

| 측정 지표 | 개선 전 | 개선 후 |
|---|---:|---:|
| p95 응답 시간 | 10.37s | **53.57ms (99.48% 감소)** |
| 목록 조회 SQL 실행 시간 | 340ms | 0.583ms |
| 실제 처리량 | 20.41 RPS | 49.98 RPS |
| Dropped Iterations | 1,587 | 0 |

*게시물 100만 건, 목표 50 RPS 로컬 부하 테스트 측정 결과.*

- [상세 트러블슈팅 문서](../../Downloads/veyro-docs-current/docs/troubleshooting/01-post-index-optimization.md)
- [전체 트러블슈팅 목록](../../Downloads/veyro-docs-current/docs/troubleshooting/README.md)
- [Velog — VEYRO 성능 개선기 #1: 게시물 100만 건 조회, p95 응답 시간 99% 단축](https://velog.io/@qlrxn115/VEYRO-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0%EA%B8%B0-1-%EA%B2%8C%EC%8B%9C%EB%AC%BC-100%EB%A7%8C%EA%B1%B4-%EC%A1%B0%ED%9A%8C-MySQL-%EC%9D%B8%EB%8D%B1%EC%8A%A4%EB%A5%BC-%ED%86%B5%ED%95%B4-p95-%EC%9D%91%EB%8B%B5-%EC%8B%9C%EA%B0%84-99-%EB%8B%A8%EC%B6%95)
