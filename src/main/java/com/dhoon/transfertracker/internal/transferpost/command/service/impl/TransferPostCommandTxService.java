package com.dhoon.transfertracker.internal.transferpost.command.service.impl;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.command.event.TransferPostTeamAssignedEvent;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TransferPostCommandTxService {

    private static final String NOTIFICATION_TEST_POST_ID_PREFIX = "notification-test-";

    private final TransferPostRepository transferPostRepository;
    private final TeamRepository teamRepository;
    private final TransferPostTeamRepository transferPostTeamRepository;

    private final ApplicationEventPublisher eventPublisher;


    public String postAssignTeam() {
        List<TransferPost> teamNotAssignPosts = transferPostRepository.findAll().stream()
                .filter(post -> !transferPostTeamRepository.existsByPostId(post.getId()))
                .toList();

        List<Team> allLeagueTeams = teamRepository.findAllByLeagueCodeIsNotNull();

        teamNotAssignPosts.forEach(post -> {
            String lowerContent = post.getContent().toLowerCase();

            allLeagueTeams.forEach(team -> {
                String lowerTeamName = team.getTeamName().toLowerCase();

                if (lowerContent.contains(lowerTeamName)) {
                    transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post));

                    eventPublisher.publishEvent(
                            new TransferPostTeamAssignedEvent(post.getId(), team.getId())
                    );

                }

            });
        });

        return "OK";
    }

    /**
     * 수신 환경을 점검할 때만 사용하는 명시적인 테스트 게시물을 생성합니다.
     * 이벤트를 트랜잭션 안에서 발행해 일반 게시물-팀 연결과 같은 AFTER_COMMIT 알림 경로를 사용합니다.
     */
    public Long publishNotificationTestPost(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다."));

        String testId = NOTIFICATION_TEST_POST_ID_PREFIX + UUID.randomUUID();
        TransferPost post = transferPostRepository.save(TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "[TEST] " + team.getTeamName() + " 관심 팀 푸시 알림 수신 확인용 게시물입니다.",
                testId,
                Instant.now(),
                false
        ));

        transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post));
        eventPublisher.publishEvent(new TransferPostTeamAssignedEvent(post.getId(), team.getId()));

        log.info("푸시 수신 확인용 테스트 게시물 생성 postId = {}, teamId = {}", post.getId(), team.getId());
        return post.getId();
    }

    /** 테스트 전용 표식이 있는 게시물과 그 팀 연결만 삭제합니다. */
    public int removeNotificationTestPosts() {
        List<TransferPost> testPosts = transferPostRepository
                .findAllByExternalPostIdStartingWith(NOTIFICATION_TEST_POST_ID_PREFIX);

        if (testPosts.isEmpty()) {
            return 0;
        }

        testPosts.forEach(post -> transferPostTeamRepository.deleteAll(
                transferPostTeamRepository.findAllByPostId(post.getId())
        ));
        transferPostTeamRepository.flush();
        transferPostRepository.deleteAll(testPosts);

        log.info("푸시 수신 확인용 테스트 게시물 {}건 삭제", testPosts.size());
        return testPosts.size();
    }


}
