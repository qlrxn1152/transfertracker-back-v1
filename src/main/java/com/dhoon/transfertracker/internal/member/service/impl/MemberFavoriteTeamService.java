package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteTeamRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.*;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MemberFavoriteTeamService {

    private final MemberFavoriteTeamRepository memberFavoriteTeamRepository;

    private final TransferPostTeamRepository transferPostTeamRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    public MemberFavoriteTeamCreateResponseDto createFavoriteTeam(Long memberId, MemberFavoriteTeamRegisterRequestDto requestDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        Map<Long, String> teams = new HashMap<>();

        requestDto.getTeamIds()
                .stream()
                .distinct()
                .forEach(teamId -> {
                    Team team = teamRepository.findById(teamId)
                            .orElseThrow(NotFoundTeamException::new);

                    memberFavoriteTeamRepository.findByMemberIdAndTeamId(memberId, teamId)
                            .orElseGet(() -> memberFavoriteTeamRepository.save(MemberFavoriteTeam.of(member, team)));


                    teams.put(team.getId(), team.getDisplayName());
                });


        return MemberFavoriteTeamCreateResponseDto.of(memberId, teams);
    }

    public MemberFavoriteTeamDetailResponseDto getMyFavoriteTeams(Long memberId, int page) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        // 가장 최근에 저장된 게시물을 20개 단위로.
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("contentCreatedAt"),
                        Sort.Order.desc("id")
                )
        );

        List<MemberFavoriteTeamDetailTeamResponseDto> teamDetails = new ArrayList<>();

        List<MemberFavoriteTeamDetailPostResponseDto> postDetails = new ArrayList<>();


        List<MemberFavoriteTeam> memberAllFavoriteTeams = memberFavoriteTeamRepository.findAllByMemberId(memberId);

        for (MemberFavoriteTeam memberAllFavoriteTeam : memberAllFavoriteTeams) {
            Long teamId = memberAllFavoriteTeam.getTeam().getId();
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(NotFoundTeamException::new); // 관심팀에 대한 팀
            teamDetails.add(MemberFavoriteTeamDetailTeamResponseDto.of(team));


            Page<TransferPostTeam> teamPostPage = transferPostTeamRepository.findAllByTeamId(teamId, pageable);

            teamPostPage.getContent()
                    .stream()
                    .map(MemberFavoriteTeamDetailPostResponseDto::of)
                    .forEach(postDetails::add);

        }


        MemberFavoriteTeamDetailMemberResponseDto memberDetail = MemberFavoriteTeamDetailMemberResponseDto.of(member);

        return MemberFavoriteTeamDetailResponseDto.of(memberDetail, teamDetails, postDetails);
    }



}
