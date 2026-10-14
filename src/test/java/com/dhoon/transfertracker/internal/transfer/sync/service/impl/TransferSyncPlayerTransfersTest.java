package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.etc.notification.service.MemberNotificationService;
import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        TransferSyncOrchestrationService.class,
        TransferSyncTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferSyncPlayerTransfersTest {


    @Autowired
    TransferSyncOrchestrationService transferSyncOrchestrationService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TransferRepository transferRepository;


    @MockitoBean
    ApiFootballHttpClient apiFootballHttpClient;


    /*
     * TransferSyncTxService는
     *
     * ObjectProvider<MemberNotificationService>
     *
     * 를 통해 NotificationService를 선택적으로 사용한다.
     *
     * 테스트에서는 실제 알림 로직을 실행할 필요가 없으므로
     * Mock Bean으로 교체한다.
     */
    @MockitoBean
    MemberNotificationService memberNotificationService;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @AfterEach
    void clearDatabase() {
        transferRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상
    // ==================================================

    @Test
    @DisplayName("선수 이적 정보를 동기화하면 Player, Team, Transfer가 저장된다.")
    void syncPlayerTransfers_success() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        PlayerTransfersResponseDto response =
                transferSyncOrchestrationService
                        .syncPlayerTransfers(
                                playerApiId
                        );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerTransfers())
                .hasSize(1);


        PlayerTransferItemResponseDto transferResponse =
                response.getPlayerTransfers().get(0);


        assertThat(transferResponse.getInTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transferResponse.getOutTeamName())
                .isEqualTo(
                        "Chelsea"
                );

        assertThat(transferResponse.getType())
                .isEqualTo(
                        "Transfer"
                );

        assertThat(transferResponse.getDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );


        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(1);


        Player player =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        List<Transfer> transfers =
                transferRepository
                        .findAllByPlayerId(
                                player.getId()
                        );


        assertThat(transfers)
                .hasSize(1);


        Transfer transfer =
                transfers.get(0);


        assertThat(transfer.getPlayer().getId())
                .isEqualTo(
                        player.getId()
                );

        assertThat(transfer.getInTeam().getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transfer.getOutTeam().getTeamName())
                .isEqualTo(
                        "Chelsea"
                );

        assertThat(transfer.getTransferType())
                .isEqualTo(
                        "Transfer"
                );

        assertThat(transfer.getTransferDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );


        then(memberNotificationService)
                .should(times(1))
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // Transaction Boundary
    // ==================================================

    @Test
    @DisplayName("선수 이적 외부 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    void syncPlayerTransfers_externalApiCall_withoutTransaction() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
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
        transferSyncOrchestrationService
                .syncPlayerTransfers(
                        playerApiId
                );


        // then
        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalPlayerTransferApi(
                        playerApiId
                );


        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(1);
    }


    // ==================================================
    // 멱등성
    // ==================================================

    @Test
    @DisplayName("동일한 선수 이적 정보를 여러 번 동기화해도 중복 저장하지 않는다.")
    void syncPlayerTransfers_sameTransfer_idempotent() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        transferSyncOrchestrationService
                .syncPlayerTransfers(
                        playerApiId
                );

        transferSyncOrchestrationService
                .syncPlayerTransfers(
                        playerApiId
                );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(1);


        then(apiFootballHttpClient)
                .should(times(2))
                .callExternalPlayerTransferApi(
                        playerApiId
                );


        /*
         * 첫 번째 Sync에서만 새로운 Transfer가 생성된다.
         *
         * 두 번째 Sync에서는 기존 Transfer를 재사용하므로
         * 알림 생성도 다시 호출하면 안 된다.
         */
        then(memberNotificationService)
                .should(times(1))
                .createFavoriteTeamNotificationsForAllMembers();
    }


    @Test
    @DisplayName("선수와 팀이 같아도 이적 날짜가 다르면 별도의 Transfer로 저장한다.")
    void syncPlayerTransfers_sameTeamsDifferentDate_saveSeparately() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2022-07-01",
                        "2026-07-01"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        PlayerTransfersResponseDto response =
                transferSyncOrchestrationService
                        .syncPlayerTransfers(
                                playerApiId
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);


        assertThat(response.getPlayerTransfers())
                .hasSize(2);


        Player player =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        List<Transfer> transfers =
                transferRepository
                        .findAllByPlayerId(
                                player.getId()
                        );


        assertThat(transfers)
                .extracting(
                        Transfer::getTransferDate
                )
                .containsExactly(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        ),
                        LocalDate.of(
                                2022,
                                7,
                                1
                        )
                );


        /*
         * Transfer는 두 건 생성됐지만
         * 한 Player의 Sync 작업 안에서
         * 알림 생성은 마지막에 한 번 호출된다.
         */
        then(memberNotificationService)
                .should(times(1))
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // 기존 Entity 재사용
    // ==================================================

    @Test
    @DisplayName("이미 존재하는 Player와 Team은 새로 저장하지 않고 기존 Entity를 사용한다.")
    void syncPlayerTransfers_existingPlayerAndTeam_reuse() {

        // given
        Player existingPlayer =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        Team existingArsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L
                        )
                );


        Team existingChelsea =
                teamRepository.save(
                        Team.of(
                                "Chelsea",
                                49L
                        )
                );


        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
                                10L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        PlayerTransfersResponseDto response =
                transferSyncOrchestrationService
                        .syncPlayerTransfers(
                                10L
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(1);


        Player foundPlayer =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        Team foundChelsea =
                teamRepository
                        .findByApiFootballId(
                                49L
                        )
                        .orElseThrow();


        assertThat(foundPlayer.getId())
                .isEqualTo(
                        existingPlayer.getId()
                );

        assertThat(foundArsenal.getId())
                .isEqualTo(
                        existingArsenal.getId()
                );

        assertThat(foundChelsea.getId())
                .isEqualTo(
                        existingChelsea.getId()
                );


        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerTransfers())
                .hasSize(1);
    }


    // ==================================================
    // 경계
    // ==================================================

    @Test
    @DisplayName("이적 목록이 비어있으면 Player만 저장하고 빈 이적 목록을 반환한다.")
    void syncPlayerTransfers_emptyTransfers_savePlayerOnly() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse();


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        PlayerTransfersResponseDto response =
                transferSyncOrchestrationService
                        .syncPlayerTransfers(
                                playerApiId
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isZero();

        assertThat(transferRepository.count())
                .isZero();


        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerTransfers())
                .isEmpty();


        /*
         * 새로운 Transfer가 생성된 것이 없으므로
         * 알림도 생성하지 않는다.
         */
        then(memberNotificationService)
                .should(never())
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // Rollback
    // ==================================================

    @Test
    @DisplayName("이적 정보 저장 중 예외가 발생하면 해당 선수 Sync 전체를 롤백한다.")
    void syncPlayerTransfers_fail_rollbackAll() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01",
                        "invalid-date"
                );


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when & then
        assertThatThrownBy(() ->
                transferSyncOrchestrationService
                        .syncPlayerTransfers(
                                playerApiId
                        )
        )
                .isInstanceOf(
                        DateTimeParseException.class
                );


        /*
         * TransferSyncTxService의
         * getOrCreatePlayerTransfersResponse() 전체가
         * 하나의 Transaction이다.
         *
         * 첫 번째 Transfer가 정상 처리되었더라도
         * 두 번째 Transfer에서 실패하면
         * Player / Team / Transfer 전부 Rollback되어야 한다.
         */
        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamRepository.count())
                .isZero();

        assertThat(transferRepository.count())
                .isZero();


        /*
         * Transaction 자체가 Rollback되었으므로
         * 정상적인 알림 생성까지 도달해서도 안 된다.
         */
        then(memberNotificationService)
                .should(never())
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // 외부 API 실패
    // ==================================================

    @Test
    @DisplayName("선수 이적 외부 API 호출에 실패하면 DB 데이터는 변경되지 않는다.")
    void syncPlayerTransfers_externalApiFail_noDatabaseChange() {

        // given
        Long playerApiId = 10L;


        given(
                apiFootballHttpClient
                        .callExternalPlayerTransferApi(
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
                transferSyncOrchestrationService
                        .syncPlayerTransfers(
                                playerApiId
                        )
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "API-Football 호출 실패"
                );


        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalPlayerTransferApi(
                        playerApiId
                );


        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamRepository.count())
                .isZero();

        assertThat(transferRepository.count())
                .isZero();


        then(memberNotificationService)
                .should(never())
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // Fixture
    // ==================================================

    private JsonNode playerTransfersResponse(
            String... transferDates
    ) {

        StringBuilder transfers =
                new StringBuilder();


        for (int i = 0; i < transferDates.length; i++) {

            if (i > 0) {
                transfers.append(",");
            }

            transfers.append(
                    """
                    {
                      "date": "%s",
                      "type": "Transfer",
                      "teams": {
                        "in": {
                          "id": 42,
                          "name": "Arsenal"
                        },
                        "out": {
                          "id": 49,
                          "name": "Chelsea"
                        }
                      }
                    }
                    """.formatted(
                            transferDates[i]
                    )
            );
        }


        return readJson(
                """
                {
                  "response": [
                    {
                      "player": {
                        "id": 10,
                        "name": "Bukayo Saka"
                      },
                      "transfers": [
                        %s
                      ]
                    }
                  ]
                }
                """.formatted(
                        transfers
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