package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoritePlayer;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoritePlayerRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoritePlayerDetailResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoritePlayerDetailsResponse;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoritePlayersResponseDto;
import com.dhoon.transfertracker.internal.member.exception.AlreadyExistMemberFavoritePlayerException;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberFavoritePlayerException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoritePlayerRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MemberFavoritePlayerService {

    private final MemberFavoritePlayerRepository memberFavoritePlayerRepository;

    private final MemberRepository memberRepository;
    private final PlayerRepository playerRepository;

    private final TeamPlayerRepository teamPlayerRepository;
    private final TransferRepository transferRepository;

    public MemberFavoritePlayerDetailResponseDto followPlayer(Long memberId, MemberFavoritePlayerRegisterRequestDto requestDto) {
        Long playerId = requestDto.getPlayerId();

        if (memberId == null) {
            throw new IllegalArgumentException("MemberId 의 값이 Null 입니다.");
        }

        if (playerId == null) {
            throw new InvalidPlayerIdException();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);


        Player player = playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        if ( memberFavoritePlayerRepository.existsByMemberIdAndPlayerId(memberId, playerId) ) {
            throw new AlreadyExistMemberFavoritePlayerException();
        }

        MemberFavoritePlayer savedEntity = memberFavoritePlayerRepository.save(MemberFavoritePlayer.of(member, player));

        return MemberFavoritePlayerDetailResponseDto.of(savedEntity);
    }

    public void unfollowPlayer(Long memberId, MemberFavoritePlayerRegisterRequestDto requestDto) {
        Long playerId = requestDto.getPlayerId();

        if (memberId == null) {
            throw new IllegalArgumentException("MemberId 의 값이 Null 입니다.");
        }

        if (playerId == null) {
            throw new InvalidPlayerIdException();
        }

        memberRepository.findById(memberId).orElseThrow(NotFoundMemberException::new);
        playerRepository.findById(playerId).orElseThrow(NotFoundPlayerException::new);

        MemberFavoritePlayer memberFavoritePlayer = memberFavoritePlayerRepository.findByMemberIdAndPlayerId(memberId, playerId)
                .orElseThrow(NotFoundMemberFavoritePlayerException::new);

        memberFavoritePlayerRepository.delete(memberFavoritePlayer);
    }

    // 선수 팔로우 목록
    public MemberFavoritePlayersResponseDto getMemberFollowPlayers(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("MemberId 의 값이 Null 입니다.");
        }
        memberRepository.findById(memberId).orElseThrow(NotFoundMemberException::new);

        List<MemberFavoritePlayerDetailResponseDto> memberFollowPlayers = memberFavoritePlayerRepository.findAllByMemberId(memberId)
                .stream()
                .map(MemberFavoritePlayerDetailResponseDto::of)
                .toList();

        return MemberFavoritePlayersResponseDto.of(memberFollowPlayers);
    }

    // 팔로우하고있는 특정 플레이어가 어떤팀에 속해있고, 이름은뭐고, 해당 선수의 이적은 무엇이있었고 등등 ...
    public MemberFavoritePlayerDetailsResponse getMemberFollowPlayerDetails(Long memberId, Long playerId) {
        if (memberId == null) {
            throw new IllegalArgumentException("MemberId 의 값이 Null 입니다.");
        }

        if (playerId == null) {
            throw new InvalidPlayerIdException();
        }

        memberRepository.findById(memberId).orElseThrow(NotFoundMemberException::new);

        Player player = playerRepository.findById(playerId).orElseThrow(NotFoundPlayerException::new);
        List<PlayerTransferItemResponseDto> playerTransfers = transferRepository.findAllByPlayerId(playerId)
                .stream()
                .map(PlayerTransferItemResponseDto::of)
                .toList();

        PlayerTransfersResponseDto playerTransfersResponseDto = PlayerTransfersResponseDto.of(player, playerTransfers);
        PlayerItemResponseDto playerItemResponseDto = teamPlayerRepository.findByPlayerId(playerId)
                .map(PlayerItemResponseDto::of)
                .orElse(PlayerItemResponseDto.of(player));

        return MemberFavoritePlayerDetailsResponse.of(playerTransfersResponseDto, playerItemResponseDto);
    }


}
