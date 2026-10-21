package com.dhoon.transfertracker.internal.transfer.query.repository;

import com.dhoon.transfertracker.internal.player.domain.QPlayer;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.QTeam;
import com.dhoon.transfertracker.internal.transfer.domain.QTransfer;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.dhoon.transfertracker.internal.transfer.domain.QTransfer.*;

@Repository
@RequiredArgsConstructor
public class TransferQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QTransfer transfer = QTransfer.transfer;
    private final QPlayer player = QPlayer.player;

    private final QTeam inTeam = new QTeam("inTeam");
    private final QTeam outTeam = new QTeam("outTeam");

    public Page<Transfer> findTransfers(Long teamId, LeagueCode leagueCode, String keyWord, Pageable pageable) {
        List<Transfer> transfers = queryFactory
                .selectFrom(transfer)
                .join(transfer.player, player).fetchJoin()
                .join(transfer.inTeam, inTeam).fetchJoin()
                .join(transfer.outTeam, outTeam).fetchJoin()
                .where(
                        teamEq(teamId),
                        leagueCodeEq(leagueCode),
                        keywordContains(keyWord)
                )
                .orderBy(
                        transfer.transferDate.desc(),
                        transfer.id.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(transfer.count())
                .from(transfer)
                .join(transfer.player, player)
                .join(transfer.inTeam, inTeam)
                .join(transfer.outTeam, outTeam)
                .where(
                        teamEq(teamId),
                        leagueCodeEq(leagueCode),
                        keywordContains(keyWord)
                )
                .fetchOne();

        return new PageImpl<>(
                transfers,
                pageable,
                total == null ? 0L : total
        );

    }

    private BooleanExpression teamEq(Long teamId) {
        if (teamId == null) {
            return null;
        }

        return inTeam.id.eq(teamId)
                .or(outTeam.id.eq(teamId));
    }

    private BooleanExpression leagueCodeEq(LeagueCode leagueCode) {
        if (leagueCode == null) {
            return null;
        }

        return inTeam.leagueCode.eq(leagueCode)
                .or(outTeam.leagueCode.eq(leagueCode));
    }

    private BooleanExpression keywordContains(String keyWord) {
        if (keyWord == null || keyWord.isBlank()) {
            return null;
        }

        return player.playerName.containsIgnoreCase(keyWord)
                .or(player.playerNameKo.containsIgnoreCase(keyWord));
    }
}
