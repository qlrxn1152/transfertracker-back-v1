package com.dhoon.transfertracker.etc.notification.repository;

import com.dhoon.transfertracker.etc.notification.domain.MemberNotification;
import com.dhoon.transfertracker.etc.notification.domain.NotificationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberNotificationRepository extends JpaRepository<MemberNotification, Long> {

    List<MemberNotification> findTop50ByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long memberId);

    List<MemberNotification> findAllByMemberIdAndReadAtIsNullAndDeletedAtIsNull(Long memberId);

    List<MemberNotification> findAllByMemberIdAndReadAtIsNotNullAndDeletedAtIsNull(Long memberId);

    Optional<MemberNotification> findByIdAndMemberIdAndDeletedAtIsNull(Long id, Long memberId);

    long countByMemberIdAndReadAtIsNullAndDeletedAtIsNull(Long memberId);

    boolean existsByMemberIdAndTargetTypeAndTargetId(
            Long memberId,
            NotificationTargetType targetType,
            Long targetId
    );
}
