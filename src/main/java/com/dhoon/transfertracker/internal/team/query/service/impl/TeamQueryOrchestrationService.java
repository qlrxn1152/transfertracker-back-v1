package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamQueryOrchestrationService {

    private final TeamQueryTxService txService;

    public TeamItemResponseDto getTeam(Long teamId) {
        validateTeamId(teamId);

        return txService.getTeam(teamId);
    }

    public TeamsResponseDto getTeams() {
        return txService.getTeams();
    }

    public TeamsResponseDto getLeagueTeams(LeagueCode leagueCode) {
        validateLeagueCode(leagueCode);
        return txService.getLeagueTeams(leagueCode);
    }

    public TeamPageInfosResponseDto getTeamInfo(Long teamId, int page) {
        validateTeamId(teamId);

        if (page < 0) {
            throw new IllegalArgumentException("페이지 값은 0보다 작을 수 없습니다.");
        }

        // 가장 최근에 저장된 게시물을 20개 단위로.
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("post.contentCreatedAt"),
                        Sort.Order.desc("post.id")
                )
        );

        return txService.getTeamInfo(teamId, pageable);
    }


    private void validateTeamId(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }
    }

    private void validateLeagueCode(LeagueCode leagueCode) {
        if (leagueCode == null) {
            throw new InvalidLeagueCodeValueException();
        }
    }

}
