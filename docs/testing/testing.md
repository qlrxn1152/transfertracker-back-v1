# 테스트 설계 및 검증

## 테스트 전략

서비스 단위 테스트에서 Mockito로 Repository와 인증 관련 의존성을 격리하여 비즈니스 로직을 검증합니다.

## 주요 검증 시나리오

| 기능 | 검증 내용 |
|---|---|
| 회원가입 | 정상 가입, 중복 ID 예외, 저장 방지 |
| 로그인 | 정상 로그인 및 JWT 발급 |
| 로그인 실패 | 비밀번호 불일치, 존재하지 않는 회원, JWT 미발급 |

## 기술적 판단

- `@SpringBootTest` 대신 Mockito를 활용해 Spring Context 없이 서비스 로직 검증
- `ArgumentCaptor`로 저장 대상 엔티티의 상태 검증
- `verifyNoInteractions()`로 로그인 실패 시 JWT 발급이 실행되지 않음을 검증

## 향후 개선

Testcontainers를 활용한 JPA 및 MySQL 통합 테스트 추가