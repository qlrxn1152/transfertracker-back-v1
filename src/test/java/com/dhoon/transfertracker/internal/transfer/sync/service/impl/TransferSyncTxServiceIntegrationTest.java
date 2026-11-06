package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.TransferTrackerApplication;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(classes = TransferTrackerApplication.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
        TransferSyncTxService.class,
})
public class TransferSyncTxServiceIntegrationTest {

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @PersistenceContext
    private EntityManager em;

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
    private TransferSyncTxService txService;

    private static JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("신규 선수이고 이적 기록이 없으면 선수만 저장한다.")
    void getOrCreatePlayerTransfersResponse_newPlayer_emptyTransfers() {
        // given
        JsonNode playerData = jsonMapper.readTree("""
            {
                "id": 111,
                "name": "Messi"
            }
            """);

        JsonNode transferDatas = jsonMapper.readTree("[]");

        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        em.flush();
        em.clear();


        // then
        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).isEmpty();

        Player savedPlayer = playerRepository.findByApiFootballId(111L).orElseThrow();

        assertThat(savedPlayer.getId()).isNotNull();
        assertThat(savedPlayer.getDisplayName()).isEqualTo("Messi");
        assertThat(savedPlayer.getApiFootballId()).isEqualTo(111L);
    }

    @Test
    @DisplayName("신규 선수와 팀, 이적 기록을 DB에 저장한다.")
    void getOrCreatePlayerTransfersResponse_newPlayerAndTransfer() {
        // given
        JsonNode playerData = jsonMapper.readTree("""
            {
                "id": 111,
                "name": "Messi"
            }
            """);

        JsonNode transferDatas = jsonMapper.readTree("""
            [
                {
                    "date": "2026-07-01",
                    "type": "Free",
                    "teams": {
                        "in": {
                            "id": 44,
                            "name": "Barcelona"
                        },
                        "out": {
                            "id": 33,
                            "name": "PSG"
                        }
                    }
                }
            ]
            """);

        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        em.flush();
        em.clear();

        // then
        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).hasSize(1);

        PlayerTransferItemResponseDto savedTransferResponseDto = response.getPlayerTransfers().getFirst();

        assertThat(savedTransferResponseDto.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(savedTransferResponseDto.getType()).isEqualTo("Free");
        assertThat(savedTransferResponseDto.getInTeamName()).isEqualTo("Barcelona");
        assertThat(savedTransferResponseDto.getOutTeamName()).isEqualTo("PSG");

        Player savedPlayer = playerRepository.findByApiFootballId(111L).orElseThrow();
        assertThat(savedPlayer).isNotNull();
        assertThat(savedPlayer.getDisplayName()).isEqualTo("Messi");
        assertThat(savedPlayer.getApiFootballId()).isEqualTo(111L);
        assertThat(savedPlayer.getId()).isNotNull();

        Team savedBarcelona = teamRepository.findByApiFootballId(44L).orElseThrow();
        Team savedPsg = teamRepository.findByApiFootballId(33L).orElseThrow();

        Transfer savedTransfer = transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(savedBarcelona.getId(), savedPsg.getId(), savedPlayer.getId(), LocalDate.of(2026, 7, 1))
                .orElseThrow();

        assertThat(savedTransfer.getTransferDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(savedTransfer.getTransferType()).isEqualTo("Free");
        assertThat(savedTransfer.getOutTeam().getId()).isEqualTo(savedPsg.getId());
        assertThat(savedTransfer.getInTeam().getId()).isEqualTo(savedBarcelona.getId());

        assertThat(savedTransfer.getPlayer().getId()).isEqualTo(savedPlayer.getId());
    }

    @Test
    @DisplayName("기존 이적 기록이 존재하면 중복 저장하지 않는다.")
    void getOrCreatePlayerTransfersResponse_existingTransfer() {
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

        Transfer existingTransfer = transferRepository.save(
                Transfer.of(
                        messi,
                        barcelona,
                        psg,
                        "Free",
                        transferDate
                )
        );

        Long originalTransferId = existingTransfer.getId();

        JsonNode playerData = jsonMapper.readTree("""
            {
                "id": 111,
                "name": "Messi"
            }
            """);

        JsonNode transferDatas = jsonMapper.readTree("""
            [
                {
                    "date": "2026-07-01",
                    "type": "Free",
                    "teams": {
                        "in": {
                            "id": 44,
                            "name": "Barcelona"
                        },
                        "out": {
                            "id": 33,
                            "name": "PSG"
                        }
                    }
                }
            ]
            """);

        em.flush();
        em.clear();

        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        em.flush();
        em.clear();

        // then
        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).hasSize(1);

        assertThat(playerRepository.count()).isEqualTo(1L);
        assertThat(teamRepository.count()).isEqualTo(2L);

        Transfer foundTransfer = transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(
                barcelona.getId(),
                psg.getId(),
                messi.getId(),
                LocalDate.of(2026, 7, 1)
        ).orElseThrow();

        assertThat(transferRepository.count()).isEqualTo(1L);
        assertThat(foundTransfer.getId()).isEqualTo(originalTransferId);
    }


}
