package com.dhoon.transfertracker.internal.player.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.sync.dto.response.PlayerSaveResponseDto;
import org.junit.jupiter.api.AfterEach;
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
        PlayerSyncOrchestrationService.class,
        PlayerSyncTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerSyncTest {


    @Autowired
    PlayerSyncOrchestrationService playerSyncOrchestrationService;

    @Autowired
    PlayerRepository playerRepository;


    @MockitoBean
    ApiFootballHttpClient apiFootballHttpClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @AfterEach
    void clearDatabase() {
        playerRepository.deleteAll();
    }


    // ==================================================
    // 정상
    // ==================================================

    @Test
    @DisplayName("선수 정보를 동기화하면 새로운 Player가 저장된다.")
    void syncPlayer_success() {

        // given
        Long playerApiId = 10L;


        given(
                apiFootballHttpClient
                        .callExternalPlayerApi(
                                playerApiId
                        )
        )
                .willReturn(
                        playerResponse(
                                10L,
                                "Bukayo Saka"
                        )
                );


        // when
        PlayerSaveResponseDto response =
                playerSyncOrchestrationService
                        .syncPlayer(
                                playerApiId
                        );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerAPIId())
                .isEqualTo(
                        10L
                );


        assertThat(playerRepository.count())
                .isEqualTo(1);


        Player player =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        assertThat(player.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(player.getApiFootballId())
                .isEqualTo(
                        10L
                );
    }


    // ==================================================
    // Transaction Boundary
    // ==================================================

    @Test
    @DisplayName("선수 외부 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    void syncPlayer_externalApiCall_withoutTransaction() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerResponse(
                        10L,
                        "Bukayo Saka"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerApi(
                                playerApiId
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
        playerSyncOrchestrationService
                .syncPlayer(
                        playerApiId
                );


        // then
        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalPlayerApi(
                        playerApiId
                );


        assertThat(playerRepository.count())
                .isEqualTo(1);
    }


    // ==================================================
    // 멱등성
    // ==================================================

    @Test
    @DisplayName("동일한 선수를 여러 번 동기화해도 Player를 중복 저장하지 않는다.")
    void syncPlayer_samePlayer_idempotent() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerResponse(
                        10L,
                        "Bukayo Saka"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        PlayerSaveResponseDto firstResponse =
                playerSyncOrchestrationService
                        .syncPlayer(
                                playerApiId
                        );


        Player firstPlayer =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        Long firstPlayerId =
                firstPlayer.getId();


        PlayerSaveResponseDto secondResponse =
                playerSyncOrchestrationService
                        .syncPlayer(
                                playerApiId
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);


        Player secondPlayer =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        assertThat(secondPlayer.getId())
                .isEqualTo(
                        firstPlayerId
                );


        assertThat(firstResponse.getPlayerAPIId())
                .isEqualTo(
                        10L
                );

        assertThat(secondResponse.getPlayerAPIId())
                .isEqualTo(
                        10L
                );


        then(apiFootballHttpClient)
                .should(times(2))
                .callExternalPlayerApi(
                        playerApiId
                );
    }


    // ==================================================
    // 기존 Entity 재사용
    // ==================================================

    @Test
    @DisplayName("이미 존재하는 선수는 새로 저장하지 않고 기존 Player를 재사용한다.")
    void syncPlayer_existingPlayer_reuse() {

        // given
        Player existingPlayer =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerApi(
                                10L
                        )
        )
                .willReturn(
                        playerResponse(
                                10L,
                                "Bukayo Saka"
                        )
                );


        // when
        PlayerSaveResponseDto response =
                playerSyncOrchestrationService
                        .syncPlayer(
                                10L
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);


        Player foundPlayer =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        assertThat(foundPlayer.getId())
                .isEqualTo(
                        existingPlayer.getId()
                );

        assertThat(foundPlayer.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerAPIId())
                .isEqualTo(
                        10L
                );
    }


    @Test
    @DisplayName("기존 Player의 한글 이름은 다시 동기화해도 유지된다.")
    void syncPlayer_existingKoreanName_preserve() {

        // given
        Player player =
                Player.of(
                        "Son Heung-min",
                        20L
                );

        player.assignKoPlayerName(
                "손흥민"
        );

        Player existingPlayer =
                playerRepository.save(
                        player
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerApi(
                                20L
                        )
        )
                .willReturn(
                        playerResponse(
                                20L,
                                "Son Heung-min"
                        )
                );


        // when
        PlayerSaveResponseDto response =
                playerSyncOrchestrationService
                        .syncPlayer(
                                20L
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);


        Player foundPlayer =
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
                        .orElseThrow();


        assertThat(foundPlayer.getId())
                .isEqualTo(
                        existingPlayer.getId()
                );

        assertThat(foundPlayer.getPlayerNameKo())
                .isEqualTo(
                        "손흥민"
                );


        /*
         * PlayerSaveResponseDto가
         * player.getDisplayName()을 사용하므로
         * 기존 번역 이름이 응답에도 유지된다.
         */
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "손흥민"
                );
    }


    // ==================================================
    // 현재 Sync 정책
    // ==================================================

    @Test
    @DisplayName("기존 Player가 존재하면 외부 API의 변경된 영문 이름으로 덮어쓰지 않는다.")
    void syncPlayer_existingPlayer_doesNotOverwriteName() {

        // given
        Player existingPlayer =
                playerRepository.save(
                        Player.of(
                                "Old Player Name",
                                30L
                        )
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerApi(
                                30L
                        )
        )
                .willReturn(
                        playerResponse(
                                30L,
                                "New Player Name"
                        )
                );


        // when
        PlayerSaveResponseDto response =
                playerSyncOrchestrationService
                        .syncPlayer(
                                30L
                        );


        // then
        Player foundPlayer =
                playerRepository
                        .findByApiFootballId(
                                30L
                        )
                        .orElseThrow();


        assertThat(foundPlayer.getId())
                .isEqualTo(
                        existingPlayer.getId()
                );


        /*
         * 현재 getOrCreatePlayer()는
         * 기존 Entity를 찾으면 그대로 반환한다.
         *
         * API의 최신 name으로 update하지 않는다.
         */
        assertThat(foundPlayer.getPlayerName())
                .isEqualTo(
                        "Old Player Name"
                );


        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Old Player Name"
                );
    }


    // ==================================================
    // 외부 API 실패
    // ==================================================

    @Test
    @DisplayName("선수 외부 API 호출에 실패하면 Player 데이터는 변경되지 않는다.")
    void syncPlayer_externalApiFail_noDatabaseChange() {

        // given
        Long playerApiId = 10L;


        given(
                apiFootballHttpClient
                        .callExternalPlayerApi(
                                playerApiId
                        )
        )
                .willThrow(
                        new RuntimeException(
                                "API-Football 호출 실패"
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                playerSyncOrchestrationService
                        .syncPlayer(
                                playerApiId
                        )
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "API-Football 호출 실패"
                );


        assertThat(playerRepository.count())
                .isZero();


        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalPlayerApi(
                        playerApiId
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private JsonNode playerResponse(
            Long playerApiId,
            String playerName
    ) {

        return readJson(
                """
                {
                  "id": %d,
                  "name": "%s"
                }
                """.formatted(
                        playerApiId,
                        playerName
                )
        );
    }


    private JsonNode readJson(
            String json
    ) {

        try {

            return objectMapper.readTree(
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