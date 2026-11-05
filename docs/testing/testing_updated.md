# 테스트 설계 및 검증

## 테스트 전략

JUnit 5, Mockito, AssertJ를 활용하여 Service 단위 테스트를 작성합니다.
Repository 및 인증 관련 의존성을 Mock으로 격리하여 정상 흐름, 예외 처리, 반환 데이터와 상호작용을 검증합니다.
Spring Security 인가 정책은 `@WebMvcTest`와 MockMvc로 검증합니다.

Repository 통합 테스트는 `@DataJpaTest`, Testcontainers MySQL 8.4, Flyway, Hibernate, QueryDSL을 사용합니다.
Mock이 아닌 실제 데이터베이스에서 조회 조건, 정렬, 페이지네이션, FULLTEXT 검색 및 쿼리 결과를 검증합니다.

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

## 12. TransferQueryTxServiceTest

**테스트 범위:** 선수별 이적 기록 조회, 전체 이적 기록 검색, 팀별 이적 기록 조회, 이적 상세 정보 및 예외 처리

**테스트 대상:** `TransferQueryTxService`

**Mock 의존성:** `TransferRepository`, `PlayerRepository`, `TeamRepository`, `TransferQueryRepository`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getPlayerTransfers_success` | 기존 선수의 이적 이력 조회 및 이적 전후 팀, 날짜, 반환 순서 검증 |
| `getPlayerTransfers_empty` | 선수는 존재하지만 이적 기록이 없으면 빈 목록 반환 |
| `getPlayerTransfers_notFound` | 선수 미존재 시 `NotFoundPlayerException` 발생 및 이적 Repository 미호출 |
| `getTransfer_success` | 이적 ID 기반 상세 조회 및 선수·팀 ID, 날짜 DTO 변환 검증 |
| `getTransfer_notFound` | 이적 기록 미존재 시 `NotFoundTransferException` 발생 |
| `getTransfers_success` | 전체 이적 목록, DTO 변환 순서 및 첫 페이지의 페이징 정보 검증 |
| `getTransfers_empty` | 이적 기록이 없으면 빈 목록과 `hasNext=false`, `hasPrevious=false` 반환 |
| `getTransfers_withFilters` | 검색어·리그·팀 ID·페이지 조건 전달 및 중간 페이지의 페이징 정보 검증 |
| `getTeamTransfers_success` | 기존 팀의 이적 기록 조회, DTO 변환 및 페이징 정보 검증 |
| `getTeamTransfers_empty` | 팀은 존재하지만 이적 기록이 없으면 빈 목록 반환 |
| `getTeamTransfers_notFoundTeam` | 팀 미존재 시 `NotFoundTeamException` 발생 및 이적 검색 Repository 미호출 |

### 주요 검증 사항

- `PlayerRepository.findById()` 및 `TeamRepository.findById()`를 이용한 리소스 존재 여부 검증
- `TransferRepository`의 선수별 이적 이력 및 상세 조회 결과를 DTO로 변환
- `TransferQueryRepository.findTransfers()`에 전달되는 검색 조건 검증
- `PageImpl`, `Page.empty()`를 이용한 페이징 데이터 구성
- `hasNext`, `hasPrevious`를 이용한 첫 페이지·중간 페이지·빈 페이지 검증
- AssertJ `extracting()`, `containsExactly()`, `isEmpty()`를 활용한 컬렉션 검증
- `verifyNoInteractions()`로 리소스 조회 실패 이후 불필요한 Repository 호출 차단 확인

---

## 13. TransferQueryOrchestrationServiceTest

**테스트 범위:** 이적 조회 요청의 입력값 검증, 검색어 정규화, 페이징 및 정렬 조건 생성, TxService 호출 위임

**테스트 대상:** `TransferQueryOrchestrationService`

**Mock 의존성:** `TransferService`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getPlayerTransfers_success` | 정상 선수 ID를 TxService에 전달하고 반환 DTO의 참조 동일성 검증 |
| `getPlayerTransfers_invalidPlayerId` | 선수 ID가 `null`이면 `InvalidPlayerIdException` 발생 및 TxService 미호출 |
| `getTransfer_success` | 정상 이적 ID 조회 시 TxService 위임 및 반환 DTO의 참조 동일성 검증 |
| `getTransfer_invalidTransferId` | 이적 ID가 `null`이면 `InvalidTransferIdException` 발생 및 TxService 미호출 |
| `getTransfers_success` | 전체 이적 조회의 검색 조건, 페이지 번호·크기·OFFSET·정렬 조건 검증 |
| `getTransfers_trimKeyword` | 검색어 앞뒤 공백 제거 후 TxService 전달 |
| `getTransfers_nullKeyword` | `null` 검색어를 빈 문자열로 정규화 |
| `getTransfers_invalidPage` | 음수 페이지 번호에 대한 `InvalidTransferSearchPageValueException` 검증 |
| `getTeamTransfers_success` | 팀별 이적 조회의 팀 ID·검색어·페이징 및 정렬 조건 검증 |
| `getTeamTransfers_trimKeyword` | 팀별 조회 검색어의 앞뒤 공백 제거 |
| `getTeamTransfers_nullKeyword` | `null` 검색어를 빈 문자열로 변환 |
| `getTeamTransfers_invalidArguments` | 음수 페이지와 `null` 팀 ID에 대한 각각의 예외 발생 및 TxService 미호출 검증 |

