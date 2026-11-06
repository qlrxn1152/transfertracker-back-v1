
package com.dhoon.transfertracker.internal.transfer.repository;

import com.dhoon.transfertracker.TransferTrackerApplication;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(classes = TransferTrackerApplication.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class TransferRepositoryIntegrationTest {

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
    private TransferRepository transferRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("동일한 이적 기록을 저장하면 UNIQUE 제약 조건 위반 예외가 발생한다.")
    void save_duplicateTransfer_throwsException() {
        // given
        Player messi = playerRepository.save(
                Player.of("Messi", 111L)
        );

        Team barcelona = teamRepository.save(
                Team.of("Barcelona", 44L)
        );

        Team psg = teamRepository.save(
                Team.of("PSG", 33L)
        );

        LocalDate transferDate = LocalDate.of(2026, 7, 1);

        Transfer firstTransfer = Transfer.of(
                messi,
                barcelona,
                psg,
                "Free",
                transferDate
        );

        Transfer duplicateTransfer = Transfer.of(
                messi,
                barcelona,
                psg,
                "Permanent",
                transferDate
        );

        transferRepository.saveAndFlush(firstTransfer);

        // when & then
        assertThatThrownBy(() ->
                transferRepository.saveAndFlush(duplicateTransfer)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
