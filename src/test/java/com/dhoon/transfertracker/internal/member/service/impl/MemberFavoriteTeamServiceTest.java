package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteTeamRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.exception.AlreadyRegisteredFavoriteTeamException;
import com.dhoon.transfertracker.internal.member.exception.InvalidFavoriteTeamRequestException;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
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
class MemberFavoriteTeamServiceTest {

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

        // FK 자식부터 삭제
        memberFavoriteTeamRepository.deleteAll();

        teamRepository.deleteAll();
        memberRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("회원은 여러 팀을 관심 팀으로 한 번에 등록할 수 있다.")
    void registerFavoriteTeams_success() {

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

        Team manUnited =
                saveTeam(
                        "Manchester United",
                        33L
                );

        MemberFavoriteTeamRegisterRequestDto request =
                new MemberFavoriteTeamRegisterRequestDto(
                        List.of(
                                arsenal.getId(),
                                chelsea.getId(),
                                manUnited.getId()
                        )
                );


        // when
        memberFavoriteTeamService.registerFavoriteTeams(
                member.getId(),
                request
        );


        // then
        List<MemberFavoriteTeam> favoriteTeams =
                memberFavoriteTeamRepository.findAll();

        assertThat(favoriteTeams)
                .hasSize(3);

        assertThat(favoriteTeams)
                .extracting(
                        favoriteTeam ->
                                favoriteTeam.getMember().getId()
                )
                .containsOnly(
                        member.getId()
                );

        assertThat(favoriteTeams)
                .extracting(
                        favoriteTeam ->
                                favoriteTeam.getTeam().getId()
                )
                .containsExactlyInAnyOrder(
                        arsenal.getId(),
                        chelsea.getId(),
                        manUnited.getId()
                );
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    @Test
    @DisplayName("요청에 동일한 팀 ID가 중복되면 관심 팀 등록에 실패한다.")
    void registerFavoriteTeams_duplicateTeamIds() {

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

        MemberFavoriteTeamRegisterRequestDto request =
                new MemberFavoriteTeamRegisterRequestDto(
                        List.of(
                                arsenal.getId(),
                                arsenal.getId()
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                memberFavoriteTeamService.registerFavoriteTeams(
                        member.getId(),
                        request
                )
        )
                .isInstanceOf(
                        InvalidFavoriteTeamRequestException.class
                );

        assertThat(memberFavoriteTeamRepository.count())
                .isZero();
    }


    @Test
    @DisplayName("존재하지 않는 회원이면 관심 팀 등록에 실패한다.")
    void registerFavoriteTeams_memberNotFound() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );

        Long invalidMemberId =
                999999L;

        MemberFavoriteTeamRegisterRequestDto request =
                new MemberFavoriteTeamRegisterRequestDto(
                        List.of(
                                arsenal.getId()
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                memberFavoriteTeamService.registerFavoriteTeams(
                        invalidMemberId,
                        request
                )
        )
                .isInstanceOf(
                        NotFoundMemberException.class
                );

        assertThat(memberFavoriteTeamRepository.count())
                .isZero();
    }


    @Test
    @DisplayName("요청한 관심 팀 중 존재하지 않는 팀이 있으면 전체 등록에 실패한다.")
    void registerFavoriteTeams_teamNotFound() {

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

        Long invalidTeamId =
                999999L;

        MemberFavoriteTeamRegisterRequestDto request =
                new MemberFavoriteTeamRegisterRequestDto(
                        List.of(
                                arsenal.getId(),
                                invalidTeamId
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                memberFavoriteTeamService.registerFavoriteTeams(
                        member.getId(),
                        request
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                );

        /*
         * Arsenal은 정상 Team이지만
         * 요청 중 하나라도 존재하지 않으면
         * 아무 팀도 등록하면 안 된다.
         */
        assertThat(memberFavoriteTeamRepository.count())
                .isZero();
    }


    @Test
    @DisplayName("이미 등록한 관심 팀이 요청에 포함되어 있으면 전체 등록에 실패한다.")
    void registerFavoriteTeams_alreadyRegistered() {

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


        // Arsenal은 이미 관심 팀
        memberFavoriteTeamRepository.save(
                MemberFavoriteTeam.of(
                        member,
                        arsenal
                )
        );


        MemberFavoriteTeamRegisterRequestDto request =
                new MemberFavoriteTeamRegisterRequestDto(
                        List.of(
                                arsenal.getId(),
                                chelsea.getId()
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                memberFavoriteTeamService.registerFavoriteTeams(
                        member.getId(),
                        request
                )
        )
                .isInstanceOf(
                        AlreadyRegisteredFavoriteTeamException.class
                );


        /*
         * 기존 Arsenal만 남아 있어야 한다.
         *
         * Chelsea가 새로 등록되면
         * "전체 실패"라는 비즈니스 규칙이 깨진 것이다.
         */
        List<MemberFavoriteTeam> favoriteTeams =
                memberFavoriteTeamRepository.findAll();

        assertThat(favoriteTeams)
                .hasSize(1);

        assertThat(favoriteTeams.get(0).getTeam().getId())
                .isEqualTo(
                        arsenal.getId()
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