### 주요 검증 사항

- 선수 ID, 이적 ID, 팀 ID에 대한 `null` 검증
- 음수 페이지 번호 검증
- 검색어 정규화: `trim()` 및 `null` → `""`
- 페이지 크기 `50`과 0부터 시작하는 페이지 번호 검증
- `transferDate DESC`, `id DESC` 순서의 정렬 검증
- `ArgumentCaptor<Pageable>`을 이용한 페이지 번호, 크기, OFFSET, 정렬 속성 확인
- `eq()`, `any()`, `isNull()` 등 Mockito ArgumentMatcher 활용
- `isSameAs()`를 이용한 TxService 반환 객체의 참조 동일성 검증
- `verifyNoInteractions()`를 이용한 유효하지 않은 요청의 위임 차단 검증

---

## 14. TransferSyncOrchestrationServiceTest

**테스트 범위:** 외부 API-Football 호출, JSON 응답 처리, 선수별 이적 동기화 위임 및 최종 응답 생성

**테스트 대상:** `TransferSyncOrchestrationService`

**Mock 의존성:** `ApiFootballHttpClient`, `TransferSyncTxService`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `syncPlayerTransfers_success` | 외부 API 응답의 선수 정보·이적 기록 추출, TxService 인자 전달 및 반환 DTO 동일성 검증 |
| `syncTeamTransfers_success` | 팀의 선수별 이적 기록을 순회하여 TxService에 전달하고 팀 이름·선수 목록을 반환하는지 검증 |

### 주요 검증 사항

- `ApiFootballHttpClient.callExternalPlayerTransferApi()` 호출 검증
- `ApiFootballHttpClient.callExternalTeamTransfersApi()` 호출 검증
- Jackson `JsonMapper`, `JsonNode`를 이용한 테스트용 JSON 구성
- 외부 API 응답의 `response[].player`, `response[].transfers` 필드 추출
- 선수별 JSON 데이터가 해당 TxService 메서드에 전달되는지 검증
- 여러 선수의 동기화 결과를 `TeamTransfersSaveResponseDto`로 조합
- Mockito `given().willReturn()`을 이용한 외부 HTTP 응답 모방
- `verify(times(1))`, `verify(times(2))`를 이용한 외부 API 및 TxService 호출 횟수 검증
- AssertJ `isSameAs()`, `hasSize()`, `extracting()`을 활용한 응답 검증

---

## 15. TransferSyncTxServiceTest

**테스트 범위:** 외부 API 데이터 기반 선수·팀·이적 기록 조회 및 생성, 기존 데이터 재사용, 중복 저장 방지, 팀 표시 이름 조회

**테스트 대상:** `TransferSyncTxService`

**Mock 의존성:** `PlayerRepository`, `TeamRepository`, `TransferRepository`

| 테스트 메서드 | 검증 내용 |
|---|---|
| `getOrCreatePlayerTransfersResponse_existingPlayer_emptyTransfers` | 기존 선수와 빈 이적 목록 조회 시 저장 없이 빈 목록 반환 |
| `getOrCreatePlayerTransfersResponse_newPlayer_emptyTransfers` | 선수 미존재 시 신규 선수 생성·저장 및 저장 대상 필드 검증 |
| `getOrCreatePlayerTransfersResponse_existingTransfer` | 선수·팀·이적 기록이 모두 존재하면 기존 엔티티 재사용 및 중복 저장 방지 |
| `getOrCreatePlayerTransfersResponse_newTransfer` | 기존 선수·팀의 신규 이적 기록 생성 및 연관관계 검증 |
| `getOrCreatePlayerTransfersResponse_newTeam` | 팀 미존재 시 신규 팀을 저장하고 해당 팀과 연결된 이적 기록 생성 |
| `getDisplayTeamName_success` | 한국어 팀 이름이 존재하면 한국어 표시 이름 반환 |
| `getDisplayTeamName_success_notAssignKoTeamName` | 한국어 팀 이름이 없으면 원문 팀 이름 반환 |
| `getDisplayTeamName_notFound` | 팀 미존재 시 `NotFoundTeamException` 발생 |

