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

---

## 5. TeamSyncTxServiceTest

**테스트 범위:** 외부 팀 데이터 기반 기존 팀 조회, 신규 팀 생성 및 리그 할당

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getOrCreateTeam_success` | 기존 팀 조회 및 중복 저장 방지 |
| `getOrCreateTeam_success_notExistTeam` | 팀 미존재 시 생성·저장 및 응답 DTO 검증 |
| `getOrCreateTeamAndAssignLeague_success` | 기존 팀에 지정 리그 할당 및 중복 저장 방지 |
| `getOrCreateTeamAndAssignLeague_success_notFoundTeam` | 신규 팀 생성·저장 후 리그 할당 및 DTO 검증 |

**주요 검증**
- `given().willReturn()`, `willAnswer()`로 Repository 조회·저장 동작 모방
- `verify(never())`로 기존 팀의 불필요한 중복 저장 방지 확인
- `ArgumentCaptor`로 신규 저장 대상 Team의 이름 및 API ID 검증
- `ReflectionTestUtils`로 Mock 저장 과정의 ID 할당을 모방하고, 리그 변경 상태 확인


---
## 6. PlayerQueryTxServiceTest

**테스트 범위:** 선수 단건 조회, FA 선수 조회, 선수 목록 조회 및 Slice 기반 페이징 처리

**테스트 대상:** `PlayerQueryTxService`

**Mock 의존성:** `PlayerRepository`, `TeamPlayerRepository`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getPlayer_success_teamPlayer` | 팀에 소속된 선수 조회 시 선수 ID, 이름 및 소속팀 정보가 정상 반환되는지 검증 |
| `getPlayer_success_freeAgent` | 소속팀이 없는 FA 선수 조회 시 선수 정보가 반환되고 팀 이름은 `null`인지 검증 |
| `getPlayer_notFound` | 존재하지 않는 선수 ID 조회 시 `NotFoundPlayerException`이 발생하고 `TeamPlayerRepository`가 호출되지 않는지 검증 |
| `getPlayers_success` | 선수 목록 조회 시 DTO 목록이 정상 변환되고 첫 페이지의 `hasNext=true`, `hasPrevious=false`가 반환되는지 검증 |
| `getPlayers_empty` | 조회 결과가 없을 때 빈 목록과 `hasNext=false`, `hasPrevious=false`가 반환되는지 검증 |
| `getPlayers_lastPage` | 마지막 페이지 조회 시 선수 목록이 반환되고 `hasNext=false`, `hasPrevious=true`가 반환되는지 검증 |

### 주요 검증 사항

- `PlayerRepository.findById()`로 선수 존재 여부 확인
- `TeamPlayerRepository.findByPlayerId()`로 선수의 팀 소속 여부 조회
- 팀 소속 선수와 FA 선수에 따른 응답 DTO 생성
- 존재하지 않는 선수 조회 시 예외 처리 및 불필요한 Repository 호출 방지
- `Slice<TeamPlayer>`의 데이터와 페이징 정보를 응답 DTO로 변환
- 첫 페이지, 마지막 페이지, 빈 결과에 대한 페이징 동작 검증

---

## 7. PlayerQueryOrchestrationServiceTest

**테스트 범위:** 선수 조회 요청의 입력값 검증, 검색어 정규화, 페이징 및 정렬 조건 생성, TxService 호출 위임

**테스트 대상:** `PlayerQueryOrchestrationService`

