import com.dhoon.transfertracker.TransferTrackerApplication;
import com.dhoon.transfertracker.config.QueryDslConfig;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.exception.NotFoundTransferException;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.*;
import com.dhoon.transfertracker.internal.transfer.query.repository.TransferQueryRepository;
import com.dhoon.transfertracker.internal.transfer.query.service.impl.TransferQueryTxService;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;


import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(classes = TransferTrackerApplication.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
        TransferQueryTxService.class,
        TransferQueryRepository.class,
        QueryDslConfig.class
})
class TransferQueryTxServiceIntegrationTest {

    @Container
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TransferQueryTxService transferQueryTxService;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @PersistenceContext
    private EntityManager em;


    @Test
    @DisplayName("선수의 이적 기록을 실제 DB에서 조회해 DTO로 반환한다.")
    void getPlayerTransfers_success() {
        // given
        Player messi = playerRepository.save(
                Player.of("Lionel Messi", 111L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 44L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 55L)
        );

        Team interMiami = teamRepository.save(
                Team.of("Inter Miami", 66L)
        );

        transferRepository.save(
                Transfer.of( // barcelona -> psg
                        messi,
                        psg,
                        barcelona,
                        "Permanent",
                        LocalDate.of(2021, 8, 10)
                )
        );

        transferRepository.save(
                Transfer.of( // psg -> interMiami
                        messi,
                        interMiami,
                        psg,
                        "Free",
                        LocalDate.of(2023, 7, 15)
                )
        );

        Long messiId = messi.getId();

        em.flush();
        em.clear();

        // when
        PlayerTransfersResponseDto response = transferQueryTxService.getPlayerTransfers(messiId);

        // then
        assertThat(response.getPlayerName()).isEqualTo("Lionel Messi");
        assertThat(response.getPlayerTransfers()).hasSize(2);
        assertThat(response.getPlayerTransfers()).extracting(PlayerTransferItemResponseDto::getOutTeamName)
                .containsExactly("PSG", "Barcelona");
        assertThat(response.getPlayerTransfers()).extracting(PlayerTransferItemResponseDto::getInTeamName)
                .containsExactly("Inter Miami", "PSG");
        assertThat(response.getPlayerTransfers()).extracting(PlayerTransferItemResponseDto::getDate)
                .containsExactly(LocalDate.of(2023, 7, 15), LocalDate.of(2021, 8, 10));
    }

    @Test
    @DisplayName("선수는 존재하지만 이적 기록이 없으면 빈 목록을 반환한다.")
    void getPlayerTransfers_empty() {
        // given
        Player messi = playerRepository.save(
                Player.of("Lionel Messi", 111L)
        );

        Long messiId = messi.getId();

        em.flush();
        em.clear();

        // when
        PlayerTransfersResponseDto response = transferQueryTxService.getPlayerTransfers(messiId);

        // then
        assertThat(response.getPlayerName()).isEqualTo("Lionel Messi");
        assertThat(response.getPlayerTransfers()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 선수의 이적 기록을 조회하면 예외가 발생한다.")
    void getPlayerTransfers_notFound() {
        // given
        Long playerId = 999999L;

        // when & then
        assertThatThrownBy(() -> transferQueryTxService.getPlayerTransfers(playerId))
                .isInstanceOf(NotFoundPlayerException.class);
    }

    @Test
    @DisplayName("이적 기록을 실제 DB에서 조회해 상세 DTO로 반환한다.")
    void getTransfer_success() {
        // given
        Player messi = playerRepository.save(
                Player.of("Lionel Messi", 111L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 44L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 55L)
        );

        Transfer transfer = transferRepository.save(
                Transfer.of(
                        messi,
                        psg,
                        barcelona,
                        "Free",
                        LocalDate.of(2021, 8, 10)
                )
        );

        Long transferId = transfer.getId();
        Long playerId = messi.getId();
        Long inTeamId = psg.getId();
        Long outTeamId = barcelona.getId();

        em.flush();
        em.clear();

        // when
        TransferDetailResponseDto response = transferQueryTxService.getTransfer(transferId);

        // then
        TransferResponseDto result = response.getTransfer();

        assertThat(result).isNotNull();
        assertThat(result.getTransferId()).isEqualTo(transferId);
        assertThat(result.getPlayerId()).isEqualTo(playerId);
        assertThat(result.getInTeamId()).isEqualTo(inTeamId);
        assertThat(result.getOutTeamId()).isEqualTo(outTeamId);
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2021, 8, 10));
        assertThat(result.getType()).isEqualTo("Free");
    }

    @Test
    @DisplayName("존재하지 않는 이적 기록을 조회하면 예외가 발생한다.")
    void getTransfer_notFound() {
        // given
        Long transferId = 999999L;

        // when & then
        assertThatThrownBy(() -> transferQueryTxService.getTransfer(transferId))
                .isInstanceOf(NotFoundTransferException.class);
    }

