package com.dhoon.transfertracker.internal.player.sync.service.impl;


import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.sync.dto.PlayerSyncData;
import com.dhoon.transfertracker.internal.player.sync.dto.response.PlayerSaveResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.dhoon.transfertracker.internal.player.sync.service.impl.PlayerSyncOrchestrationService.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PlayerSyncTxService {

    private final PlayerRepository playerRepository;


    /**
     * 외부 API의 선수 식별자를 기준으로 선수를 조회하거나 생성한다.
     *
     * <p>동일한 apiFootballId의 선수가 이미 존재하면
     * 해당 선수가 이미 DB에 존재하는것이므로, 기존 정보를 변경하지 않고 반환한다.
     * 존재하지 않는 경우에만 새로운 선수를 저장하고, 반환한다.</p>
     *
     * @param playerData 외부 API에서 조회한 선수 데이터
     * @return 기존 또는 신규 생성된 선수의 정보
     */
    public PlayerSaveResponseDto getOrCreatePlayerResponse(PlayerSyncData playerSyncData) {
        Player player = getOrCreatePlayer(playerSyncData);

        return PlayerSaveResponseDto.of(player);
    }





    private Player getOrCreatePlayer(PlayerSyncData playerSyncData) {
        return playerRepository.findByApiFootballId(playerSyncData.apiFootballId())
                .orElseGet(() -> playerRepository.save(
                        Player.of(
                                playerSyncData.playerName(),
                                playerSyncData.apiFootballId()
                        ))
                );
    }

}
