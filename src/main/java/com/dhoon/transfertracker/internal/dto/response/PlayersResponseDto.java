package com.dhoon.transfertracker.internal.dto.response;

import com.dhoon.transfertracker.internal.domain.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayersResponseDto {

    private List<PlayerItemResponseDto> players = new ArrayList<>();

    public static PlayersResponseDto of(List<PlayerItemResponseDto> players) {
        return new PlayersResponseDto(players);
    }
}