**Mock 의존성:** `PlayerQueryTxService`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getPlayer_success_FreeAgent` | 정상 선수 ID 조회 시 TxService가 반환한 FA 선수 DTO를 동일한 객체로 반환하고 조회 메서드를 한 번 호출하는지 검증 |
| `getPlayer_success_teamPlayer` | 정상 선수 ID 조회 시 TxService가 반환한 팀 소속 선수 DTO를 동일한 객체로 반환하고 조회 메서드를 한 번 호출하는지 검증 |
| `getPlayer_invalidId` | 선수 ID가 `null`이면 `InvalidPlayerIdException`이 발생하고 TxService가 호출되지 않는지 검증 |
| `getPlayers_success` | 검색어 앞뒤 공백 제거, 리그·팀 ID 전달, 페이지 번호·크기 및 정렬 조건 생성, 반환 DTO의 동일성 검증 |
| `getPlayers_invalidPage` | 페이지 번호가 음수이면 `InvalidPlayerSearchPageValueException`이 발생하고 TxService가 호출되지 않는지 검증 |
| `getPlayers_nullKeyword` | 검색어가 `null`이면 빈 문자열 `""`로 변환하여 TxService에 전달하는지 검증 |

### 주요 검증 사항

- 선수 ID에 대한 `null` 검증
- 음수 페이지 번호에 대한 예외 처리
- 검색어 정규화: `trim()` 및 `null` → `""`
- `PageRequest`의 페이지 번호와 기본 페이지 크기 `50` 검증
- `player.playerName ASC`, `player.id ASC` 순서의 정렬 조건 검증
- `ArgumentCaptor<Pageable>`을 활용한 실제 전달 인자 확인
- Mockito의 `verify()`, `eq()`, `isNull()`을 활용한 TxService 호출 인자 검증
- `verifyNoInteractions()`를 활용한 유효하지 않은 요청의 TxService 호출 차단 확인
- `isSameAs()`를 활용한 TxService 응답 객체의 동일성 검증


---

## 8. TeamPlayerQueryTxServiceTest

**테스트 범위:** 팀 소속 선수 목록 조회, 빈 목록 반환, 팀 미존재 예외 처리

**테스트 대상:** `TeamPlayerQueryTxService`

**Mock 의존성:** `TeamRepository`, `TeamPlayerRepository`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getTeamPlayers_success` | 팀에 소속된 선수 3명의 정보가 응답 DTO로 정상 변환되는지 검증 |
| `getTeamPlayers_empty` | 팀은 존재하지만 소속 선수가 없으면 빈 목록을 반환하는지 검증 |
| `getTeamPlayers_notFoundTeam` | 팀이 존재하지 않으면 `NotFoundTeamException`이 발생하고 TeamPlayerRepository가 호출되지 않는지 검증 |

### 주요 검증 사항

- `TeamRepository.findById()`를 이용한 팀 존재 여부 확인
- `TeamPlayerRepository.findAllByTeamIdWithLazyEntity()` 조회 결과의 DTO 변환
- AssertJ `extracting()`, `containsExactlyInAnyOrder()`, `isEmpty()`를 활용한 목록 검증
- `verifyNoInteractions()`를 이용한 불필요한 Repository 호출 방지

---

## 9. TeamPlayerQueryOrchestrationServiceTest

**테스트 범위:** 팀 ID 입력값 검증 및 TxService 조회 위임

**테스트 대상:** `TeamPlayerQueryOrchestrationService`

**Mock 의존성:** `TeamPlayerService`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getTeamPlayers_success` | 정상적인 팀 ID를 TxService에 전달하고 반환 DTO를 동일한 객체로 반환하는지 검증 |
| `getTeamPlayers_invalidTeamId` | 팀 ID가 `null`이면 `InvalidTeamIdException`이 발생하고 TxService가 호출되지 않는지 검증 |

### 주요 검증 사항

- 팀 ID에 대한 `null` 검증
- `isSameAs()`를 활용한 TxService 반환 객체의 동일성 검증
- `verify(times(1))`을 이용한 조회 메서드 호출 검증
- `verifyNoInteractions()`를 이용한 유효하지 않은 요청의 TxService 호출 차단 검증

---

## 10. TeamPlayerSyncTxServiceTest

**테스트 범위:** 외부 API 데이터 기반 Player, Team, TeamPlayer의 조회·생성 및 기존 데이터 재사용

**테스트 대상:** `TeamPlayerSyncTxService`

**Mock 의존성:** `PlayerRepository`, `TeamRepository`, `TeamPlayerRepository`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getOrCreateTeamPlayerResponse_success_existing` | 선수·팀·소속 관계가 모두 존재하면 기존 엔티티를 재사용하고 저장 메서드를 호출하지 않는지 검증 |
| `getOrCreateTeamPlayerResponse_success_newPlayer` | 기존 팀에 신규 선수를 생성하고 소속 관계를 저장하는지 검증 |
| `getOrCreateTeamPlayerResponse_success_newTeam` | 기존 선수에 신규 팀을 생성하고 소속 관계를 저장하는지 검증 |
| `getOrCreateTeamPlayerResponse_success_newAll` | 선수·팀·소속 관계가 모두 없으면 각각 생성하고 올바르게 연결하는지 검증 |
| `getOrCreateTeamPlayerResponse_success_newRelation` | 기존 선수와 팀 사이에 소속 관계만 없는 경우 TeamPlayer만 저장하는지 검증 |

