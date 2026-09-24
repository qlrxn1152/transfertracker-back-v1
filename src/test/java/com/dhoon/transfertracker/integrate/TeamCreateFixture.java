package com.dhoon.transfertracker.integrate;

import com.dhoon.transfertracker.internal.domain.Team;
import com.dhoon.transfertracker.internal.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TeamCreateFixture {

    private final TeamRepository teamRepository;

    public List<Team> saveTeams(int count) {
        List<Team> teams = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Team team = teamRepository.save(Team.of(
                    "team" + i,
                    (long) i
            ));

            teams.add(team);
        }

        return teams;
    }


}
