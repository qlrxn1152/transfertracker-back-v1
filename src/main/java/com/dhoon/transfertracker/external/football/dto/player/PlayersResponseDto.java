package com.dhoon.transfertracker.external.football.dto.player;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class PlayersResponseDto {

    private int teamId;
    private String teamName;
    private List<PlayerItemResponseDto> players = new ArrayList<>();
}