### 주요 검증 사항

- API-Football ID를 이용한 기존 Player·Team 조회
- `Optional.orElseGet()`을 이용한 기존 엔티티 재사용 및 신규 저장 분기 검증
- 선수·팀·이적 기록이 존재할 때 `verify(never())`로 중복 저장 요청 방지
- 신규 Player·Team·Transfer 생성 시 Repository `save()` 호출 검증
- Mockito `willAnswer()`를 이용해 저장 요청 객체를 반환하는 Mock 동작 구성
- `ReflectionTestUtils`를 이용한 Mock 환경의 신규 엔티티 ID 할당 모방
- `ArgumentCaptor<Player>`, `ArgumentCaptor<Team>`, `ArgumentCaptor<Transfer>`를 활용한 저장 대상 검증
- `isSameAs()`로 Transfer의 Player·inTeam·outTeam 연관관계 검증
- 신규 이적 기록의 날짜, 이적 유형 및 이적 전후 팀 검증
- `verifyNoInteractions()`로 빈 이적 목록에서 불필요한 Team·Transfer Repository 호출 방지
- 최종 응답 DTO의 선수 이름, 이적 기록 개수, 날짜 및 팀 이름 검증

### 단위 테스트 범위의 한계

현재 Repository는 Mockito Mock으로 대체되어 있으므로 다음 항목은 검증하지 않는다.

- 실제 MySQL INSERT 및 SELECT 실행
- 데이터베이스 UNIQUE 제약 조건
- 실제 JPA 영속성 컨텍스트 및 ID 생성
- 데이터베이스 트랜잭션 커밋 및 롤백
- 동시 요청에 따른 중복 저장 가능성

실제 조회 SQL 및 JPA 동작 일부는 아래 Repository 통합 테스트에서 검증한다. UNIQUE 제약 조건과 서비스 트랜잭션 롤백, 동시성은 별도 검증 대상이다.

---

---

## 16. TransferQueryRepositoryIntegrationTest

**테스트 범위:** 실제 MySQL을 이용한 QueryDSL 이적 기록 검색, 정렬, 페이지네이션, 동적 필터 및 FULLTEXT 검색

**테스트 대상:** `TransferQueryRepository.findTransfers()`

**테스트 환경:** JUnit Jupiter, `@DataJpaTest`, Testcontainers(MySQL 8.4), Flyway, Hibernate/JPA, QueryDSL, AssertJ

| 테스트 메서드 | 검증 내용 |
|---|---|
| `findTransfers_success` | 실제 DB에 저장된 이적 기록 및 연관 엔티티 조회 |
| `findTransfers_orderByTransferDateDescAndIdDesc` | 이적일 DESC 및 동일 이적일의 ID DESC 정렬 |
| `findTransfers_pagination` | LIMIT/OFFSET, 전체 건수, 페이지 정보 및 순서 검증 |
| `findTransfers_filterByTeamId` | 입단팀 또는 방출팀에 지정된 팀이 포함된 기록 조회 |
| `findTransfers_filterByLeagueCode` | 입단팀 또는 방출팀에 지정된 리그가 포함된 기록 조회 |
| `findTransfers_filterByKeyword` | 커밋된 데이터에 대한 MySQL FULLTEXT 선수 이름 검색 |
| `findTransfers_combinedFilters` | 선수 이름·팀 ID·리그 조건을 동시에 적용한 검색 |
| `findTransfers_combinedFilters_differentTeamMatchesLeague` | 팀 조건과 리그 조건을 서로 다른 팀이 충족해도 조회하는 정책 검증 |
| `findTransfers_blankKeyword_returnsAll` | `null`, `""`, 공백 문자열 검색어일 때 검색 필터 없이 전체 조회 (`@ParameterizedTest`, 3회 실행) |
| `findTransfers_noMatchingResults` | 검색 결과가 없을 때 빈 `Page`, 전체 건수 및 페이지 수 0 검증 |
| `findTransfers_pageOutOfRange` | 범위 밖 페이지의 빈 `content`와 실제 전체 건수·페이지 수 유지 검증 |

### 주요 검증 사항

