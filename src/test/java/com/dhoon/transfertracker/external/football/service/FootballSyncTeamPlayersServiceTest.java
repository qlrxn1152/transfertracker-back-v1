package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
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

import java.util.List;

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
 * [신규 - 중요]
 *
 * 실제 Football Sync 구조의 Transaction 경계를 검증하기 위해
 * @DataJpaTest가 기본으로 제공하는 Test Transaction을 비활성화한다.
 *
 * FootballSyncService
 *      → 외부 API
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → DB 작업
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


    /*
     * 실제 API-Football 서버를 호출하지 않고
     * 테스트에서 원하는 응답과 실패 상황을 직접 만든다.
     */
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
     * [신규]
     *
     * 나타내는 상황:
     *
     * Arsenal 팀과
     *
     * - Bukayo Saka
     * - Martin Odegaard
     *
     * 두 선수가 아직 DB에 존재하지 않는 상태에서
     * API-Football이 정상적으로 선수 목록을 반환한다.
     *
     * 기대 결과:
     *
     * Team 1건
     * Player 2건
     * TeamPlayer 2건
     *
     * 이 저장되어야 한다.
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
         * API 응답 DTO에도
         * 두 선수 정보가 들어있는지 확인한다.
         */
        assertThat(response.getPlayers())
                .hasSize(2);

        assertThat(response.getPlayers())
                .extracting(
                        player -> player.getPlayerName()
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );
    }


    /*
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * FootballSyncService가
     * API-Football에서 팀 선수 목록을 가져오는 순간.
     *
     * 기대 결과:
     *
     * 외부 HTTP 요청을 기다리는 동안에는
     * DB Transaction이 열려 있으면 안 된다.
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
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * 같은 Arsenal 선수 목록을
     * 두 번 연속 동기화한다.
     *
     * 기대 결과:
     *
     * Team
     * Player
     * TeamPlayer
     *
     * 어느 것도 중복 저장되면 안 된다.
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
     * [신규]
     *
     * 나타내는 상황:
     *
     * Arsenal과 Bukayo Saka가 이미 DB에 존재한다.
     *
     * Martin Odegaard만 새로운 선수다.
     *
     * 기대 결과:
     *
     * 기존 Arsenal과 Saka를 재사용하고,
     * Odegaard만 새로 생성한 뒤
     * 각각의 TeamPlayer 관계를 만들어야 한다.
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
     * [신규]
     *
     * 나타내는 상황:
     *
     * API-Football에서 Team 정보는 정상적으로 반환했지만
     * 현재 소속 선수 목록이 비어있는 경우.
     *
     * 기대 결과:
     *
     * Team은 저장되지만
     * Player와 TeamPlayer는 생성되지 않는다.
     *
     * 현재 syncTeamPlayers() 구현의 경계 상황을 검증한다.
     */
    @Test
    @DisplayName(
            "선수 목록이 비어있으면 Team만 저장되고 Player 관계는 생성되지 않는다."
    )
    void syncTeamPlayers_emptyPlayers_saveTeamOnly() {

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
                .isEqualTo(1);

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
     * [신규]
     *
     * 나타내는 상황:
     *
     * API-Football 팀 선수 API 자체가 실패한다.
     *
     * 기대 결과:
     *
     * 아직 DB 작업을 시작하기 전이므로
     * Team / Player / TeamPlayer 어느 것도
     * 저장되어서는 안 된다.
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
     * [신규 - 중요]
     *
     * 이 테스트는 "원하는 최종 정책"을 검증하는 것이 아니라
     * 현재 구현의 Transaction 범위를 확인하기 위한 테스트다.
     *
     *
     * 나타내는 상황:
     *
     * 1. Arsenal 저장 성공
     *
     * 2. 첫 번째 선수 Bukayo Saka 저장 성공
     *
     * 3. 첫 번째 TeamPlayer 저장 성공
     *
     * 4. 두 번째 선수는 name 필드가 없어서 처리 실패
     *
     *
     * 현재 구현에서는 각각의
     * FootballSyncTxService 메서드 호출마다
     * 별도의 Transaction이 실행된다.
     *
     * 따라서 두 번째 선수 처리에서 실패해도
     * 앞에서 이미 Commit된 데이터는 남는다.
     */
    @Test
    @DisplayName(
            "여러 선수 중 뒤의 선수 처리에 실패하면 앞에서 완료된 데이터는 현재 구조상 유지된다."
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
         * Team 저장 Transaction은 이미 Commit됐다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);


        /*
         * 첫 번째 Player 저장 Transaction도 Commit됐다.
         */
        assertThat(playerRepository.count())
                .isEqualTo(1);


        /*
         * 첫 번째 TeamPlayer 관계도 이미 Commit됐다.
         */
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
     * 사용 대상:
     *
     * FootballSyncService.syncTeamPlayers()
     *
     *
     * 나타내는 상황:
     *
     * API-Football의 팀 선수 API에서
     *
     * Arsenal
     *
     * 소속 선수로
     *
     * - Bukayo Saka
     * - Martin Odegaard
     *
     * 두 명을 정상적으로 반환한 상황.
     *
     *
     * 이 Fixture는 다음 상황을 검증할 때 사용한다.
     *
     * 1. Team / Player / TeamPlayer 정상 저장
     * 2. 외부 API 호출 시 Transaction 비활성
     * 3. 동일 데이터 반복 Sync / 멱등성
     * 4. 기존 Team / Player 재사용
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
     * [Fixture - 선수 목록이 비어있는 팀]
     *
     * 사용 대상:
     *
     * FootballSyncService.syncTeamPlayers()
     *
     *
     * 나타내는 상황:
     *
     * Arsenal Team 정보는 정상적으로 존재하지만
     * API 응답의 players 배열이 비어있는 상황.
     *
     * Team만 저장되고 Player / TeamPlayer는
     * 생성되지 않는 경계 상황을 검증하기 위해 사용한다.
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
     * [Fixture - 두 번째 선수 데이터가 잘못된 응답]
     *
     * 사용 대상:
     *
     * syncTeamPlayers_secondPlayerFail_partialCommit()
     *
     *
     * 나타내는 상황:
     *
     * 첫 번째 선수 Bukayo Saka는 정상 데이터라서
     * Player와 TeamPlayer 저장까지 완료된다.
     *
     * 하지만 두 번째 Player에는 name 필드가 없다.
     *
     * 현재 getOrCreatePlayer()는
     *
     * playerData.get("name").asString()
     *
     * 을 사용하므로 두 번째 Player 처리 과정에서
     * 예외가 발생한다.
     *
     *
     * 이를 이용해서 한 선수 처리 실패 시
     * 앞에서 Commit된 데이터가 어떻게 되는지
     * 현재 Transaction 범위를 확인한다.
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
     * [Fixture Helper - JSON 변환]
     *
     * 문자열로 작성한 Fixture를
     * JsonNode로 변환한다.
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