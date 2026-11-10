package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.TransferTrackerApplication;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = TransferTrackerApplication.class)
@Transactional
@ActiveProfiles("test")
public class TransferPostTeamRepositoryTest {

    @Autowired
    private TransferPostTeamRepository transferPostTeamRepository;

    @Autowired
    private TransferPostRepository transferPostRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
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

    @Test
    @DisplayName("하나의 이적 게시물에 여러 팀을 연결할 수 있다")
    void saveTransferPostWithMultipleTeams() {
        // given
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        TransferPost post1 = TransferPost.of(TransferPostSource.FABRIZIO_ROMANO, "content-1", "id-100", now.minusSeconds(3600), false);
        transferPostRepository.save(post1);

        Team team1 = Team.of("team-1", 111L);
        Team team2 = Team.of("team-2", 222L);

        teamRepository.save(team1);
        teamRepository.save(team2);

        transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team1, post1));
        transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team2, post1));

        em.flush();
        em.clear();

        // when
        List<TransferPostTeam> response = transferPostTeamRepository.findAllByPostId(post1.getId());

        // then
        assertThat(response).hasSize(2);

        assertThat(response).extracting(tpt -> tpt.getTeam().getApiFootballId())
                .containsExactlyInAnyOrder(111L, 222L);

        assertThat(response).extracting(tpt -> tpt.getPost().getExternalPostId())
                .containsExactlyInAnyOrder("id-100", "id-100");
    }

    @Test
    @DisplayName("동일한 게시물과 팀을 중복 연결할 수 없다")
    void saveDuplicateTransferPostTeam() {
        // given
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        TransferPost post1 = TransferPost.of(TransferPostSource.FABRIZIO_ROMANO, "content-1", "id-100", now.minusSeconds(3600), false);
        transferPostRepository.save(post1);

        Team team1 = Team.of("team-1", 111L);

        teamRepository.save(team1);

        transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team1, post1));

        em.flush();
        em.clear();

        // when && then
        List<TransferPostTeam> response = transferPostTeamRepository.findAllByPostId(post1.getId());

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getTeam().getApiFootballId()).isEqualTo(111L);
        assertThat(response.getFirst().getPost().getExternalPostId()).isEqualTo("id-100");

        assertThatThrownBy(() -> transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team1, post1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }





}
