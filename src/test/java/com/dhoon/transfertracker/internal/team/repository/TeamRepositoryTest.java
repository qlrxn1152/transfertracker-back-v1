package com.dhoon.transfertracker.internal.team.repository;

import com.dhoon.transfertracker.TransferTrackerApplication;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryTxService;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostTeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
public class TeamRepositoryTest {

    @Container
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamPlayerRepository teamPlayerRepository;

    @Autowired
    private TransferPostTeamRepository transferPostTeamRepository;

    @Autowired
    private TransferPostRepository transferPostRepository;

    @Test
    @DisplayName(value = "존재하는 팀을 조회 후, 존재하는경우 팀을 반환한다.")
    void getTeam() throws Exception {
        // given
        Team team = Team.of("testTeam", 111L);

        Team savedTeam = teamRepository.save(team);
        Long savedTeamId = savedTeam.getId();

        em.flush();
        em.clear();

        // when
        Team findTeam = teamRepository.findById(savedTeamId).orElseThrow();

        // then
        assertThat(findTeam.getId()).isEqualTo(savedTeamId);
        assertThat(findTeam.getTeamName()).isEqualTo("testTeam");
        assertThat(findTeam.getApiFootballId()).isEqualTo(111L);
    }

    @Test
    @DisplayName(value = "존재하지 않는 팀을 조회 시, 빈 Optional 을 반환한다.")
    void getTeam_notFound() throws Exception {
        // given
        Long notExistTeamId = 123L;

        // when
        Optional<Team> findTeam = teamRepository.findById(notExistTeamId);

        // then
        assertThat(findTeam).isEmpty();
    }

    @Test
    @DisplayName(value = "리그에 속해있는 모든팀들을 조회할 수 있다.")
    void getLeagueCodeAssignedTeams() throws Exception {
        // given
        Team t1 = Team.of("team-1", 111L);
        Team t2 = Team.of("team-2", 222L);
        Team t3 = Team.of("team-3", 333L);

        t1.assignTeamLeague(LeagueCode.EPL);
        t2.assignTeamLeague(LeagueCode.EPL);
        t3.assignTeamLeague(LeagueCode.LA_LIGA);

        teamRepository.save(t1);
        teamRepository.save(t2);
        teamRepository.save(t3);

        // when
        List<Team> leagueTeams = teamRepository.findAllByLeagueCodeIsNotNull();

        // then
        assertThat(leagueTeams).hasSize(3);
        assertThat(leagueTeams).contains(t1, t2, t3);
    }

    @Test
    @DisplayName(value = "리그에 속해있는 팀이 존재하지 않는경우, 빈 리스트를 반환한다.")
    void getLeagueCodeAssignedTeams_empty() throws Exception {
        // when
        List<Team> leagueTeams = teamRepository.findAllByLeagueCodeIsNotNull();

        // then
        assertThat(leagueTeams).isEmpty();
    }

    @Test
    @DisplayName(value = "특정 리그에 속해있는 팀들만 조회할 수 있다.")
    void get_specificLeagueTeams() throws Exception {
        // given
        Team t1 = Team.of("team-1", 111L);
        Team t2 = Team.of("team-2", 222L);
        Team t3 = Team.of("team-3", 333L);

        t1.assignTeamLeague(LeagueCode.EPL);
        t2.assignTeamLeague(LeagueCode.EPL);
        t3.assignTeamLeague(LeagueCode.LA_LIGA);

        teamRepository.save(t1);
        teamRepository.save(t2);
        teamRepository.save(t3);

        // when
        List<Team> eplTeams = teamRepository.findAllByLeagueCode(LeagueCode.EPL);

        // then
        assertThat(eplTeams).hasSize(2);
        assertThat(eplTeams).contains(t1, t2);
    }

    @Test
    @DisplayName(value = "특정 리그에 속해있는 팀이 존재하지 않는경우, 빈 리스트를 반환한다.")
    void get_specificLeagueTeams_empty() throws Exception {
        // when
        List<Team> leagueTeams = teamRepository.findAllByLeagueCode(LeagueCode.EPL);

        // then
        assertThat(leagueTeams).isEmpty();
    }

    @Test
    @DisplayName(value = "존재하는 팀에대해서, 팀 상세정보를 조회할 수 있다.")
    void getTeamInfo() throws Exception {
        // given
        Team team = teamRepository.save(Team.of("testTeam", 111L));
        Player player1 = playerRepository.save(Player.of("player-1", 33L));
        Player player2 = playerRepository.save(Player.of("player-2", 34L));
        TeamPlayer teamPlayer1 = teamPlayerRepository.save(TeamPlayer.of(team, player1));
        TeamPlayer teamPlayer2 = teamPlayerRepository.save(TeamPlayer.of(team, player2));

        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        TransferPost post1 = transferPostRepository.save(TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 1",
                "post-100",
                now.minusSeconds(3600),
                true
        ));

