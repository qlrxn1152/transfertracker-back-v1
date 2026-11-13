package com.dhoon.transfertracker.internal.notification.service.impl;

import com.dhoon.transfertracker.external.fcm.FcmNotificationService;
import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteSourcer;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteSourcerRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.notification.domain.MemberFcmToken;
import com.dhoon.transfertracker.internal.notification.repository.MemberFcmTokenRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberNotificationService {

    private final MemberFcmTokenRepository memberFcmTokenRepository;
    private final FcmNotificationService fcmNotificationService;

    private final MemberFavoriteTeamRepository memberFavoriteTeamRepository;
    private final TeamRepository teamRepository;
    private final MemberFavoriteSourcerRepository memberFavoriteSourcerRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void sendTransferPostNotification(Long postId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        List<MemberFavoriteTeam> teamFavoriteMembers = memberFavoriteTeamRepository.findAllByTeamId(teamId); // 해당 팀을 관심팀으로 등록한 멤버들 조회.

        teamFavoriteMembers.forEach(memberFavoriteTeam -> {
            Member member = memberFavoriteTeam.getMember(); // 해당 팀을 관심팀으로 등록한 멤버

            List<MemberFcmToken> memberTokens = memberFcmTokenRepository.findAllByMemberId(member.getId()); // 해당 멤버가 알림을 받는것을 허용한 ..

            memberTokens.forEach(memberFcmToken -> { // 1개의 기기
                try {
                    fcmNotificationService.sendTransferPostNotification(memberFcmToken.getToken(), team.getDisplayName()); // 알림발송
                } catch (FirebaseMessagingException e) {
                    throw new RuntimeException(e);
                }
            });

        });
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void sendTransferPostCreatedNotification(Long postId, TransferPostSource journalist) {
        List<MemberFavoriteSourcer> journalistFollowMembers = memberFavoriteSourcerRepository.findAllByJournalist(journalist);

        journalistFollowMembers.forEach(journalistFollowMember -> {
            Member member = journalistFollowMember.getMember();
            TransferPostSource followJournalist = journalistFollowMember.getJournalist();

            List<MemberFcmToken> memberTokens = memberFcmTokenRepository.findAllByMemberId(member.getId());

            memberTokens.forEach(memberToken -> {
                try {
                    fcmNotificationService.sendTransferPostCreatedNotification(followJournalist, memberToken.getToken());
                } catch (FirebaseMessagingException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

}
