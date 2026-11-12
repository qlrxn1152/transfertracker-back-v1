package com.dhoon.transfertracker.internal.notification.service.impl;

import com.dhoon.transfertracker.external.fcm.FcmNotificationService;
import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.notification.domain.MemberFcmToken;
import com.dhoon.transfertracker.internal.notification.repository.MemberFcmTokenRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    public void sendTestNotification(Long memberId) {
        List<MemberFcmToken> memberTokens = memberFcmTokenRepository.findAllByMemberId(memberId);

        memberTokens.forEach(memberFcmToken -> {
            try {
                fcmNotificationService.sendTestNotification(memberFcmToken.getToken());
            } catch (FirebaseMessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sendTransferPostNotification(Long postId, Long teamId) {
        List<MemberFavoriteTeam> teamFavoriteMembers = memberFavoriteTeamRepository.findAllByTeamId(teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        teamFavoriteMembers.forEach(memberFavoriteTeam -> {
            Member member = memberFavoriteTeam.getMember();

            List<MemberFcmToken> memberTokens = memberFcmTokenRepository.findAllByMemberId(member.getId());

            memberTokens.forEach(memberFcmToken -> {
                try {

                    fcmNotificationService.sendTransferPostNotification(memberFcmToken.getToken(), team.getDisplayName());

                } catch (FirebaseMessagingException e) {
                    throw new RuntimeException(e);
                }
            });

        });
    }

}