        TransferPost post2 = transferPostRepository.save(TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 2",
                "post-101",
                now.minusSeconds(1800),
                true
        ));


        TransferPostTeam teamPost1 = transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post1));
        TransferPostTeam teamPost2 = transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post2));

        em.flush();
        em.clear();

        // when
        Team findTeam = teamRepository.findById(team.getId()).orElseThrow();
        long teamPlayerCount = teamPlayerRepository.countByTeamId(team.getId());
        List<TeamPlayer> teamPlayers = teamPlayerRepository.findAllByTeamIdWithLazyEntity(team.getId());
        Page<TransferPostTeam> teamPosts = transferPostTeamRepository.findAllByTeamId(team.getId(),
                PageRequest.of(
                        0,
                        20,
                        Sort.by(
                                Sort.Order.desc("post.contentCreatedAt"),
                                Sort.Order.desc("post.id")
                        )
                ));

        // then
        assertThat(findTeam.getTeamName()).isEqualTo("testTeam");
        assertThat(findTeam.getApiFootballId()).isEqualTo(111L);
        assertThat(findTeam.getId()).isNotNull();

        assertThat(teamPlayerCount).isEqualTo(2L);

        assertThat(teamPlayers).hasSize(2);
        assertThat(teamPlayers).extracting(TeamPlayer::getPlayer).extracting(Player::getPlayerName)
                .containsExactlyInAnyOrder("player-1", "player-2");
        assertThat(teamPlayers).extracting(TeamPlayer::getTeam).extracting(Team::getTeamName)
                .containsOnly("testTeam");

        assertThat(teamPosts.hasPrevious()).isFalse();
        assertThat(teamPosts.hasNext()).isFalse();
        assertThat(teamPosts.getContent()).hasSize(2);
        assertThat(teamPosts.getContent()).extracting(TransferPostTeam::getPost).extracting(TransferPost::getExternalPostId)
                .containsExactly("post-101", "post-100");
    }

    @Test
    @DisplayName(value = "팀 플레이어가 존재하지 않는경우, 팀 플레이어에 관한 정보는 빈 리스트를 반환한다.")
    void getTeamInfo_empty_teamPlayer() throws Exception {
        // given
        Team team = teamRepository.save(Team.of("testTeam", 111L));

        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        TransferPost post1 = transferPostRepository.save(TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 1",
                "post-100",
                now.minusSeconds(3600),
                true
        ));

        TransferPost post2 = transferPostRepository.save(TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Romano transfer news 2",
                "post-101",
                now.minusSeconds(1800),
                true
        ));


        TransferPostTeam teamPost1 = transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post1));
        TransferPostTeam teamPost2 = transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post2));

        em.flush();
        em.clear();

        // when
        Team findTeam = teamRepository.findById(team.getId()).orElseThrow();
        long teamPlayerCount = teamPlayerRepository.countByTeamId(team.getId());
        List<TeamPlayer> teamPlayers = teamPlayerRepository.findAllByTeamIdWithLazyEntity(team.getId());
        Page<TransferPostTeam> teamPosts = transferPostTeamRepository.findAllByTeamId(team.getId(),
                PageRequest.of(
                        0,
                        20,
                        Sort.by(
                                Sort.Order.desc("post.contentCreatedAt"),
                                Sort.Order.desc("post.id")
                        )
                ));


        // then
        assertThat(findTeam.getTeamName()).isEqualTo("testTeam");
        assertThat(findTeam.getApiFootballId()).isEqualTo(111L);
        assertThat(findTeam.getId()).isNotNull();

        assertThat(teamPlayerCount).isZero();
        assertThat(teamPlayers).isEmpty();

        assertThat(teamPosts.hasPrevious()).isFalse();
        assertThat(teamPosts.hasNext()).isFalse();
        assertThat(teamPosts.getContent()).hasSize(2);
        assertThat(teamPosts.getContent()).extracting(TransferPostTeam::getPost).extracting(TransferPost::getExternalPostId)
                .containsExactly("post-101", "post-100");

    }

    @Test
    @DisplayName(value = "해당 팀 소식이 없는경우, 빈 리스트를 반환한다.")
    void getTeamInfo_empty_teamPost() throws Exception {
        // given
        Team team = teamRepository.save(Team.of("testTeam", 111L));
        Player player1 = playerRepository.save(Player.of("player-1", 33L));
        Player player2 = playerRepository.save(Player.of("player-2", 34L));
        TeamPlayer teamPlayer1 = teamPlayerRepository.save(TeamPlayer.of(team, player1));
        TeamPlayer teamPlayer2 = teamPlayerRepository.save(TeamPlayer.of(team, player2));

        em.flush();
        em.clear();

        // when
        Team findTeam = teamRepository.findById(team.getId()).orElseThrow();
        long teamPlayerCount = teamPlayerRepository.countByTeamId(team.getId());
        List<TeamPlayer> teamPlayers = teamPlayerRepository.findAllByTeamIdWithLazyEntity(team.getId());
        Page<TransferPostTeam> teamPosts = transferPostTeamRepository.findAllByTeamId(team.getId(),
                PageRequest.of(
                        0,
                        20,
                        Sort.by(
                                Sort.Order.desc("post.contentCreatedAt"),
                                Sort.Order.desc("post.id")
                        )
                ));

        // then
        assertThat(findTeam.getTeamName()).isEqualTo("testTeam");
        assertThat(findTeam.getApiFootballId()).isEqualTo(111L);
        assertThat(findTeam.getId()).isNotNull();

        assertThat(teamPlayerCount).isEqualTo(2L);

        assertThat(teamPlayers).hasSize(2);
        assertThat(teamPlayers).extracting(TeamPlayer::getPlayer).extracting(Player::getPlayerName)
                .containsExactlyInAnyOrder("player-1", "player-2");
        assertThat(teamPlayers).extracting(TeamPlayer::getTeam).extracting(Team::getTeamName)
                .containsOnly("testTeam");

        assertThat(teamPosts.hasPrevious()).isFalse();
        assertThat(teamPosts.hasNext()).isFalse();
        assertThat(teamPosts.getContent()).isEmpty();
    }

}
