package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

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


    public PlayerTransfersResponseDto getOrCreatePlayerTransfersResponse(JsonNode playerData, JsonNode transferDatas) {
        Player player = getOrCreatePlayer(playerData);
        List<PlayerTransferItemResponseDto> transfers = getOrCreatePlayerTransfers(player, transferDatas);

        return PlayerTransfersResponseDto.of(player, transfers);
    }
















    // =================================================== //

    private List<PlayerTransferItemResponseDto> getOrCreatePlayerTransfers(Player player, JsonNode transferDatas) {
        List<PlayerTransferItemResponseDto> playerTransfers = new ArrayList<>();

        transferDatas.forEach(transferData -> {
            if (!hasRequiredTransferFields(transferData)) {
                log.warn("팀 식별자가 없는 API-Football 이적 항목을 건너뜁니다. playerApiId={}, transfer={}",
                        player.getApiFootballId(), transferData);
                return;
            }

            JsonNode teamData = transferData.get("teams");

            String transferType = transferData.get("type").asString();
            LocalDate transferDate = LocalDate.parse(transferData.get("date").asString());

            Team inTeam = getOrCreateTeam(teamData.get("in")); // 팀 존재 안하면 생성
            Team outTeam = getOrCreateTeam(teamData.get("out"));

            Transfer transfer = transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(inTeam.getId(), outTeam.getId(), player.getId(), transferDate) // 중복 데이터 없는지 확인
                    .orElseGet(() -> transferRepository.save(Transfer.of(
                            player, inTeam, outTeam, transferType, transferDate // 없으면 저장
                    )));

            playerTransfers.add(PlayerTransferItemResponseDto.of(transfer));
        });

        return playerTransfers;
    }

    private boolean hasRequiredTransferFields(JsonNode transferData) {
        if (transferData == null || !transferData.isObject()) {
            return false;
        }

        JsonNode date = transferData.get("date");
        JsonNode teams = transferData.get("teams");

        return date != null
                && date.isTextual()
                && teams != null
                && hasValidTeamId(teams.get("in"))
                && hasValidTeamId(teams.get("out"))
                && teams.get("in").get("id").asLong() != teams.get("out").get("id").asLong();
    }

    private boolean hasValidTeamId(JsonNode teamData) {
        JsonNode id = teamData == null ? null : teamData.get("id");

        return id != null
                && id.isIntegralNumber()
                && id.canConvertToLong()
                && id.asLong() > 0;
    }

    private Player getOrCreatePlayer(JsonNode playerData) {
        long playerApiId = playerData.get("id").asLong(); // 외부 API ID

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
        String teamName = teamData.get("name").asString();

        return teamRepository.findByApiFootballId(teamApiId)
                // API-Football occasionally changes a historical team's numeric ID
                // while preserving its name. Reuse the existing team instead of
                // failing on the unique team_name constraint.
                .or(() -> teamRepository.findByTeamName(teamName))
                .orElseGet(() -> teamRepository.save(
                        Team.of(
                                teamName,
                                teamApiId
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
