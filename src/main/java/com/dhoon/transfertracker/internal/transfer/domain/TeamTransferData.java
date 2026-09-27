package com.dhoon.transfertracker.internal.transfer.domain;

import com.dhoon.transfertracker.internal.team.domain.Team;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

@Getter
public class TeamTransferData {

    private Team inTeam;
    private Team outTeam;
    private String transferType;
    private String transferDate;

    private TeamTransferData(JsonNode transfer) {
        inTeam = getTargetTeam(transfer, "in");
        outTeam = getTargetTeam(transfer, "out");

        transferType = transfer.get("type").asString();
        transferDate = transfer.get("date").asString();
    }

    public static TeamTransferData of(JsonNode transfer) {
        return new TeamTransferData(transfer);
    }

    public Team getTargetTeam(JsonNode transfer, String target) {
        long targetTeamApiId = transfer.get("teams").get(target).get("id").asLong();
        String targetTeamName = transfer.get("teams").get(target).get("name").asString();
        return Team.of(targetTeamName, targetTeamApiId ); //
    }


}
