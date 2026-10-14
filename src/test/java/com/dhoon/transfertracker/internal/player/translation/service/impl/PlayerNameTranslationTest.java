package com.dhoon.transfertracker.internal.player.translation.service.impl;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.PlayerNameTranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationResult;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.translation.dto.response.PlayerNameTranslationResponseDto;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;


@DataJpaTest
@Import({
        PlayerNameTranslationOrchestrationService.class,
        PlayerNameTranslationTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerNameTranslationTest {


    @Autowired
    PlayerNameTranslationOrchestrationService orchestrationService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    @MockitoBean
    OpenAiClient openAiClient;


    @AfterEach
    void clearDatabase() {

        teamPlayerRepository.deleteAll();

        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 번역 대상 없음
    // ==================================================

    @Test
    @DisplayName("번역할 선수가 없으면 OpenAI를 호출하지 않고 0건을 반환한다.")
    void translate_emptyTargets() {

        // when
        PlayerNameTranslationResponseDto response =
                orchestrationService.translate();


        // then
        assertThat(response.getTargetCount())
                .isZero();

        assertThat(response.getTranslatedCount())
                .isZero();

        assertThat(response.getBatchCount())
                .isZero();


        verifyNoInteractions(
                openAiClient
        );
    }


    // ==================================================
    // 정상 번역
    // ==================================================

    @Test
    @DisplayName("번역 대상 선수 이름을 OpenAI로 번역하고 DB에 반영한다.")
    void translate_success() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );

        Player odegaard =
                savePlayerWithTeam(
                        "Martin Odegaard",
                        20L,
                        arsenal
                );


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    List<PlayerNameTranslationTarget> targets =
                            invocation.getArgument(0);

                    return translationResult(
                            targets
                    );
                });


        // when
        PlayerNameTranslationResponseDto response =
                orchestrationService.translate();


        // then
        assertThat(response.getTargetCount())
                .isEqualTo(2);

        assertThat(response.getTranslatedCount())
                .isEqualTo(2);

        assertThat(response.getBatchCount())
                .isEqualTo(1);


        Player foundSaka =
                playerRepository.findById(
                        saka.getId()
                ).orElseThrow();

        Player foundOdegaard =
                playerRepository.findById(
                        odegaard.getId()
                ).orElseThrow();


        assertThat(foundSaka.getPlayerNameKo())
                .isEqualTo(
                        "번역-" + saka.getId()
                );

        assertThat(foundOdegaard.getPlayerNameKo())
                .isEqualTo(
                        "번역-" + odegaard.getId()
                );
    }


    // ==================================================
    // Transaction Boundary
    // ==================================================

    @Test
    @DisplayName("OpenAI를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    void translate_openAiCall_withoutTransaction() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        savePlayerWithTeam(
                "Bukayo Saka",
                10L,
                arsenal
        );


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();


                    List<PlayerNameTranslationTarget> targets =
                            invocation.getArgument(0);


                    return translationResult(
                            targets
                    );
                });


        // when
        orchestrationService.translate();


        // then
        then(openAiClient)
                .should(times(1))
                .translatePlayerNames(
                        anyList()
                );
    }


    // ==================================================
    // Batch
    // ==================================================

    @Test
    @DisplayName("번역 대상이 50명을 초과하면 50명 단위로 나누어 OpenAI를 호출한다.")
    void translate_splitIntoBatches() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        IntStream.rangeClosed(
                        1,
                        51
                )
                .forEach(i ->
                        savePlayerWithTeam(
                                "Player " + i,
                                (long) i,
                                arsenal
                        )
                );


        AtomicInteger callCount =
                new AtomicInteger();

        AtomicInteger firstBatchSize =
                new AtomicInteger();

        AtomicInteger secondBatchSize =
                new AtomicInteger();


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    List<PlayerNameTranslationTarget> targets =
                            invocation.getArgument(0);


                    int call =
                            callCount.incrementAndGet();


                    if (call == 1) {
                        firstBatchSize.set(
                                targets.size()
                        );
                    }

                    if (call == 2) {
                        secondBatchSize.set(
                                targets.size()
                        );
                    }


                    return translationResult(
                            targets
                    );
                });


        // when
        PlayerNameTranslationResponseDto response =
                orchestrationService.translate();


        // then
        assertThat(callCount.get())
                .isEqualTo(2);

        assertThat(firstBatchSize.get())
                .isEqualTo(50);

        assertThat(secondBatchSize.get())
                .isEqualTo(1);


        assertThat(response.getTargetCount())
                .isEqualTo(51);

        assertThat(response.getTranslatedCount())
                .isEqualTo(51);

        assertThat(response.getBatchCount())
                .isEqualTo(2);


        assertThat(
                playerRepository.findAll()
        )
                .allSatisfy(player ->
                        assertThat(
                                player.getPlayerNameKo()
                        )
                                .isNotBlank()
                );
    }


    // ==================================================
    // 번역 대상 필터
    // ==================================================

    @Test
    @DisplayName("이미 한글 이름이 있는 선수는 번역 대상에서 제외한다.")
    void translate_excludeAlreadyTranslatedPlayer() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );


        Player odegaard =
                Player.of(
                        "Martin Odegaard",
                        20L
                );

        odegaard.assignKoPlayerName(
                "마르틴 외데고르"
        );

        odegaard =
                playerRepository.save(
                        odegaard
                );


        teamPlayerRepository.save(
                TeamPlayer.of(
                        arsenal,
                        odegaard
                )
        );


        Long odegaardId =
                odegaard.getId();


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    List<PlayerNameTranslationTarget> targets =
                            invocation.getArgument(0);


                    assertThat(targets)
                            .hasSize(1);

                    assertThat(targets.get(0).getPlayerId())
                            .isEqualTo(
                                    saka.getId()
                            );

                    assertThat(targets)
                            .extracting(
                                    PlayerNameTranslationTarget::getPlayerId
                            )
                            .doesNotContain(
                                    odegaardId
                            );


                    return translationResult(
                            targets
                    );
                });


        // when
        PlayerNameTranslationResponseDto response =
                orchestrationService.translate();


        // then
        assertThat(response.getTargetCount())
                .isEqualTo(1);

        assertThat(response.getTranslatedCount())
                .isEqualTo(1);


        Player foundOdegaard =
                playerRepository.findById(
                        odegaardId
                ).orElseThrow();


        assertThat(foundOdegaard.getPlayerNameKo())
                .isEqualTo(
                        "마르틴 외데고르"
                );
    }


    @Test
    @DisplayName("현재 소속팀이 없는 Player는 이름 번역 대상에 포함하지 않는다.")
    void translate_excludePlayerWithoutTeam() {

        // given
        playerRepository.save(
                Player.of(
                        "Free Agent Player",
                        10L
                )
        );


        // when
        PlayerNameTranslationResponseDto response =
                orchestrationService.translate();


        // then
        assertThat(response.getTargetCount())
                .isZero();

        assertThat(response.getTranslatedCount())
                .isZero();

        assertThat(response.getBatchCount())
                .isZero();


        verifyNoInteractions(
                openAiClient
        );
    }


    // ==================================================
    // 공백 한글 이름
    // ==================================================

    @Test
    @DisplayName("한글 이름이 공백인 선수도 번역 대상으로 조회하고 정상적으로 반영한다.")
    void translate_blankKoreanName() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                Player.of(
                        "Bukayo Saka",
                        10L
                );

        saka.assignKoPlayerName(
                "   "
        );

        saka =
                playerRepository.save(
                        saka
                );


        teamPlayerRepository.save(
                TeamPlayer.of(
                        arsenal,
                        saka
                )
        );


        Long playerId =
                saka.getId();


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    List<PlayerNameTranslationTarget> targets =
                            invocation.getArgument(0);


                    assertThat(targets)
                            .hasSize(1);


                    return PlayerNameTranslationBatchResult.of(
                            List.of(
                                    PlayerNameTranslationResult.of(
                                            playerId,
                                            "부카요 사카"
                                    )
                            )
                    );
                });


        // when
        PlayerNameTranslationResponseDto response =
                orchestrationService.translate();


        // then
        assertThat(response.getTargetCount())
                .isEqualTo(1);

        assertThat(response.getTranslatedCount())
                .isEqualTo(1);


        Player foundPlayer =
                playerRepository.findById(
                        playerId
                ).orElseThrow();


        assertThat(foundPlayer.getPlayerNameKo())
                .isEqualTo(
                        "부카요 사카"
                );
    }


    // ==================================================
    // OpenAI 응답 검증
    // ==================================================

    @Test
    @DisplayName("OpenAI 번역 결과가 null이면 예외가 발생하고 번역을 반영하지 않는다.")
    void translate_nullResult_fail() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willReturn(
                        null
                );


        // when & then
        assertThatThrownBy(() ->
                orchestrationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "선수 이름 번역 결과가 존재하지 않습니다."
                );


        Player foundPlayer =
                playerRepository.findById(
                        saka.getId()
                ).orElseThrow();


        assertThat(foundPlayer.getPlayerNameKo())
                .isNull();
    }


    @Test
    @DisplayName("OpenAI 결과에 동일한 playerId가 중복되면 예외가 발생한다.")
    void translate_duplicatePlayerId_fail() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willReturn(
                        PlayerNameTranslationBatchResult.of(
                                List.of(
                                        PlayerNameTranslationResult.of(
                                                saka.getId(),
                                                "부카요 사카"
                                        ),
                                        PlayerNameTranslationResult.of(
                                                saka.getId(),
                                                "사카"
                                        )
                                )
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                orchestrationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "선수 이름 번역 결과에 중복 playerId가 존재합니다."
                );


        Player foundPlayer =
                playerRepository.findById(
                        saka.getId()
                ).orElseThrow();


        assertThat(foundPlayer.getPlayerNameKo())
                .isNull();
    }


    @Test
    @DisplayName("번역 요청과 응답의 playerId가 일치하지 않으면 예외가 발생한다.")
    void translate_playerIdMismatch_fail() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willReturn(
                        PlayerNameTranslationBatchResult.of(
                                List.of(
                                        PlayerNameTranslationResult.of(
                                                999999L,
                                                "잘못된 선수"
                                        )
                                )
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                orchestrationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "선수 이름 번역 요청과 응답의 playerId가 일치하지 않습니다."
                );


        Player foundPlayer =
                playerRepository.findById(
                        saka.getId()
                ).orElseThrow();


        assertThat(foundPlayer.getPlayerNameKo())
                .isNull();
    }


    @Test
    @DisplayName("OpenAI 결과의 한글 이름이 비어있으면 예외가 발생한다.")
    void translate_blankTranslatedName_fail() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willReturn(
                        PlayerNameTranslationBatchResult.of(
                                List.of(
                                        PlayerNameTranslationResult.of(
                                                saka.getId(),
                                                "   "
                                        )
                                )
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                orchestrationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "선수 이름 번역 결과에 비어있는 playerNameKo가 존재합니다."
                );


        Player foundPlayer =
                playerRepository.findById(
                        saka.getId()
                ).orElseThrow();


        assertThat(foundPlayer.getPlayerNameKo())
                .isNull();
    }


    // ==================================================
    // 동시 변경 보호
    // ==================================================

    @Test
    @DisplayName("번역 요청 후 사람이 한글 이름을 수정했다면 AI 결과로 덮어쓰지 않는다.")
    void translate_manualChange_preserve() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );


        Long playerId =
                saka.getId();


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    /*
                     * 번역 대상 조회 이후,
                     * OpenAI 응답을 기다리는 사이
                     * 사람이 직접 값을 수정했다고 가정한다.
                     */
                    Player player =
                            playerRepository.findById(
                                    playerId
                            ).orElseThrow();


                    player.assignKoPlayerName(
                            "수동 수정 이름"
                    );


                    playerRepository.save(
                            player
                    );


                    return PlayerNameTranslationBatchResult.of(
                            List.of(
                                    PlayerNameTranslationResult.of(
                                            playerId,
                                            "AI 번역 이름"
                                    )
                            )
                    );
                });


        // when
        orchestrationService.translate();


        // then
        Player foundPlayer =
                playerRepository.findById(
                        playerId
                ).orElseThrow();


        assertThat(foundPlayer.getPlayerNameKo())
                .isEqualTo(
                        "수동 수정 이름"
                );
    }


    // ==================================================
    // Batch별 Transaction / Partial Commit
    // ==================================================

    @Test
    @DisplayName("두 번째 번역 Batch가 실패하면 첫 번째 Batch의 번역 결과는 유지된다.")
    void translate_secondBatchFail_partialCommit() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        IntStream.rangeClosed(
                        1,
                        51
                )
                .forEach(i ->
                        savePlayerWithTeam(
                                "Player " + i,
                                (long) i,
                                arsenal
                        )
                );


        AtomicInteger callCount =
                new AtomicInteger();


        given(
                openAiClient.translatePlayerNames(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    List<PlayerNameTranslationTarget> targets =
                            invocation.getArgument(0);


                    int call =
                            callCount.incrementAndGet();


                    if (call == 1) {

                        return translationResult(
                                targets
                        );
                    }


                    /*
                     * 두 번째 Batch는 요청 대상이 1명인데
                     * 빈 응답을 반환한다.
                     *
                     * validateResult()에서
                     * ID 불일치로 실패한다.
                     */
                    return PlayerNameTranslationBatchResult.empty();
                });


        // when & then
        assertThatThrownBy(() ->
                orchestrationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "선수 이름 번역 요청과 응답의 playerId가 일치하지 않습니다."
                );


        /*
         * 첫 번째 Batch의 applyTranslations()는
         * 이미 별도 Transaction에서 Commit됐다.
         */
        long translatedCount =
                playerRepository.findAll()
                        .stream()
                        .filter(player ->
                                player.getPlayerNameKo() != null
                        )
                        .count();


        assertThat(translatedCount)
                .isEqualTo(50);


        long untranslatedCount =
                playerRepository.findAll()
                        .stream()
                        .filter(player ->
                                player.getPlayerNameKo() == null
                        )
                        .count();


        assertThat(untranslatedCount)
                .isEqualTo(1);


        then(openAiClient)
                .should(times(2))
                .translatePlayerNames(
                        anyList()
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private Team saveTeam(
            String teamName,
            Long apiFootballId
    ) {

        return teamRepository.save(
                Team.of(
                        teamName,
                        apiFootballId,
                        LeagueCode.EPL
                )
        );
    }


    private Player savePlayerWithTeam(
            String playerName,
            Long apiFootballId,
            Team team
    ) {

        Player player =
                playerRepository.save(
                        Player.of(
                                playerName,
                                apiFootballId
                        )
                );


        teamPlayerRepository.save(
                TeamPlayer.of(
                        team,
                        player
                )
        );


        return player;
    }


    private PlayerNameTranslationBatchResult translationResult(
            List<PlayerNameTranslationTarget> targets
    ) {

        List<PlayerNameTranslationResult> players =
                targets.stream()
                        .map(target ->
                                PlayerNameTranslationResult.of(
                                        target.getPlayerId(),
                                        "번역-" + target.getPlayerId()
                                )
                        )
                        .toList();


        return PlayerNameTranslationBatchResult.of(
                players
        );
    }
}