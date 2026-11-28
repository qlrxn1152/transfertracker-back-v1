package com.dhoon.transfertracker.internal.team.repository;

import com.dhoon.transfertracker.TransferTrackerApplication;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

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

    @Test
    @DisplayName(value = "팀을 정상적으로 저장한다.")
    void saveTeam() throws Exception {
        // given
        Team team = Team.createTeam("testTeam", 22L);

        // when
        Team savedTeam = teamRepository.save(team);

        em.flush();
        em.clear();

        // then
        Team foundTeam = teamRepository.findById(savedTeam.getId()).orElseThrow();

        assertThat(foundTeam.getId()).isEqualTo(savedTeam.getId());
        assertThat(foundTeam.getTeamName()).isEqualTo("testTeam");
        assertThat(foundTeam.getApiFootballId()).isEqualTo(22L);
    }

    @Test
    @DisplayName(value = "동일한 api Football Id 값을 가진경우, 중복 저장될 수 없다.")
    void save_fail_duplicate_apiFootballId() throws Exception {
        // given
        teamRepository.save(Team.createTeam("testTeam", 22L));
        em.flush();
        em.clear();

        Team duplicateTeam = Team.createTeam("aa", 22L);

        // when && then
        assertThatThrownBy(() -> teamRepository.saveAndFlush(duplicateTeam))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName(value = "Api Football Id 의 값이 다른경우, 팀 영문이름이 같아도 저장된다.")
    void save_duplicate_teamName() throws Exception {
        // given
        teamRepository.saveAndFlush(Team.createTeam("testTeam", 22L));
        em.clear();

        Team duplicateTeam = Team.createTeam("testTeam", 44L);

        // when
        Team savedDuplicateTeam = teamRepository.saveAndFlush(duplicateTeam);
        em.clear();

        // then
        Team foundDuplicateTeam = teamRepository.findById(savedDuplicateTeam.getId()).orElseThrow();

        assertThat(foundDuplicateTeam.getId()).isEqualTo(savedDuplicateTeam.getId());
        assertThat(foundDuplicateTeam.getTeamName()).isEqualTo("testTeam");

        assertThat(teamRepository.findAll()).hasSize(2);
        assertThat(teamRepository.findAll()).extracting(Team::getTeamName).containsOnly("testTeam");
    }

    @Test
    @DisplayName(value = "Api Football Id 의 값이 다른경우, 동일한 한국어 이름을 가질 수 있다.")
    void save_duplicate_teamNameKo() throws Exception {
        // given
        Team t1 = Team.createTeam("testTeam1", 22L);
        Team t2 = Team.createTeam("testTeam2", 44L);

        t1.assignKoTeamName("한국어 팀이름");
        t2.assignKoTeamName("한국어 팀이름");

        // when
        teamRepository.saveAllAndFlush(List.of(t1, t2));

        // then
        assertThat(teamRepository.findAll()).hasSize(2);
        assertThat(teamRepository.findAll()).extracting(Team::getTeamName).containsExactlyInAnyOrder("testTeam1", "testTeam2");
        assertThat(teamRepository.findAll()).extracting(Team::getApiFootballId).containsExactlyInAnyOrder(22L, 44L);
        assertThat(teamRepository.findAll()).extracting(Team::getTeamNameKo).containsOnly("한국어 팀이름");
    }

    @Test
    @DisplayName(value = "Api Football Id 로 팀을 조회할 수 있다.")
    void findByApiFootballId() throws Exception {
        // given
        Team team = Team.createTeam("testTeam", 22L);
        teamRepository.saveAndFlush(team);

        em.clear();

        // when
        Team foundTeam = teamRepository.findByApiFootballId(22L).orElseThrow();

        // then
        assertThat(foundTeam.getApiFootballId()).isEqualTo(22L);
        assertThat(foundTeam.getTeamName()).isEqualTo("testTeam");
    }

    @Test
    @DisplayName(value = "존재하지 않는 Api Football Id로 조회시, 빈 Optional 을 반환한다.")
    void findBy_invalid_footballId() throws Exception {
        // when
        Optional<Team> result = teamRepository.findByApiFootballId(123124L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName(value = "여러 Api FootballId 로 팀들을 조회할 수 있다.")
    void findAll_apiFootballId() throws Exception {
        // given
        teamRepository.saveAllAndFlush(
                List.of(
                        Team.createTeam("team1", 22L),
                        Team.createTeam("team2", 33L),
                        Team.createTeam("team3", 44L)
                )
        );

        em.clear();

        // when
        List<Team> teams = teamRepository.findAllByApiFootballIdIn(List.of(22L, 33L, 44L));

        // then
        assertThat(teams).hasSize(3);
        assertThat(teams).extracting(Team::getApiFootballId)
                .containsExactlyInAnyOrder(22L, 33L, 44L);
        assertThat(teams).extracting(Team::getTeamName)
                .containsExactlyInAnyOrder("team1", "team2", "team3");
    }

    @Test
    @DisplayName(value = "특정 리그에 속한 팀들을 조회할 수 있다.")
    void findAllByLeagueCode() throws Exception {
        // given
        Team arsenal = Team.createTeam("Arsenal", 42L);
        Team chelsea = Team.createTeam("Chelsea", 49L);
        Team madrid = Team.createTeam("Real Madrid", 541L);

        arsenal.assignTeamLeague(LeagueCode.EPL);
        chelsea.assignTeamLeague(LeagueCode.EPL);
        madrid.assignTeamLeague(LeagueCode.LA_LIGA);

        teamRepository.saveAllAndFlush(List.of(arsenal, chelsea, madrid));

        em.clear();

        // when
        List<Team> eplTeams = teamRepository.findAllByLeagueCode(LeagueCode.EPL);

        // then
        assertThat(eplTeams).hasSize(2);
        assertThat(eplTeams).extracting(Team::getLeagueCode)
                .containsOnly(LeagueCode.EPL);
        assertThat(eplTeams).extracting(Team::getApiFootballId)
                .containsExactlyInAnyOrder(42L, 49L);
    }

    @Test
    @DisplayName(value = "특정 리그에 속한 팀이 존재하지 않으면, 빈 리스트를 반환한다.")
    void findAllByLeagueCode_empty() throws Exception {
        // when
        List<Team> eplTeams = teamRepository.findAllByLeagueCode(LeagueCode.EPL);

        // then
        assertThat(eplTeams).isEmpty();
    }

    @Test
    @DisplayName(value = "리그에 속해있는 팀들을 조회할 수 있다.")
    void findAllByLeagueCodeIsNotNull() throws Exception {
        // given
        Team arsenal = Team.createTeam("Arsenal", 42L);
        Team chelsea = Team.createTeam("Chelsea", 49L);
        Team madrid = Team.createTeam("Real Madrid", 541L);

        arsenal.assignTeamLeague(LeagueCode.EPL);
        chelsea.assignTeamLeague(LeagueCode.EPL);
        madrid.assignTeamLeague(LeagueCode.LA_LIGA);

        teamRepository.saveAllAndFlush(List.of(arsenal, chelsea, madrid));

        em.clear();

        // when
        List<Team> leagueTeams = teamRepository.findAllByLeagueCodeIsNotNull();

        // then
        assertThat(leagueTeams).hasSize(3);
        assertThat(leagueTeams).extracting(Team::getTeamName)
                .containsExactlyInAnyOrder("Arsenal", "Chelsea", "Real Madrid");
        assertThat(leagueTeams).extracting(Team::getApiFootballId)
                .containsExactlyInAnyOrder(42L, 49L, 541L);
    }

    @Test
    @DisplayName(value = "리그에 속해있는 팀이 존재하지않으면, 빈 리스트를 반환한다..")
    void findAllByLeagueCodeIsNotNull_empty() throws Exception {
        // when
        List<Team> leagueTeams = teamRepository.findAllByLeagueCodeIsNotNull();

        // then
        assertThat(leagueTeams).isEmpty();
    }


    @Test
    @DisplayName("영문 팀 이름으로 팀을 조회한다")
    void findAllByAnyDisplayNameWithEnglishName() {
        // given
        Team team = Team.createTeam("Arsenal", 42L);

        team.assignKoTeamName("아스널");

        teamRepository.saveAndFlush(team);

        em.clear();

        // when
        List<Team> teams = teamRepository.findAllByAnyDisplayName("Arsenal");

        // then
        assertThat(teams).hasSize(1);

        assertThat(teams.getFirst().getApiFootballId())
                .isEqualTo(42L);
    }

    @Test
    @DisplayName("한국어 팀 이름으로 팀을 조회한다")
    void findAllByAnyDisplayNameWithKoreanName() {
        // given
        Team team = Team.createTeam("Arsenal", 42L);

        team.assignKoTeamName("아스널");

        teamRepository.saveAndFlush(team);

        em.clear();

        // when
        List<Team> teams =
                teamRepository.findAllByAnyDisplayName("아스널");

        // then
        assertThat(teams).hasSize(1);

        assertThat(teams.getFirst().getApiFootballId())
                .isEqualTo(42L);
    }

    @Test
    @DisplayName("팀 이름의 대소문자를 구분하지 않고 조회한다")
    void findAllByAnyDisplayNameIgnoreCase() {
        // given
        Team team = Team.createTeam("Arsenal", 42L);

        teamRepository.saveAndFlush(team);

        em.clear();

        // when
        List<Team> teams =
                teamRepository.findAllByAnyDisplayName("ARSENAL");

        // then
        assertThat(teams).hasSize(1);

        assertThat(teams.getFirst().getTeamName())
                .isEqualTo("Arsenal");
    }












}