- 실제 MySQL의 INSERT/SELECT 및 JPA 영속성 컨텍스트 동작 확인: `flush()`와 `clear()`로 DB 조회 경로 검증
- QueryDSL `where()`의 선택적 조건 적용 및 `null` 조건 무시 동작 검증
- `transferDate DESC`, `id DESC` 정렬과 OFFSET 페이지네이션 검증
- `getNumberOfElements()`, `getTotalElements()`, `getTotalPages()`, `hasNext()`, `hasPrevious()`의 차이 검증
- 팀과 리그 조건은 입단팀 또는 방출팀이 **각각 독립적으로 충족**할 수 있다는 검색 정책을 테스트로 명시
- Flyway 마이그레이션으로 생성한 FULLTEXT 인덱스와 Hibernate `FunctionContributor`의 `MATCH ... AGAINST` 함수 동작 확인
- 검색어가 `null`, 빈 문자열 또는 공백 문자열이면 FULLTEXT 조건을 생성하지 않도록 하는 분기 검증

### FULLTEXT 테스트 데이터 및 트랜잭션

일반 조회·정렬·페이징 테스트는 `Repository.save()`로 데이터를 준비하며, 기본 `@DataJpaTest` 트랜잭션을 이용해 테스트 종료 시 롤백한다.

MySQL InnoDB FULLTEXT는 같은 트랜잭션의 미커밋 INSERT 데이터를 일반 SELECT처럼 검색하지 못할 수 있다. 따라서 FULLTEXT 테스트에서는 테스트용 SQL을 **별도 트랜잭션에서 커밋**하도록 설정한다.

```java
@Sql(
        scripts = "/sql/transfer/fulltext-setup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Sql(
        scripts = "/sql/transfer/fulltext-cleanup.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
```

- `BEFORE_TEST_METHOD`: INSERT 후 별도 트랜잭션 COMMIT
- 테스트 실행: 실제 `MATCH ... AGAINST` 조건으로 조회 및 검증
- `AFTER_TEST_METHOD`: FK 제약을 고려하여 Transfer → Player → Team 순서로 DELETE 및 COMMIT
- 커밋된 데이터는 테스트 트랜잭션의 자동 롤백 대상이 아니므로 정리 스크립트가 필요하다.
- 테스트별 식별자 충돌, 병렬 실행 시 데이터 간섭 및 중간 종료 시 잔여 데이터에 유의한다.

### 문제 해결 기록

1. **QueryDSL Bean 등록:** `JPAQueryFactory` 자체가 아니라 이를 Bean으로 생성하는 `QueryDslConfig`를 테스트 컨텍스트에 등록
2. **Hibernate 함수 등록:** `match_against` 이름과 `META-INF/services/org.hibernate.boot.model.FunctionContributor` SPI 경로 확인
3. **FULLTEXT 검색 결과 0건:** `flush()`와 `commit()`의 차이를 확인하고 `@Sql`의 `ISOLATED` 트랜잭션으로 해결
4. **테스트 데이터 격리:** FULLTEXT용 사전 INSERT·사후 DELETE 스크립트 분리

**작성한 테스트 메서드:** 11개 / **JUnit 파라미터 호출 포함 테스트 실행 수:** 13회(동일 구성을 유지한 경우)

※ 작성 내역은 대화에서 확인한 기준이며, 전체 테스트 스위트의 최종 통과 여부는 Gradle 실행 결과로 확인한다.

---

## 전체 테스트 현황

| 테스트 클래스 | 테스트 메서드 수 |
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
| `TransferQueryTxServiceTest` | 11 |
| `TransferQueryOrchestrationServiceTest` | 12 |
| `TransferSyncOrchestrationServiceTest` | 2 |
| `TransferSyncTxServiceTest` | 8 |
| `TransferQueryRepositoryIntegrationTest` | 11 |
| **합계** | **92** |

- 기존 단위·Security 테스트: **81개 메서드**
- Repository 통합 테스트: **11개 메서드**
- 파라미터화 테스트의 개별 실행을 포함하면 총 **94회 호출**로 예상된다(다른 파라미터화 테스트가 없다는 전제).
- 전체 테스트 통과 여부는 아직 별도로 확인하지 않았다.



### 향후 테스트 계획

- `TransferQueryTxService` 등 Service 계층의 실제 Bean·Repository 연동 통합 테스트
- `TransferSyncTxService` 신규 저장 및 중복 동기화 통합 테스트
- Flyway 스키마 및 MySQL UNIQUE 제약 조건 검증
- 예외 발생 시 실제 트랜잭션 롤백 검증
- Transfer API HTTP 응답 및 예외 처리 검증

**통합 테스트 환경:** Testcontainers + MySQL 8.4 + Flyway

**현재 상태:** 단위 테스트 81개 및 Transfer Repository 통합 테스트 11개 작성 내역 반영. 전체 테스트 실행 성공 여부는 별도 확인 필요.
