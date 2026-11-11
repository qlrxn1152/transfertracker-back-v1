package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteTeamRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamCreateResponseDto;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberFavoriteTeamServiceTest {
    
    @Mock
    private MemberFavoriteTeamRepository memberFavoriteTeamRepository;

    @Mock
    private TeamRepository teamRepository;
    
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberFavoriteTeamService memberFavoriteTeamService;

    @Test
    @DisplayName("존재하는 회원은 여러 팀을 관심 팀으로 등록할 수 있다")
    void create_member_favorite_team() throws Exception {
        // given
        Member member = Member.signUp("testMember", "password123!");
        ReflectionTestUtils.setField(member, "id", 1L);

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));

        Team t1 = Team.of("team-1", 100L);
        ReflectionTestUtils.setField(t1, "id", 30L);

        Team t2 = Team.of("team-2", 200L);
        ReflectionTestUtils.setField(t2, "id", 40L);

        given(teamRepository.findById(30L))
                .willReturn(Optional.of(t1));

        given(teamRepository.findById(40L))
                .willReturn(Optional.of(t2));


        MemberFavoriteTeamRegisterRequestDto requestDto = new MemberFavoriteTeamRegisterRequestDto(List.of(30L, 40L));

        // when
        MemberFavoriteTeamCreateResponseDto response = memberFavoriteTeamService.createFavoriteTeam(1L, requestDto);

        // then

        ArgumentCaptor<MemberFavoriteTeam> captor = ArgumentCaptor.forClass(MemberFavoriteTeam.class);

        verify(memberFavoriteTeamRepository, times(2)).save(captor.capture());

        List<MemberFavoriteTeam> savedEntities = captor.getAllValues();

        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities).extracting(MemberFavoriteTeam::getTeam)
                .containsExactlyInAnyOrder(t1, t2);
        assertThat(savedEntities).extracting(MemberFavoriteTeam::getMember)
                .containsOnly(member);


        verify(memberRepository, times(1)).findById(1L);

        verify(teamRepository, times(1)).findById(30L);
        verify(teamRepository, times(1)).findById(40L);

        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getTeams()).hasSize(2);

        assertThat(response.getTeams()).containsKey(30L);
        assertThat(response.getTeams()).containsKey(40L);
    }

    @Test
    @DisplayName(value = "존재하지 않는 회원은 관심팀 기능을 이용할 수 없다.")
    void create_fail_notFoundMember() throws Exception {
        // given
        given(memberRepository.findById(1L))
                .willReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> memberFavoriteTeamService.createFavoriteTeam(1L, mock(MemberFavoriteTeamRegisterRequestDto.class)))
                .isInstanceOf(NotFoundMemberException.class);

        verify(memberRepository, times(1)).findById(1L);
        verifyNoInteractions(teamRepository);
        verifyNoInteractions(memberFavoriteTeamRepository);
    }

    @Test
    @DisplayName(value = "존재하지 않는 팀에 대해서는 관심팀 기능을 이용할 수 없다.")
    void create_fail_notFoundTeam() throws Exception {
        // given
        Member member = Member.signUp("testMember", "password123!");
        ReflectionTestUtils.setField(member, "id", 1L);

        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));

        given(teamRepository.findById(30L))
                .willReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> memberFavoriteTeamService.createFavoriteTeam(1L, new MemberFavoriteTeamRegisterRequestDto(List.of(30L))))
                .isInstanceOf(NotFoundTeamException.class);

        verify(memberRepository, times(1)).findById(1L);
        verify(teamRepository, times(1)).findById(30L);

        verifyNoInteractions(memberFavoriteTeamRepository);
    }

    @Test
    @DisplayName("이미 등록된 관심 팀은 중복 저장하지 않고, 새로운팀만 저장한다.")
    void create_skipExistingFavoriteTeam() {
        // given
        Member member = Member.signUp("testMember", "password123!");
        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));

        Team team = Team.of("testTeam", 100L);
        ReflectionTestUtils.setField(team, "id", 30L);

        Team team2 = Team.of("testTeam2", 200L);
        ReflectionTestUtils.setField(team2, "id", 40L);

        given(teamRepository.findById(30L))
                .willReturn(Optional.of(team));
        given(teamRepository.findById(40L))
                .willReturn(Optional.of(team2));

        MemberFavoriteTeam favoriteTeam = MemberFavoriteTeam.of(member, team);

        given(memberFavoriteTeamRepository.findByMemberIdAndTeamId(1L, 30L))
                .willReturn(Optional.of(favoriteTeam));

        given(memberFavoriteTeamRepository.findByMemberIdAndTeamId(1L, 40L))
                .willReturn(Optional.empty());

        MemberFavoriteTeamRegisterRequestDto requestDto = new MemberFavoriteTeamRegisterRequestDto(List.of(30L, 40L));

        // when
        MemberFavoriteTeamCreateResponseDto response = memberFavoriteTeamService.createFavoriteTeam(1L, requestDto);

        // then
        verify(memberRepository, times(1)).findById(1L);

        verify(teamRepository, times(1)).findById(30L);
        verify(teamRepository, times(1)).findById(40L);

        ArgumentCaptor<MemberFavoriteTeam> captor = ArgumentCaptor.forClass(MemberFavoriteTeam.class);
        verify(memberFavoriteTeamRepository, times(1)).save(captor.capture());

        MemberFavoriteTeam saved = captor.getValue();

        verify(memberFavoriteTeamRepository, times(1)).findByMemberIdAndTeamId(1L, 30L);
        verify(memberFavoriteTeamRepository, times(1)).findByMemberIdAndTeamId(1L, 40L);

        assertThat(saved.getTeam().getId()).isEqualTo(40L);
        assertThat(saved.getMember().getId()).isEqualTo(1L);

        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getTeams()).hasSize(2);

        assertThat(response.getTeams().get(30L)).isEqualTo("testTeam");
        assertThat(response.getTeams().get(40L)).isEqualTo("testTeam2");
    }




}