### 주요 검증 사항

- Jackson `JsonMapper`와 `JsonNode`를 이용한 외부 API 데이터 구성
- API Football ID 기반 기존 Player·Team 조회
- `Optional.orElseGet()`에 따른 기존 엔티티 재사용 및 신규 저장 분기 검증
- Mockito `willAnswer()`와 `ReflectionTestUtils`를 이용한 신규 엔티티의 DB PK 할당 모방
- `ArgumentCaptor`를 이용한 Repository 저장 대상 엔티티 검증
- 신규 엔티티의 이름과 API Football ID 검증
- `isSameAs()`를 이용한 TeamPlayer의 Player·Team 연관관계 검증
- `verify(never())`를 이용한 기존 엔티티의 불필요한 중복 저장 방지
- 최종 응답 DTO의 Player ID와 Team ID 검증

---

## 11. TeamPlayerSyncOrchestrationServiceTest

**테스트 범위:** 외부 API 호출, JSON 응답 처리, 선수별 동기화 위임 및 최종 응답 생성

**테스트 대상:** `TeamPlayerSyncOrchestrationService`

**Mock 의존성:** `ApiFootballHttpClient`, `TeamPlayerSyncTxService`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `syncTeamPlayers_success` | 외부 API로 선수 목록을 조회하고 선수별 TxService 호출 결과를 하나의 응답 DTO로 반환하는지 검증 |
| `syncTeamPlayers_empty` | 외부 API의 선수 목록이 비어 있으면 TxService를 호출하지 않고 빈 목록을 반환하는지 검증 |

### 주요 검증 사항

- `ApiFootballHttpClient.callExternalTeamPlayersApi()` 호출 검증
- JSON의 `response[0].team`, `response[0].players` 데이터 추출
- 선수별 `TeamPlayerSyncTxService.getOrCreateTeamPlayerResponse()` 호출 검증
- `extracting()`을 이용한 선수 이름 및 팀 이름 검증
- `verify(times(1))`을 이용한 외부 API 호출 및 선수별 TxService 위임 검증
- `verifyNoInteractions()`를 이용한 빈 선수 목록의 불필요한 동기화 호출 방지

---


## 전체 테스트 현황

| 테스트 클래스 | 테스트 수 |
|---|---:|
| `MemberServiceImplTest` | 5 |
| `MemberJwtAuthenticationConverterTest` | 4 |
| `SecurityConfigTest` | 3 |
| `TeamQueryTxServiceTest` | 8 |
| `TeamSyncTxServiceTest` | 4 |
| `PlayerQueryTxServiceTest` | 6 |
| `PlayerQueryOrchestrationServiceTest` | 6 |
| `TeamPlayerQueryTxServiceTest` | 3 |
| `TeamPlayerQueryOrchestrationServiceTest` | 2 |
| `TeamPlayerSyncTxServiceTest` | 5 |
| `TeamPlayerSyncOrchestrationServiceTest` | 2 |
| **합계** | **48** |