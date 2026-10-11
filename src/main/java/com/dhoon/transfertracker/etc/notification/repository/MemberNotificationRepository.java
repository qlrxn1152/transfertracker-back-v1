package com.dhoon.transfertracker.etc.notification.repository;

import com.dhoon.transfertracker.etc.notification.domain.MemberNotification;
import com.dhoon.transfertracker.etc.notification.domain.NotificationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberNotificationRepository extends JpaRepository<MemberNotification, Long> {

    List<MemberNotification> findTop50ByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    List<MemberNotification> findAllByMemberIdAndReadAtIsNull(Long memberId);

    Optional<MemberNotification> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    boolean existsByMemberIdAndTargetTypeAndTargetId(
            Long memberId,
            NotificationTargetType targetType,
            Long targetId
    );
}
