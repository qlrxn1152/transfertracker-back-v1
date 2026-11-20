package com.dhoon.transfertracker.internal.post.notification;

import com.dhoon.transfertracker.external.fcm.FcmNotificationService;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoritePlayer;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteSourcer;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoritePlayerRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteSourcerRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.notification.domain.MemberFcmToken;
import com.dhoon.transfertracker.internal.notification.repository.MemberFcmTokenRepository;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.exception.NotFoundPostException;
import com.dhoon.transfertracker.internal.post.repository.PostPlayerRepository;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostMemberNotificationService {

    private final PostRepository postRepository;
    private final PostTeamRepository postTeamRepository;
    private final PostPlayerRepository postPlayerRepository;

    private final MemberFavoritePlayerRepository memberFavoritePlayerRepository;
    private final MemberFavoriteTeamRepository memberFavoriteTeamRepository;
    private final MemberFavoriteSourcerRepository memberFavoriteSourcerRepository;

    private final MemberFcmTokenRepository memberFcmTokenRepository;

    private final FcmNotificationService fcmNotificationService;


    public void sendNotification(List<Long> postIds) {


        for (Long postId : postIds) {

            // 해당 포스터의 작성자 ( 기자 ) 를 팔로우 하고 있는 멤버들에게 기자의 새로운 소식이 등록되었다는 알림발송.
            Post post = postRepository.findById(postId).orElseThrow(NotFoundPostException::new);
            PostJournalist journalist = post.getJournalist();

            List<MemberFavoriteSourcer> journalistFollowMembers = memberFavoriteSourcerRepository.findAllByJournalist(journalist);

            journalistFollowMembers.forEach(journalistFollowMember -> {

                Long memberId = journalistFollowMember.getMember().getId();

                List<MemberFcmToken> memberTokens = memberFcmTokenRepository.findAllByMemberId(memberId);


                memberTokens.forEach(memberToken -> {
                    try {
                        fcmNotificationService.sendJournalistPostCreatedNotification(journalist, memberToken.getToken(), postId);
                    } catch (FirebaseMessagingException e) {
                        log.warn(
                                "FCM 알림 발송 실패. postId={}, memberId={}",
                                postId,
                                memberId,
                                e
                        );
                    }
                });
            });

            // 해당 포스터의 관련된 팀을 팔로우 하고 있는 멤버들
            List<PostTeam> postAssignTeams = postTeamRepository.findAllByPostId(postId);

            postAssignTeams.forEach(postTeam -> {
                Long teamId = postTeam.getTeam().getId();

                List<MemberFavoriteTeam> teamFollowMembers = memberFavoriteTeamRepository.findAllByTeamId(teamId);

                teamFollowMembers.forEach(teamFollowMember -> {

                    Team team = teamFollowMember.getTeam();

                    Long memberId = teamFollowMember.getMember().getId();
                    List<MemberFcmToken> memberTokens = memberFcmTokenRepository.findAllByMemberId(memberId);

                    memberTokens.forEach(memberToken -> {
                        try {
                            fcmNotificationService.sendTeamPostNotification(memberToken.getToken(), team.getDisplayName(), postId);
                        } catch (FirebaseMessagingException e) {
                            log.warn(
                                    "FCM 알림 발송 실패. postId={}, memberId={}",
                                    postId,
                                    memberId,
                                    e
                            );
                        }
                    });

                });
            });

            // 해당 포스터의 관련된 선수들을 팔로우 하고 있는 멤버들
            List<PostPlayer> postAssignPlayers = postPlayerRepository.findAllByPostId(postId);

            postAssignPlayers.forEach(postPlayer -> {
                Long playerId = postPlayer.getPlayer().getId();

                List<MemberFavoritePlayer> playerFollowMembers = memberFavoritePlayerRepository.findAllByPlayerId(playerId);

                playerFollowMembers.forEach(playerFollowMember -> {

                    Player player = playerFollowMember.getPlayer();

                    Long memberId = playerFollowMember.getMember().getId();
                    List<MemberFcmToken> memberTokens = memberFcmTokenRepository.findAllByMemberId(memberId);

                    memberTokens.forEach(memberToken -> {
                        try {
                            fcmNotificationService.sendPlayerPostNotification(memberToken.getToken(), player.getDisplayName(), postId);
                        } catch (FirebaseMessagingException e) {
                            log.warn(
                                    "FCM 알림 발송 실패. postId={}, memberId={}",
                                    postId,
                                    memberId,
                                    e
                            );
                        }
                    });
                });


            });


        }




    }
}
