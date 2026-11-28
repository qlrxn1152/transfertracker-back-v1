package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamDetailResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamsResponseDto;
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

    // 리그에 속해있는 모든팀들
    public TeamsResponseDto getTeams() {
        return txService.getTeams();
    }




    public TeamsResponseDto getLeagueTeams(LeagueCode leagueCode) {
        validateLeagueCode(leagueCode);
        return txService.getLeagueTeams(leagueCode);
    }



    public TeamDetailResponseDto getTeamInfo(Long teamId, int page) {
        validateTeamId(teamId);
        validatePage(page);

        Pageable pageable = PageRequest.of(page, 20);

        return txService.getTeamInfo(teamId, pageable);
    }





    private void validatePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 값은 0보다 작을 수 없습니다.");
        }
    }


    private void validateTeamId(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new InvalidTeamIdException();
        }
    }

    private void validateLeagueCode(LeagueCode leagueCode) {
        if ( leagueCode == null || !leagueCode.isDomesticLeague()) {
            throw new InvalidLeagueCodeValueException();
        }
    }

}
