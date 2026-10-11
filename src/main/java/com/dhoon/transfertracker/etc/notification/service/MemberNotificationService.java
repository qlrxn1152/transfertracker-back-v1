package com.dhoon.transfertracker.etc.notification.service;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.etc.notification.domain.MemberNotification;
import com.dhoon.transfertracker.etc.notification.domain.NotificationTargetType;
import com.dhoon.transfertracker.etc.notification.domain.NotificationType;
import com.dhoon.transfertracker.etc.notification.dto.response.MemberNotificationItemResponseDto;
import com.dhoon.transfertracker.etc.notification.dto.response.MemberNotificationsResponseDto;
import com.dhoon.transfertracker.etc.notification.repository.MemberNotificationRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberNotificationService {

    private static final int SOURCE_LOOKUP_LIMIT = 30;
    private static final String OPERATOR_LOGIN_ID = "qlrxn1152";

    private final MemberRepository memberRepository;
    private final MemberFavoriteTeamRepository memberFavoriteTeamRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final TransferPostRepository transferPostRepository;
    private final TransferRepository transferRepository;
    private final MemberPushNotificationService memberPushNotificationService;

    public MemberNotificationsResponseDto getNotifications(Long memberId) {
        int createdCount = createFavoriteTeamNotifications(memberId, false, true);

        List<MemberNotificationItemResponseDto> notifications = memberNotificationRepository
                .findTop50ByMemberIdOrderByCreatedAtDescIdDesc(memberId)
                .stream()
                .map(MemberNotificationItemResponseDto::of)
                .toList();

        return MemberNotificationsResponseDto.of(
                memberNotificationRepository.countByMemberIdAndReadAtIsNull(memberId),
                createdCount,
                notifications
        );
    }

    public MemberNotificationsResponseDto refreshNotifications(Long memberId) {
        return getNotifications(memberId);
    }

    public void markRead(Long memberId, Long notificationId) {
        MemberNotification notification = memberNotificationRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(IllegalArgumentException::new);

        notification.markRead();
    }

    public void markAllRead(Long memberId) {
        memberNotificationRepository.findAllByMemberIdAndReadAtIsNull(memberId)
                .forEach(MemberNotification::markRead);
    }

    public int createFavoriteTeamNotificationsForAllMembers() {
        return memberFavoriteTeamRepository.findDistinctMemberIds()
                .stream()
                .mapToInt(memberId -> createFavoriteTeamNotifications(memberId, true, false))
                .sum();
    }

    public void notifyMemberSignup(Member signedUpMember) {
        memberRepository.findByLoginId(OPERATOR_LOGIN_ID)
                .filter(operator -> !operator.getId().equals(signedUpMember.getId()))
                .ifPresent(operator -> createMemberSignupNotification(operator, signedUpMember));
    }

    private void createMemberSignupNotification(Member operator, Member signedUpMember) {
        if (memberNotificationRepository.existsByMemberIdAndTargetTypeAndTargetId(
                operator.getId(),
                NotificationTargetType.MEMBER,
                signedUpMember.getId()
        )) {
            return;
        }

        MemberNotification notification = memberNotificationRepository.save(MemberNotification.of(
                operator,
                null,
                NotificationType.MEMBER_SIGNUP,
                NotificationTargetType.MEMBER,
                signedUpMember.getId(),
                "새 회원가입",
                signedUpMember.getLoginId() + " 님이 새롭게 가입했습니다.",
                "/notifications"
        ));

        memberPushNotificationService.send(notification);
    }

    private int createFavoriteTeamNotifications(Long memberId, boolean sendPush, boolean includeAllTeamPosts) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        List<MemberFavoriteTeam> favorites = memberFavoriteTeamRepository.findAllWithTeamByMemberId(memberId);
        List<Long> teamIds = favorites.stream()
                .map(favorite -> favorite.getTeam().getId())
                .toList();

        if (teamIds.isEmpty()) {
            return 0;
        }

        Set<Long> favoriteTeamIds = new HashSet<>(teamIds);
        int createdCount = 0;

        List<TransferPost> posts = includeAllTeamPosts
                ? transferPostRepository.findAllNotificationCandidatesByTeamIds(teamIds)
                : transferPostRepository.findRecentTransferRelatedByTeamIds(
                        teamIds,
                        PageRequest.of(0, SOURCE_LOOKUP_LIMIT)
                );

        for (TransferPost post : posts) {
            if (createPostNotification(member, post, sendPush)) {
                createdCount++;
            }
        }

        List<Transfer> transfers = transferRepository.findRecentByTeamIds(
                teamIds,
                PageRequest.of(0, SOURCE_LOOKUP_LIMIT)
        );

        for (Transfer transfer : transfers) {
            if (createTransferNotification(member, favoriteTeamIds, transfer, sendPush)) {
                createdCount++;
            }
        }

        return createdCount;
    }

    private boolean createPostNotification(Member member, TransferPost post, boolean sendPush) {
        if (post.getTeam() == null || memberNotificationRepository.existsByMemberIdAndTargetTypeAndTargetId(
                member.getId(),
                NotificationTargetType.TRANSFER_POST,
                post.getId()
        )) {
            return false;
        }

        Team team = post.getTeam();
        MemberNotification notification = memberNotificationRepository.save(MemberNotification.of(
                member,
                team,
                NotificationType.FAVORITE_TEAM_POST,
                NotificationTargetType.TRANSFER_POST,
                post.getId(),
                team.getDisplayName() + " 관련 새 기자 소식",
                summarize(post.getTranslatedContent() == null ? post.getContent() : post.getTranslatedContent()),
                "/posts/" + post.getId()
        ));

        if (sendPush) {
            memberPushNotificationService.send(notification);
        }

        return true;
    }

    private boolean createTransferNotification(Member member, Set<Long> favoriteTeamIds, Transfer transfer, boolean sendPush) {
        if (memberNotificationRepository.existsByMemberIdAndTargetTypeAndTargetId(
                member.getId(),
                NotificationTargetType.TRANSFER,
                transfer.getId()
        )) {
            return false;
        }

        Team team = favoriteTeamIds.contains(transfer.getInTeam().getId())
                ? transfer.getInTeam()
                : transfer.getOutTeam();
        String playerName = transfer.getPlayer().getDisplayName();

        MemberNotification notification = memberNotificationRepository.save(MemberNotification.of(
                member,
                team,
                NotificationType.FAVORITE_TEAM_TRANSFER,
                NotificationTargetType.TRANSFER,
                transfer.getId(),
                team.getDisplayName() + " 관련 새 이적",
                playerName + " · " + transfer.getOutTeam().getDisplayName() + " → " + transfer.getInTeam().getDisplayName(),
                "/transfers/" + transfer.getId()
        ));

        if (sendPush) {
            memberPushNotificationService.send(notification);
        }

        return true;
    }

    private String summarize(String value) {
        if (value == null || value.isBlank()) {
            return "새로운 이적 소식이 등록되었습니다.";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 220 ? normalized.substring(0, 220) + "..." : normalized;
    }
}
