# 테스트 설계 및 검증

## 테스트 전략

JUnit 5, Mockito, AssertJ를 활용하여 Service 단위 테스트를 작성합니다.
Repository 및 인증 관련 의존성을 Mock으로 격리하여 정상 흐름, 예외 처리, 반환 데이터와 상호작용을 검증합니다.
Spring Security 인가 정책은 `@WebMvcTest`와 MockMvc로 검증합니다.

## 1. MemberServiceImplTest

**테스트 범위:** 회원가입, 로그인 및 인증 실패 처리

| 테스트 메서드 | 검증 내용 |
|---|---|
| `singUp_success` | 정상 회원가입, 비밀번호 인코딩 및 저장 엔티티 상태 검증 |
| `signUp_duplicateLoginId` | 중복 ID 예외 및 회원 저장 방지 |
| `login_success` | 정상 로그인 및 JWT 발급 |
| `login_invalidPassword` | 비밀번호 불일치 시 로그인 실패 및 JWT 미발급 |
| `login_memberNotFound` | 존재하지 않는 회원의 로그인 실패 및 JWT 미발급 |

**주요 검증**
- `ArgumentCaptor`로 저장 대상 `Member`의 상태 검증
- `verify()`로 Repository 및 비밀번호 인코더 호출 확인
- `verifyNoInteractions()`로 로그인 실패 시 JWT 미발급 확인


---

## 2. MemberJwtAuthenticationConverterTest

**테스트 범위:** JWT Subject 기반 회원 조회 및 권한 변환

| 테스트 메서드 | 검증 내용 |
|---|---|
| `convert_adminSuccess` | ADMIN 회원에 `ROLE_ADMIN` 권한 부여 |
| `convert_userSuccess` | USER 회원에 `ROLE_USER` 권한 부여 |
| `convert_invalidSubject` | 숫자가 아닌 Subject에 대한 인증 실패 및 Repository 미호출 |
| `convert_notFoundMember` | 존재하지 않는 회원에 대한 인증 실패 |

**주요 검증**
- JWT Subject를 회원 ID로 변환하고 회원 등급에 따라 권한 부여
- `BadCredentialsException`과 예외 메시지 확인
- 실패 상황에서 Repository 호출 여부 검증

---


## 3. SecurityConfigTest

**테스트 범위:** 관리자 API 인증·인가 정책

| 테스트 메서드 | 검증 내용 |
|---|---|
| `external_unAuthorized` | 미인증 요청에 401 Unauthorized 반환 |
| `external_userForbidden` | `ROLE_USER` 요청에 403 Forbidden 반환 |
| `external_adminSuccess` | `ROLE_ADMIN` 요청에 200 OK 반환 |

**주요 검증**
- `@WebMvcTest`와 MockMvc를 이용한 HTTP 인가 테스트
- `@WithMockUser`로 사용자 권한별 접근 제어 확인

---


## 4. TeamQueryTxServiceTest

**테스트 범위:** 팀 단건 조회, 팀 목록, 리그별 조회 및 팀 상세 정보 조회

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getTeam_success` | 존재하는 팀의 ID·이름 DTO 반환 |
| `getTeam_notFound` | 팀 미존재 시 `NotFoundTeamException` 발생 |
| `getTeams_success` | 리그가 할당된 팀 목록의 DTO 변환 |
| `getTeams_empty` | 조회 결과가 없으면 빈 목록 반환 |
| `getLeagueTeams_success` | 지정한 리그의 팀 목록 반환 및 조회 인자 확인 |
| `getLeagueTeams_empty` | 지정한 리그에 팀이 없으면 빈 목록 반환 |
| `getTeamInfo_success` | 팀·선수 수·소속 선수·이적 게시물 정보를 조합한 DTO 검증 |
| `getTeamInfo_notFound` | 팀 미존재 시 예외 발생 및 다른 Repository 미호출 |

**주요 검증**
- `verify(times(1))`로 Repository 호출 횟수와 인자 검증
- `verifyNoInteractions()`로 팀 조회 실패 이후 불필요한 조회 차단 확인
- AssertJ `extracting()`, `containsExactlyInAnyOrder()`, `isEmpty()`로 컬렉션 검증
- 여러 Repository의 반환값을 최종 응답 DTO로 조합하는 로직 검증

## 향후 개선

- `TeamSyncTxService` 및 Player 도메인 Service 테스트 추가
- Testcontainers 기반 JPA / MySQL 통합 테스트 추가
- 실제 DB 조회 조건, 엔티티 매핑 및 쿼리 동작 검증
