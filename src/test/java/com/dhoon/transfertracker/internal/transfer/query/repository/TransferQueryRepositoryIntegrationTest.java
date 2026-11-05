package com.dhoon.transfertracker.internal.transfer.query.repository;

import com.dhoon.transfertracker.config.QueryDslConfig;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.transaction.TestTransaction;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@Slf4j
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TransferQueryRepository.class, QueryDslConfig.class})
class TransferQueryRepositoryIntegrationTest {

    @Container
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver"
        );
    }

    @Test
    void contextLoads() {}

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransferQueryRepository transferQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private static LocalDate transferAt = LocalDate.parse("2026-01-01");

    // ============================ //


    @Test
    @DisplayName("저장된 이적 기록을 실제 MySQL에서 조회한다.")
    void findTransfers_success() {
        // given
        Player messi = Player.of("Messi", 111L);
        Team barcelona = Team.of("Barcelona", 33L);
        Team psg = Team.of("PSG", 44L);
        Transfer transfer = Transfer.of(messi, barcelona, psg, "Free", transferAt);

        playerRepository.save(messi);
        teamRepository.save(barcelona);
        teamRepository.save(psg);
        transferRepository.save(transfer);

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 2);

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "", null, null);

        // then
        assertThat(response.getContent()).hasSize(1);
        Transfer foundTransfer = response.getContent().getFirst();

        assertThat(foundTransfer.getId()).isEqualTo(transfer.getId());
        assertThat(foundTransfer.getPlayer().getPlayerName()).isEqualTo("Messi");
        assertThat(foundTransfer.getInTeam().getDisplayName()).isEqualTo("Barcelona");
        assertThat(foundTransfer.getOutTeam().getDisplayName()).isEqualTo("PSG");
        assertThat(foundTransfer.getTransferType()).isEqualTo("Free");
        assertThat(foundTransfer.getTransferDate()).isEqualTo(transferAt);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("이적 기록은 날짜 내림차순, 동일 날짜는 ID 내림차순으로 조회한다.")
    void findTransfers_orderByTransferDateDescAndIdDesc() {
        Player messi = playerRepository.save(
                Player.of("Messi", 111L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 33L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 44L)
        );

        Team interMiami = teamRepository.save(
                Team.of("Inter Miami", 55L)
        );

        Transfer transferA = transferRepository.save(
                Transfer.of(
                        messi,
                        barcelona,
                        psg,
                        "Free",
                        LocalDate.of(2026, 1, 1)
                )
        ); // psg -> barcelona / 2026-1-1 // 1L

        Transfer transferB = transferRepository.save(
                Transfer.of(
                        messi,
                        psg,
                        barcelona,
                        "Loan",
                        LocalDate.of(2026, 7, 1)
                )
        ); // barcelona -> psg / 2026-7-1 // 2L

        Transfer transferC = transferRepository.save(
                Transfer.of(
                        messi,
                        interMiami,
                        psg,
                        "Permanent",
                        LocalDate.of(2026, 7, 1)
                )
        ); // psg -> interMiami / 2026-7-1 // 3L

        Long transferAId = transferA.getId(); // 1L
        Long transferBId = transferB.getId(); // 2L
        Long transferCId = transferC.getId(); // 3L

        em.flush();
        em.clear();

        // 3L -> 2L -> 1L ==> c -> b -> a

        // 최신이적순, 날짜가 같으면 아이디가 높은순으로.

        Pageable pageable = PageRequest.of(0, 10);


        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "", null, null);

        // then
        assertThat(response.getContent()).hasSize(3);

        assertThat(response.getContent()).extracting(Transfer::getId)
                .containsExactly(transferCId, transferBId, transferAId);

        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("두 번째 페이지 조회 시 정렬된 이적 기록 2건과 전체 페이징 정보를 반환한다.")
    void findTransfers_pagination() {
        // given
        Player messi = playerRepository.save(
                Player.of("Messi", 111L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 33L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 44L)
        );

        // 날짜를 다르게 해서 UNIQUE 충돌 방지
        Transfer transferA = transferRepository.save(
                Transfer.of(messi, barcelona, psg,
                        "Free", LocalDate.of(2026, 1, 1))
        );

        Transfer transferB = transferRepository.save(
                Transfer.of(messi, barcelona, psg,
                        "Free", LocalDate.of(2026, 2, 1))
        );

        Transfer transferC = transferRepository.save(
                Transfer.of(messi, barcelona, psg,
                        "Free", LocalDate.of(2026, 3, 1))
        );

        Transfer transferD = transferRepository.save(
                Transfer.of(messi, barcelona, psg,
                        "Free", LocalDate.of(2026, 4, 1))
        );

        Transfer transferE = transferRepository.save(
                Transfer.of(messi, barcelona, psg,
                        "Free", LocalDate.of(2026, 5, 1))
        );

        // E -> D -> C -> B -> A

        Long transferBId = transferB.getId();
        Long transferCId = transferC.getId();

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(1, 2); // 2번째페이지 -> 3~4 번째 데이터.

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "", null, null);

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent()).extracting(Transfer::getId)
                .containsExactly(transferCId, transferBId);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("팀 ID로 조회하면 입단팀 또는 방출팀에 해당하는 이적 기록만 반환한다.")
    void findTransfers_filterByTeamId() {
        // given
        Player messi = playerRepository.save(
                Player.of("Messi", 111L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 33L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 44L)
        );

        Team interMiami = teamRepository.save(
                Team.of("Inter Miami", 55L)
        );

        // A: Barcelona가 입단팀
        Transfer transferA = transferRepository.save(
                Transfer.of(
                        messi,
                        barcelona,
                        psg,
                        "Free",
                        LocalDate.of(2026, 1, 1)
                )
        ); // psg -> Barcelona가

        // B: Barcelona가 방출팀
        Transfer transferB = transferRepository.save(
                Transfer.of(
                        messi,
                        psg,
                        barcelona,
                        "Loan",
                        LocalDate.of(2026, 7, 1)
                )
        ); // Barcelona -> psg

        // C: Barcelona와 관계없는 이적
        Transfer transferC = transferRepository.save(
                Transfer.of(
                        messi,
                        interMiami,
                        psg,
                        "Permanent",
                        LocalDate.of(2026, 8, 1)
                )
        ); // psg -> interMiami

        Long transferAId = transferA.getId();
        Long transferBId = transferB.getId();
        Long transferCId = transferC.getId();

        Long barcelonaId = barcelona.getId();

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "", null, barcelonaId);

        // then
        assertThat(response.getContent()).hasSize(2);

        assertThat(response.getContent()).extracting(Transfer::getId)
                .containsExactly(transferBId, transferAId);
        assertThat(response.getContent()).extracting(Transfer::getId)
                .doesNotContain(transferCId);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("리그 코드로 조회하면 입단팀 또는 방출팀이 해당 리그에 속한 이적 기록만 반환한다.")
    void findTransfers_filterByLeagueCode() {
        // given
        Player messi = playerRepository.save(
                Player.of("Messi", 111L)
        );

        // EPL
        Team manCity = teamRepository.save(
                Team.of("Manchester City", 33L, LeagueCode.EPL)
        );

        Team arsenal = teamRepository.save(
                Team.of("Arsenal", 44L, LeagueCode.EPL)
        );

        // LA_LIGA
        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 55L, LeagueCode.LA_LIGA)
        );

        Team realMadrid = teamRepository.save(
                Team.of("Real Madrid", 66L, LeagueCode.LA_LIGA)
        );

        // A: 입단팀이 EPL
        Transfer transferA = transferRepository.save(
                Transfer.of(
                        messi,
                        manCity,
                        barcelona,
                        "Free",
                        LocalDate.of(2026, 1, 1)
                )
        );

        // B: 방출팀이 EPL
        Transfer transferB = transferRepository.save(
                Transfer.of(
                        messi,
                        realMadrid,
                        arsenal,
                        "Loan",
                        LocalDate.of(2026, 7, 1)
                )
        );

        // C: 입단팀과 방출팀 모두 LA_LIGA
        Transfer transferC = transferRepository.save(
                Transfer.of(
                        messi,
                        barcelona,
                        realMadrid,
                        "Permanent",
                        LocalDate.of(2026, 8, 1)
                )
        );

        Long transferAId = transferA.getId();
        Long transferBId = transferB.getId();

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "", LeagueCode.EPL, null);

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent()).extracting(Transfer::getId)
                .containsExactly(transferBId, transferAId);
        assertThat(response.getContent()).extracting(Transfer::getId)
                .doesNotContain(transferC.getId());

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("선수 이름으로 검색하면 해당 선수의 이적 기록만 반환한다.")
    @Sql(
            scripts = "/sql/transfer/fulltext-setup.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    @Sql(
            scripts = "/sql/transfer/fulltext-cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    void findTransfers_filterByKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Transfer> response =
                transferQueryRepository.findTransfers(
                        pageable,
                        "Messi",
                        null,
                        null
                );

        // then
        assertThat(response.getContent()).hasSize(1);

        Transfer found = response.getContent().getFirst();

        assertThat(found.getPlayer().getPlayerName())
                .isEqualTo("Lionel Messi");

        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("선수 이름, 리그 코드, 팀 ID 복합 검색 조건을 모두 만족하는 이적 기록만 반환한다.")
    @Sql(
            scripts = "/sql/transfer/fulltext-combined-setup.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
    )
    @Sql(
            scripts = "/sql/transfer/fulltext-combined-cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
    )
    void findTransfers_combinedFilters() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // Barcelona의 DB ID 조회
        Long barcelonaId = teamRepository.findByApiFootballId(910044L)
                .orElseThrow()
                .getId();

        Long messiId = playerRepository.findByApiFootballId(910111L)
                .orElseThrow()
                .getId();

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "Messi", LeagueCode.LA_LIGA, barcelonaId);

        // then
        assertThat(response.getContent()).hasSize(2);

        Transfer transferF = response.getContent().get(0); // F
        assertThat(transferF.getPlayer().getId()).isEqualTo(messiId);
        assertThat(transferF.getPlayer().getPlayerName()).isEqualTo("Lionel Messi");
        assertThat(transferF.getTransferType()).isEqualTo("Permanent");
        assertThat(transferF.getTransferDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(transferF.getInTeam().getDisplayName()).isEqualTo("PSG");
        assertThat(transferF.getOutTeam().getDisplayName()).isEqualTo("Barcelona");

        Transfer transferA = response.getContent().get(1); // A
        assertThat(transferA.getPlayer().getId()).isEqualTo(messiId);
        assertThat(transferA.getPlayer().getPlayerName()).isEqualTo("Lionel Messi");
        assertThat(transferA.getTransferType()).isEqualTo("Free");
        assertThat(transferA.getTransferDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(transferA.getInTeam().getDisplayName()).isEqualTo("Barcelona");
        assertThat(transferA.getOutTeam().getDisplayName()).isEqualTo("PSG");

        assertThat(response.getContent()).extracting(Transfer::getTransferDate)
                .containsExactly(
                        LocalDate.parse("2026-06-01"), // F
                        LocalDate.parse("2026-01-01") // A
                );

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("팀 조건과 리그 조건을 서로 다른 팀이 만족해도 조회한다.")
    @Sql(
            scripts = "/sql/transfer/different-team-league-setup.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    @Sql(
            scripts = "/sql/transfer/different-team-league-cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    void findTransfers_combinedFilters_differentTeamMatchesLeague() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Long barcelonaId = teamRepository
                .findByApiFootballId(920044L)
                .orElseThrow()
                .getId();

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "Messi", LeagueCode.EPL, barcelonaId);

        // then
        assertThat(response.getContent()).hasSize(1);

        Transfer findTransfer = response.getContent().getFirst();
        assertThat(findTransfer.getPlayer().getDisplayName()).isEqualTo("Lionel Messi");
        assertThat(findTransfer.getInTeam().getDisplayName()).isEqualTo("Barcelona");
        assertThat(findTransfer.getOutTeam().getDisplayName()).isEqualTo("Arsenal");
        assertThat(findTransfer.getTransferType()).isEqualTo("Permanent");
        assertThat(findTransfer.getTransferDate()).isEqualTo(LocalDate.of(2026, 7, 1));

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("검색어가 null, 빈 문자열, 공백이면 전체 이적 기록을 조회한다.")
    void findTransfers_nullOrBlankKeyword_returnsAll(String keyword) {
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

        Transfer messiTransfer = transferRepository.save(
                Transfer.of(
                        messi,
                        barcelona,
                        psg,
                        "Free",
                        LocalDate.of(2026, 7, 1)
                )
        );

        Transfer ronaldoTransfer = transferRepository.save(
                Transfer.of(
                        ronaldo,
                        arsenal,
                        barcelona,
                        "Permanent",
                        LocalDate.of(2026, 6, 1)
                )
        );

        Transfer neymarTransfer = transferRepository.save(
                Transfer.of(
                        neymar,
                        psg,
                        arsenal,
                        "Loan",
                        LocalDate.of(2026, 5, 1)
                )
        );

        Long messiTransferId = messiTransfer.getId();
        Long ronaldoTransferId = ronaldoTransfer.getId();
        Long neymarTransferId = neymarTransfer.getId();

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, keyword, null, null);

        // then
        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getContent()).extracting(Transfer::getId)
                .containsExactly(messiTransferId, ronaldoTransferId, neymarTransferId);
        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("검색어와 일치하는 이적 기록이 없으면 빈 페이지를 반환한다.")
    @Sql(
            scripts = "/sql/transfer/fulltext-setup.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    @Sql(
            scripts = "/sql/transfer/fulltext-cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
            config = @SqlConfig(
                    transactionMode = SqlConfig.TransactionMode.ISOLATED
            )
    )
    void findTransfers_noMatchingResults() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "Haaland";

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, keyword, null, null);

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("요청한 페이지가 전체 페이지 범위를 초과하면 빈 페이지와 전체 건수를 반환한다.")
    void findTransfers_pageOutOfRange() {
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

        transferRepository.save(
                Transfer.of(
                        messi,
                        barcelona,
                        psg,
                        "Free",
                        LocalDate.of(2026, 7, 1)
                )
        );

        transferRepository.save(
                Transfer.of(
                        ronaldo,
                        arsenal,
                        barcelona,
                        "Permanent",
                        LocalDate.of(2026, 6, 1)
                )
        );

        transferRepository.save(
                Transfer.of(
                        neymar,
                        psg,
                        arsenal,
                        "Loan",
                        LocalDate.of(2026, 5, 1)
                )
        );

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(2, 2);

        // when
        Page<Transfer> response = transferQueryRepository.findTransfers(pageable, "", null, null);

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getNumberOfElements()).isZero();
        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isTrue();
    }



}
