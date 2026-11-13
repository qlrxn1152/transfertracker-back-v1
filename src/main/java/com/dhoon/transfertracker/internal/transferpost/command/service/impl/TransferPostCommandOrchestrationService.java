package com.dhoon.transfertracker.internal.transferpost.command.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostCommandOrchestrationService {

    private final TransferPostCommandTxService txService;

    public String postAssignTeam() {
        return txService.postAssignTeam();
    }

    /**
     * 운영 관리자만 사용할 수 있는 푸시 수신 점검용 게시물입니다.
     * 실제 게시물과 동일하게 팀 연결 이벤트를 발행하므로 FCM 전체 흐름을 검증할 수 있습니다.
     */
    public Long publishNotificationTestPost(Long teamId) {
        return txService.publishNotificationTestPost(teamId);
    }

    public Long publishJournalistNotificationTest(Long postId) {
        return txService.publishJournalistNotificationTest(postId);
    }

    public int removeNotificationTestPosts() {
        return txService.removeNotificationTestPosts();
    }


}
