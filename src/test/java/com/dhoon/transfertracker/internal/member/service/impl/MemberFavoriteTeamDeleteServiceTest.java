package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import(MemberFavoriteTeamService.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberFavoriteTeamDeleteServiceTest {

    @Autowired
    MemberFavoriteTeamService memberFavoriteTeamService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    MemberFavoriteTeamRepository memberFavoriteTeamRepository;


    @AfterEach
    void clearDatabase() {
        memberFavoriteTeamRepository.deleteAll();
        teamRepository.deleteAll();
        memberRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("회원은 등록한 관심 팀을 삭제할 수 있다.")
    void deleteFavoriteTeams_success() {

        // given
        Member member =
                saveMember(
                        "testMember"
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        49L
                );

        memberFavoriteTeamRepository.save(
                MemberFavoriteTeam.of(
                        member,
                        arsenal
                )
        );

        MemberFavoriteTeam chelseaFavorite =
                memberFavoriteTeamRepository.save(
                        MemberFavoriteTeam.of(
                                member,
                                chelsea
                        )
                );


        // when
        memberFavoriteTeamService.deleteFavoriteTeams(
                member.getId(),
                arsenal.getId()
        );


        // then
        List<MemberFavoriteTeam> favoriteTeams =
                memberFavoriteTeamRepository.findAll();

        assertThat(favoriteTeams)
                .hasSize(1);

        assertThat(favoriteTeams.get(0).getId())
                .isEqualTo(
                        chelseaFavorite.getId()
                );
    }


    @Test
    @DisplayName("관심 팀 삭제는 다른 회원의 동일한 관심 팀에 영향을 주지 않는다.")
    void deleteFavoriteTeams_onlyOwnFavorite() {

        // given
        Member memberA =
                saveMember(
                        "memberA"
                );

        Member memberB =
                saveMember(
                        "memberB"
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );

        memberFavoriteTeamRepository.save(
                MemberFavoriteTeam.of(
                        memberA,
                        arsenal
                )
        );

        MemberFavoriteTeam memberBFavorite =
                memberFavoriteTeamRepository.save(
                        MemberFavoriteTeam.of(
                                memberB,
                                arsenal
                        )
                );


        // when
        memberFavoriteTeamService.deleteFavoriteTeams(
                memberA.getId(),
                arsenal.getId()
        );


        // then
        List<MemberFavoriteTeam> favoriteTeams =
                memberFavoriteTeamRepository.findAll();

        assertThat(favoriteTeams)
                .hasSize(1);

        assertThat(favoriteTeams.get(0).getId())
                .isEqualTo(
                        memberBFavorite.getId()
                );

        assertThat(favoriteTeams.get(0).getMember().getId())
                .isEqualTo(
                        memberB.getId()
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("등록하지 않은 관심 팀을 삭제해도 기존 관심 팀에는 영향을 주지 않는다.")
    void deleteFavoriteTeams_notRegistered_noChange() {

        // given
        Member member =
                saveMember(
                        "testMember"
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        49L
                );

        MemberFavoriteTeam chelseaFavorite =
                memberFavoriteTeamRepository.save(
                        MemberFavoriteTeam.of(
                                member,
                                chelsea
                        )
                );


        // when
        memberFavoriteTeamService.deleteFavoriteTeams(
                member.getId(),
                arsenal.getId()
        );


        // then
        List<MemberFavoriteTeam> favoriteTeams =
                memberFavoriteTeamRepository.findAll();

        assertThat(favoriteTeams)
                .hasSize(1);

        assertThat(favoriteTeams.get(0).getId())
                .isEqualTo(
                        chelseaFavorite.getId()
                );
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    @Test
    @DisplayName("존재하지 않는 회원이 관심 팀 삭제를 요청하면 예외가 발생한다.")
    void deleteFavoriteTeams_memberNotFound() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );

        Long invalidMemberId =
                999999L;


        // when & then
        assertThatThrownBy(() ->
                memberFavoriteTeamService.deleteFavoriteTeams(
                        invalidMemberId,
                        arsenal.getId()
                )
        )
                .isInstanceOf(
                        NotFoundMemberException.class
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private Member saveMember(
            String loginId
    ) {

        return memberRepository.save(
                Member.signUp(
                        loginId,
                        "encoded-password"
                )
        );
    }


    private Team saveTeam(
            String teamName,
            Long apiFootballId
    ) {

        return teamRepository.save(
                Team.of(
                        teamName,
                        apiFootballId
                )
        );
    }
}