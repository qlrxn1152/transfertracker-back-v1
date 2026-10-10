package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamItemResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamsResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import(MemberFavoriteTeamService.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberFavoriteTeamQueryServiceTest {

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
    @DisplayName("회원이 등록한 관심 팀 목록을 조회할 수 있다.")
    void getFavoriteTeams_success() {

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

        MemberFavoriteTeam arsenalFavorite =
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
        MemberFavoriteTeamsResponseDto response =
                memberFavoriteTeamService.getFavoriteTeams(
                        member.getId()
                );


        // then
        assertThat(response.getFavoriteTeams())
                .hasSize(2);

        assertThat(response.getFavoriteTeams())
                .extracting(
                        MemberFavoriteTeamItemResponseDto::getFavoriteId
                )
                .containsExactlyInAnyOrder(
                        arsenalFavorite.getId(),
                        chelseaFavorite.getId()
                );

        assertThat(response.getFavoriteTeams())
                .extracting(
                        MemberFavoriteTeamItemResponseDto::getTeamId
                )
                .containsExactlyInAnyOrder(
                        arsenal.getId(),
                        chelsea.getId()
                );

        assertThat(response.getFavoriteTeams())
                .extracting(
                        MemberFavoriteTeamItemResponseDto::getTeamName
                )
                .containsExactlyInAnyOrder(
                        "Arsenal",
                        "Chelsea"
                );
    }


    @Test
    @DisplayName("관심 팀 조회 결과에는 팀 로고 URL이 포함된다.")
    void getFavoriteTeams_logoUrl() {

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

        memberFavoriteTeamRepository.save(
                MemberFavoriteTeam.of(
                        member,
                        arsenal
                )
        );


        // when
        MemberFavoriteTeamsResponseDto response =
                memberFavoriteTeamService.getFavoriteTeams(
                        member.getId()
                );


        // then
        MemberFavoriteTeamItemResponseDto favoriteTeam =
                response.getFavoriteTeams()
                        .get(0);

        assertThat(favoriteTeam.getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/42.png"
                );
    }


    @Test
    @DisplayName("관심 팀 조회 시 한글 팀명이 존재하면 한글 이름을 반환한다.")
    void getFavoriteTeams_koreanTeamName() {

        // given
        Member member =
                saveMember(
                        "testMember"
                );

        Team arsenal =
                Team.of(
                        "Arsenal",
                        42L
                );

        arsenal.assignKoTeamName(
                "아스널"
        );

        arsenal =
                teamRepository.save(
                        arsenal
                );

        memberFavoriteTeamRepository.save(
                MemberFavoriteTeam.of(
                        member,
                        arsenal
                )
        );


        // when
        MemberFavoriteTeamsResponseDto response =
                memberFavoriteTeamService.getFavoriteTeams(
                        member.getId()
                );


        // then
        assertThat(response.getFavoriteTeams())
                .extracting(
                        MemberFavoriteTeamItemResponseDto::getTeamName
                )
                .containsExactly(
                        "아스널"
                );
    }


    @Test
    @DisplayName("관심 팀 조회 시 다른 회원의 관심 팀은 포함되지 않는다.")
    void getFavoriteTeams_onlyOwnTeams() {

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

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        49L
                );

        memberFavoriteTeamRepository.save(
                MemberFavoriteTeam.of(
                        memberA,
                        arsenal
                )
        );

        memberFavoriteTeamRepository.save(
                MemberFavoriteTeam.of(
                        memberB,
                        chelsea
                )
        );


        // when
        MemberFavoriteTeamsResponseDto response =
                memberFavoriteTeamService.getFavoriteTeams(
                        memberA.getId()
                );


        // then
        assertThat(response.getFavoriteTeams())
                .hasSize(1);

        assertThat(response.getFavoriteTeams())
                .extracting(
                        MemberFavoriteTeamItemResponseDto::getTeamId
                )
                .containsExactly(
                        arsenal.getId()
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("회원은 존재하지만 등록한 관심 팀이 없으면 빈 목록을 반환한다.")
    void getFavoriteTeams_empty() {

        // given
        Member member =
                saveMember(
                        "testMember"
                );


        // when
        MemberFavoriteTeamsResponseDto response =
                memberFavoriteTeamService.getFavoriteTeams(
                        member.getId()
                );


        // then
        assertThat(response.getFavoriteTeams())
                .isEmpty();
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    @Test
    @DisplayName("존재하지 않는 회원의 관심 팀을 조회하면 예외가 발생한다.")
    void getFavoriteTeams_memberNotFound() {

        // given
        Long invalidMemberId =
                999999L;


        // when & then
        assertThatThrownBy(() ->
                memberFavoriteTeamService.getFavoriteTeams(
                        invalidMemberId
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