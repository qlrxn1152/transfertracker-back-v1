package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.TransferDetailResponseDto;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferSearchPageValueException;
import com.dhoon.transfertracker.internal.transfer.query.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferQueryOrchestrationService {

    private final TransferService txService;

    public PlayerTransfersResponseDto getPlayerTransfers(Long playerId) {
        validatePlayerId(playerId);

        return txService.getPlayerTransfers(playerId);
    }

    public AllTransfersResponseDto getTransfers(int page, String keyWord, LeagueCode leagueCode, Long teamId) {
        validatePageValue(page);
        String normalizedKeyWord = getNormalizedKeyWord(keyWord);

        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.desc("transferDate"), Sort.Order.desc("id")));

        return txService.getTransfers(pageable, normalizedKeyWord, leagueCode, teamId);
    }



    public AllTransfersResponseDto getTeamTransfers(Long teamId, int page, String keyWord) {
        validatePageValue(page);
        validateTeamId(teamId);
        String normalizedKeyWord = getNormalizedKeyWord(keyWord);

        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.desc("transferDate"), Sort.Order.desc("id"))
        );

        return txService.getTeamTransfers(teamId, pageable, normalizedKeyWord);
    }

    public TransferDetailResponseDto getTransfer(Long transferId) {
        if (transferId == null) {
            throw new IllegalArgumentException("이적 ID를 확인해 주세요.");
        }

        return txService.getTransfer(transferId);
    }




    private void validatePlayerId(Long playerId) {
        if (playerId == null) {
            throw new InvalidPlayerIdException();
        }
    }

    private void validatePageValue(int page) {
        if (page < 0) {
            throw new InvalidTransferSearchPageValueException();
        }
    }

    private void validateTeamId(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }
    }


    private String getNormalizedKeyWord(String keyWord) {
        return keyWord == null ? "" : keyWord.trim();
    }



}
