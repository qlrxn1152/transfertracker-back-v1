# 트러블슈팅 및 성능 최적화

프로젝트에서 발견한 기술적 문제를 분석하고 해결한 과정을 기록합니다. 문제 상황, 원인, 대안 검토, 적용 방법, 검증 결과 및 한계를 중심으로 문서화합니다.

## 성능 최적화

| 번호 | 주제 | 핵심 결과 | 상세 문서 | 기술 블로그 |
|---|---|---|---|---|
| 01 | 게시물 최신순 조회 인덱스 최적화 | p95 10.37s → 53.57ms (99.48% 감소), Dropped 1,587 → 0 | [문서 읽기](01-post-index-optimization.md) | [Velog 읽기](https://velog.io/@qlrxn115/VEYRO-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0%EA%B8%B0-1-%EA%B2%8C%EC%8B%9C%EB%AC%BC-100%EB%A7%8C%EA%B1%B4-%EC%A1%B0%ED%9A%8C-MySQL-%EC%9D%B8%EB%8D%B1%EC%8A%A4%EB%A5%BC-%ED%86%B5%ED%95%B4-p95-%EC%9D%91%EB%8B%B5-%EC%8B%9C%EA%B0%84-99-%EB%8B%A8%EC%B6%95) |

> 성능 수치는 게시물 100만 건, 목표 50 RPS 로컬 부하 테스트의 측정 결과이다.

## 문서 작성 기준

- Problem: 재현 조건과 증상
- Cause: 로그, 실행계획 및 측정 근거
- Solution: 검토한 대안, 선택 이유 및 변경 코드
- Verification: 개선 전후 비교 및 재현 방법
- Lessons Learned: 트레이드오프, 한계 및 후속 과제

[프로젝트 README로 돌아가기](../../README.md)
