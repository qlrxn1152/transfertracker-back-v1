package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.etc.notification.service.MemberNotificationService;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class TransferSyncTxService {

    private final TransferRepository transferRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final ObjectProvider<MemberNotificationService> memberNotificationServiceProvider;



    public PlayerTransfersResponseDto getOrCreatePlayerTransfersResponse(JsonNode playerData, JsonNode transferDatas) {
        Player player = getOrCreatePlayer(playerData);
        List<PlayerTransferItemResponseDto> transfers = getOrCreatePlayerTransfers(player, transferDatas);

        return PlayerTransfersResponseDto.of(player, transfers);
    }
















    // =================================================== //

    private List<PlayerTransferItemResponseDto> getOrCreatePlayerTransfers(Player player, JsonNode transferDatas) {
        List<PlayerTransferItemResponseDto> playerTransfers = new ArrayList<>();
        boolean[] createdTransfer = {false};

        transferDatas.forEach(transferData -> {

            JsonNode teamData = transferData.get("teams");

            String transferType = transferData.get("type").asString();
            LocalDate transferDate = LocalDate.parse(transferData.get("date").asString());

            Team inTeam = getOrCreateTeam(teamData.get("in"));
            Team outTeam = getOrCreateTeam(teamData.get("out"));

            Transfer transfer = transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(inTeam.getId(), outTeam.getId(), player.getId(), transferDate)
                    .orElseGet(() -> {
                        createdTransfer[0] = true;
                        return transferRepository.save(Transfer.of(
                                player, inTeam, outTeam, transferType, transferDate
                        ));
                    });

            playerTransfers.add(PlayerTransferItemResponseDto.of(transfer));
        });

        if (createdTransfer[0]) {
            MemberNotificationService memberNotificationService = memberNotificationServiceProvider.getIfAvailable();
            if (memberNotificationService != null) {
                memberNotificationService.createFavoriteTeamNotificationsForAllMembers();
            }
        }

        return playerTransfers;
    }

    private Player getOrCreatePlayer(JsonNode playerData) {
        long playerApiId = playerData.get("id").asLong();

        return playerRepository.findByApiFootballId(playerApiId)
                .orElseGet(() -> playerRepository.save(
                        Player.of(
                                playerData.get("name").asString(),
                                playerData.get("id").asLong()
                        ))
                );
    }

    private Team getOrCreateTeam(JsonNode teamData) {
        long teamApiId = teamData.get("id").asLong();

        return teamRepository.findByApiFootballId(teamApiId)
                .orElseGet(() -> teamRepository.save(
                        Team.of(
                                teamData.get("name").asString(),
                                teamData.get("id").asLong()
                        )
                ));
    }

    @Transactional(readOnly = true)
    public String getDisplayTeamName(Long teamApiId) {
        Team team = teamRepository.findByApiFootballId(teamApiId)
                .orElseThrow(NotFoundTeamException::new);

        return team.getDisplayName();
    }
}
