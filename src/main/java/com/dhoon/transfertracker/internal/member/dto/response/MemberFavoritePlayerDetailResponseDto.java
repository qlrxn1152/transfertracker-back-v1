package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoritePlayer;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteSourcerRegisterRequestDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoritePlayerDetailResponseDto {

    private Long memberId;
    private Long playerId;
    private String playerDisplayName;
    private String playerPhotoUrl;

    public static  MemberFavoritePlayerDetailResponseDto of(MemberFavoritePlayer memberFavoritePlayer) {
        return new MemberFavoritePlayerDetailResponseDto(
                memberFavoritePlayer.getMember().getId(),
                memberFavoritePlayer.getPlayer().getId(),
                memberFavoritePlayer.getPlayer().getDisplayName(),
                "https://media.api-sports.io/football/players/" + memberFavoritePlayer.getPlayer().getApiFootballId() + ".png"
        );

    }
}
