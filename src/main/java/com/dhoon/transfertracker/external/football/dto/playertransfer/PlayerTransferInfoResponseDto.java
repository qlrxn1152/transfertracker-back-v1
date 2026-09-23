package com.dhoon.transfertracker.external.football.dto.playertransfer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public class PlayerTransferInfoResponseDto {

    private String playerName;
    List<PlayerTransferResponseDto> playerTransfers = new ArrayList<>();



}