    @Test
    @DisplayName("전체 이적 기록을 실제 DB에서 조회해 DTO와 페이지 정보를 반환한다.")
    void getTransfers_success() {
        // given
        Player messi = playerRepository.save(
                Player.of("Lionel Messi", 111L)
        );

        Player ronaldo = playerRepository.save(
                Player.of("Cristiano Ronaldo", 222L)
        );

        Player neymar = playerRepository.save(
                Player.of("Neymar Junior", 333L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 44L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 55L)
        );

        Team arsenal = teamRepository.save(
                Team.of("Arsenal", 66L)
        );

        Transfer transferA = transferRepository.save(
                Transfer.of(
                        messi, barcelona, psg,
                        "Free", LocalDate.of(2026, 7, 1)
                )
        );

        Transfer transferB = transferRepository.save(
                Transfer.of(
                        ronaldo, arsenal, barcelona,
                        "Permanent", LocalDate.of(2026, 6, 1)
                )
        );

        transferRepository.save(
                Transfer.of(
                        neymar, psg, arsenal,
                        "Loan", LocalDate.of(2026, 5, 1)
                )
        );

        Long transferAId = transferA.getId();
        Long transferBId = transferB.getId();

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 2);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTransfers(pageable, "", null, null);

        // then
        assertThat(response.getTransfers()).hasSize(2);
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getTransferId)
                .containsExactly(transferAId, transferBId);
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("이적 기록이 없으면 빈 목록과 페이지 정보를 반환한다.")
    void getTransfers_empty() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTransfers(pageable, "", null, null);

        // then
        assertThat(response.getTransfers()).isEmpty();
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("복합 검색 조건을 적용한 이적 기록을 DTO로 반환한다.")
    @Sql(
            scripts = "/sql/transfer/fulltext-combined-setup.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    @Sql(
            scripts = "/sql/transfer/fulltext-combined-cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    void getTransfers_withFilters() {
        // given
        Pageable pageable = PageRequest.of(0, 2);

        Long barcelonaId = teamRepository
                .findByApiFootballId(910044L)
                .orElseThrow()
                .getId();

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTransfers(pageable, "Messi", LeagueCode.LA_LIGA, barcelonaId);

        // then
        assertThat(response.getTransfers()).hasSize(2);
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getDate)
                .containsExactly(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1));
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("특정 팀의 입단 및 방출 이적 기록을 모두 조회한다.")
    void getTeamTransfers_success() {
        // given
        Player messi = playerRepository.save(
                Player.of("Lionel Messi", 111L)
        );

        Player ronaldo = playerRepository.save(
                Player.of("Cristiano Ronaldo", 222L)
        );

        Player neymar = playerRepository.save(
                Player.of("Neymar Junior", 333L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 44L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 55L)
        );

        Team arsenal = teamRepository.save(
                Team.of("Arsenal", 66L)
        );

// Messi: Barcelona -> PSG
        Transfer messiTransfer = transferRepository.save(
                Transfer.of(
                        messi, psg, barcelona,
                        "Free", LocalDate.of(2026, 7, 1)
                )
        );

// Ronaldo: Arsenal -> Barcelona
        Transfer ronaldoTransfer = transferRepository.save(
                Transfer.of(
                        ronaldo, barcelona, arsenal,
                        "Permanent", LocalDate.of(2026, 6, 1)
                )
        );

// Neymar: Arsenal -> PSG (조회 제외)
        transferRepository.save(
                Transfer.of(
                        neymar, psg, arsenal,
                        "Loan", LocalDate.of(2026, 5, 1)
                )
        );

        Long barcelonaId = barcelona.getId();
        Long messiTransferId = messiTransfer.getId();
        Long ronaldoTransferId = ronaldoTransfer.getId();

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTeamTransfers(barcelonaId, pageable, "");

        // then
        assertThat(response.getTransfers()).hasSize(2);
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getTransferId)
                .containsExactly(messiTransferId, ronaldoTransferId);
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getDate)
                .containsExactly(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 1));
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("팀은 존재하지만 이적 기록이 없으면 빈 목록을 반환한다.")
    void getTeamTransfers_empty() {
        // given
        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 44L)
        );

        Long barcelonaId = barcelona.getId();

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTeamTransfers(barcelonaId, pageable, "");

        // then
        assertThat(response.getTransfers()).isEmpty();
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 팀의 이적 기록 조회 시 예외가 발생한다.")
    void getTeamTransfers_notFoundTeam() {
        // given
        Long teamId = 999999L;
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> transferQueryTxService.getTeamTransfers(teamId, pageable, ""))
                .isInstanceOf(NotFoundTeamException.class);
    }

}