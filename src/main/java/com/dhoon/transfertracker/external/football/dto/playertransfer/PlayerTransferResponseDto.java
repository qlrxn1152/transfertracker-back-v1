package com.dhoon.transfertracker.external.football.dto.playertransfer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerTransferResponseDto {

    private String type;

    private String date;

    private String inTeamName;

    private String outTeamName;

}
