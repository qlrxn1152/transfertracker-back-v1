package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoritePlayerDetailsResponse {

    private PlayerTransfersResponseDto playerTransfersResponseDto;
    private PlayerItemResponseDto playerItemResponseDto;

    public static MemberFavoritePlayerDetailsResponse of(
            PlayerTransfersResponseDto playerTransfersResponseDto,
            PlayerItemResponseDto playerItemResponseDto
    ) {
        return new MemberFavoritePlayerDetailsResponse(
                playerTransfersResponseDto,
                playerItemResponseDto
        );
    }

}
