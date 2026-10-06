package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.TeamPlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamPlayersSaveResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        FootballSyncService.class,
        FootballSyncTxService.class
})
@ActiveProfiles("test")

/*
 * [기존 유지 - 중요]
 *
 * 실제 Football Sync 구조의 Transaction 경계를 검증하기 위해
 * @DataJpaTest가 기본으로 제공하는 Test Transaction을 비활성화한다.
 *
 * FootballSyncService
 *      → 외부 API 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → 선수 1명 단위 DB 작업
 *      → DTO 생성
 *      → Transaction O
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncTeamPlayersServiceTest {


    @Autowired
    FootballSyncService footballSyncService;


    @Autowired
    TeamRepository teamRepository;


    @Autowired
    PlayerRepository playerRepository;


    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * TeamPlayer가 Team / Player를 FK로 참조하므로
         * 관계 Entity부터 삭제한다.
         */
        teamPlayerRepository.deleteAll();

        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    /*
     * [기존 수정]
     *
     * 상황:
     *
     * API-Football이 Arsenal과
     *
     * - Bukayo Saka
     * - Martin Odegaard
     *
     * 를 반환한다.
     *
     *
     * 현재 구조에서는 players 반복마다
     *
     * FootballSyncTxService.getOrCreateTeamPlayerResponse()
     *
     * 가 호출된다.
     *
     *
     * 선수 1명 처리 흐름:
     *
     * Player 조회 / 생성
     *
     *      ↓
     *
     * Team 조회 / 생성
     *
     *      ↓
     *
     * TeamPlayer 조회 / 생성
     *
     *      ↓
     *
     * DTO 생성
     *
     *      ↓
     *
     * Commit
     *
     *
     * 기대 결과:
     *
     * Team       1건
     * Player     2건
     * TeamPlayer 2건
     */
    @Test
    @DisplayName(
            "팀 선수 정보를 동기화하면 Team, Player, TeamPlayer가 저장된다."
    )
    void syncTeamPlayers_success() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamPlayersResponse();


        given(
                footballRestClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamPlayersSaveResponseDto response =
                footballSyncService
                        .syncTeamPlayers(
                                teamApiId
                        );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);


        assertThat(playerRepository.count())
                .isEqualTo(2);


        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);


        Team arsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        assertThat(arsenal.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
        )
                .isPresent();


        assertThat(
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
        )
                .isPresent();


        /*
         * [리팩토링 확인 - 중요]
         *
         * Entity를 FootballSyncService까지 반환해서
         * DTO를 만드는 구조가 아니다.
         *
         * TxService 안에서 DTO 변환이 끝난 뒤
         * DTO만 Service로 반환된다.
         */
        assertThat(response.getPlayers())
                .hasSize(2);


        assertThat(response.getPlayers())
                .extracting(
                        TeamPlayerSaveResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );


        /*
         * Team 관계의 Lazy Entity 접근도
         * TxService 내부 DTO 생성 시점에 끝났는지 확인한다.
         */
        assertThat(response.getPlayers())
                .extracting(
                        TeamPlayerSaveResponseDto::getTeamName
                )
                .containsOnly(
                        "Arsenal"
                );
    }


    /*
     * [기존 유지 - 중요]
     *
     * FootballSyncService가 외부 API를 호출하는 동안에는
     * DB Transaction이 활성화되어 있으면 안 된다.
     */
    @Test
    @DisplayName(
            "팀 선수 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
    void syncTeamPlayers_externalApiCall_withoutTransaction() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamPlayersResponse();


        given(
                footballRestClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willAnswer(invocation -> {

                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();


                    return apiResponse;
                });


        // when
        footballSyncService
                .syncTeamPlayers(
                        teamApiId
                );


        // then
        then(footballRestClient)
                .should(times(1))
                .callExternalTeamPlayersApi(
                        teamApiId
                );


        assertThat(teamRepository.count())
                .isEqualTo(1);


        assertThat(playerRepository.count())
                .isEqualTo(2);


        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);
    }


    /*
     * [기존 유지 - 중요]
     *
     * 같은 팀의 선수 목록을 여러 번 Sync해도
     *
     * Team
     * Player
     * TeamPlayer
     *
     * 가 중복 저장되지 않아야 한다.
     */
    @Test
    @DisplayName(
            "동일한 팀 선수 목록을 여러 번 동기화해도 중복 저장하지 않는다."
    )
    void syncTeamPlayers_sameData_idempotent() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamPlayersResponse();


        given(
                footballRestClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncTeamPlayers(
                        teamApiId
                );


        footballSyncService
                .syncTeamPlayers(
                        teamApiId
                );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);


        assertThat(playerRepository.count())
                .isEqualTo(2);


        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);


        then(footballRestClient)
                .should(times(2))
                .callExternalTeamPlayersApi(
                        teamApiId
                );
    }


    /*
     * [기존 유지]
     *
     * 상황:
     *
     * Arsenal과 Bukayo Saka는 이미 DB에 존재한다.
     *
     * Martin Odegaard만 새 데이터다.
     *
     *
     * 기대 결과:
     *
     * 기존 Arsenal 재사용
     * 기존 Saka 재사용
     * Odegaard 생성
     * TeamPlayer 2건 생성
     */
    @Test
    @DisplayName(
            "이미 존재하는 Team과 Player는 재사용하고 새로운 선수만 저장한다."
    )
    void syncTeamPlayers_existingTeamAndPlayer_reuse() {

        // given
        Team existingTeam =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L
                        )
                );


        Player existingPlayer =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        JsonNode apiResponse =
                teamPlayersResponse();


        given(
                footballRestClient
                        .callExternalTeamPlayersApi(
                                42L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncTeamPlayers(
                        42L
                );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);


        assertThat(playerRepository.count())
                .isEqualTo(2);


        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);


        Team foundTeam =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        Player foundSaka =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        assertThat(foundTeam.getId())
                .isEqualTo(
                        existingTeam.getId()
                );


        assertThat(foundSaka.getId())
                .isEqualTo(
                        existingPlayer.getId()
                );
    }


    /*
     * [기존 수정 - 경계]
     *
     * 이 테스트는 이번 리팩토링으로
     * 동작이 실제로 변경된 부분이다.
     *
     *
     * 현재 syncTeamPlayers()는:
     *
     * playersData.forEach(...)
     *
     * 안에서만 TxService를 호출한다.
     *
     *
     * 따라서 players 배열이 비어있다면
     *
     * getOrCreateTeamPlayerResponse()
     *
     * 자체가 한 번도 호출되지 않는다.
     *
     *
     * 즉 현재 구현 기준:
     *
     * Team       0
     * Player     0
     * TeamPlayer 0
     *
     * 이다.
     */
    @Test
    @DisplayName(
            "선수 목록이 비어있으면 Team, Player, TeamPlayer를 저장하지 않는다."
    )
    void syncTeamPlayers_emptyPlayers_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamPlayersEmptyResponse();


        given(
                footballRestClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamPlayersSaveResponseDto response =
                footballSyncService
                        .syncTeamPlayers(
                                teamApiId
                        );


        // then
        assertThat(teamRepository.count())
                .isZero();


        assertThat(playerRepository.count())
                .isZero();


        assertThat(teamPlayerRepository.count())
                .isZero();


        assertThat(response.getPlayers())
                .isEmpty();
    }


    // ==================================================
    // 예외 상황
    // ==================================================


    /*
     * [기존 유지]
     *
     * 외부 API 호출 자체가 실패한다.
     *
     * 아직 TxService에 진입하기 전이므로
     * DB 변경은 없어야 한다.
     */
    @Test
    @DisplayName(
            "팀 선수 API 호출에 실패하면 DB 데이터는 변경되지 않는다."
    )
    void syncTeamPlayers_externalApiFail_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        given(
                footballRestClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willThrow(
                        new RuntimeException(
                                "API-Football 호출 실패"
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                footballSyncService
                        .syncTeamPlayers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "API-Football 호출 실패"
                );


        assertThat(teamRepository.count())
                .isZero();


        assertThat(playerRepository.count())
                .isZero();


        assertThat(teamPlayerRepository.count())
                .isZero();
    }


    /*
     * [기존 수정 - 중요]
     *
     * 현재 Transaction 단위를 검증하는 테스트.
     *
     *
     * 현재 구조:
     *
     * playersData.forEach(...)
     *
     *      ↓
     *
     * 선수 #1
     * FootballSyncTxService.getOrCreateTeamPlayerResponse()
     *
     *      ↓
     *
     * Tx #1
     *
     * Player
     * Team
     * TeamPlayer
     * DTO 생성
     *
     *      ↓
     *
     * COMMIT
     *
     *
     * 선수 #2
     * FootballSyncTxService.getOrCreateTeamPlayerResponse()
     *
     *      ↓
     *
     * Tx #2
     *
     * 처리 중 실패
     *
     *      ↓
     *
     * ROLLBACK
     *
     *
     * 따라서 두 번째 선수 처리에 실패해도
     * 첫 번째 선수 Transaction은 이미 Commit되어 있다.
     */
    @Test
    @DisplayName(
            "여러 선수 중 뒤의 선수 처리에 실패하면 앞에서 완료된 데이터는 유지된다."
    )
    void syncTeamPlayers_secondPlayerFail_partialCommit() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamPlayersSecondPlayerInvalidResponse();


        given(
                footballRestClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when & then
        assertThatThrownBy(() ->
                footballSyncService
                        .syncTeamPlayers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                );


        /*
         * 첫 번째 선수 처리 Transaction에서
         *
         * Player + Team + TeamPlayer
         *
         * 가 함께 Commit됐다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);


        assertThat(playerRepository.count())
                .isEqualTo(1);


        assertThat(teamPlayerRepository.count())
                .isEqualTo(1);


        Player saka =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        Team arsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        TeamPlayer teamPlayer =
                teamPlayerRepository
                        .findByPlayerIdAndTeamId(
                                saka.getId(),
                                arsenal.getId()
                        )
                        .orElseThrow();


        assertThat(teamPlayer.getPlayer().getId())
                .isEqualTo(
                        saka.getId()
                );


        assertThat(teamPlayer.getTeam().getId())
                .isEqualTo(
                        arsenal.getId()
                );
    }


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * [Fixture - 정상 팀 선수 목록]
     *
     * Arsenal
     *
     * - Bukayo Saka
     * - Martin Odegaard
     */
    private JsonNode teamPlayersResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": 42,
                        "name": "Arsenal"
                      },
                      "players": [
                        {
                          "id": 10,
                          "name": "Bukayo Saka"
                        },
                        {
                          "id": 20,
                          "name": "Martin Odegaard"
                        }
                      ]
                    }
                  ]
                }
                """
        );
    }


    /*
     * [Fixture - 빈 선수 목록]
     *
     * Team 정보는 있지만
     * players 배열은 비어있다.
     */
    private JsonNode teamPlayersEmptyResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": 42,
                        "name": "Arsenal"
                      },
                      "players": []
                    }
                  ]
                }
                """
        );
    }


    /*
     * [Fixture - 두 번째 선수 데이터 오류]
     *
     * 첫 번째 선수는 정상.
     *
     * 두 번째 선수에는 name 필드가 없어서
     * getOrCreatePlayer() 처리 중 예외가 발생한다.
     */
    private JsonNode teamPlayersSecondPlayerInvalidResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": 42,
                        "name": "Arsenal"
                      },
                      "players": [
                        {
                          "id": 10,
                          "name": "Bukayo Saka"
                        },
                        {
                          "id": 20
                        }
                      ]
                    }
                  ]
                }
                """
        );
    }


    /*
     * [Fixture Helper]
     */
    private JsonNode readJson(
            String json
    ) {

        try {

            return objectMapper
                    .readTree(
                            json
                    );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "테스트 JSON 생성 실패",
                    e
            );
        }
    }
}