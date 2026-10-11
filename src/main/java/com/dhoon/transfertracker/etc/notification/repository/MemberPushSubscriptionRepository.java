package com.dhoon.transfertracker.etc.notification.repository;

import com.dhoon.transfertracker.etc.notification.domain.MemberPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberPushSubscriptionRepository extends JpaRepository<MemberPushSubscription, Long> {

    Optional<MemberPushSubscription> findByEndpoint(String endpoint);

    List<MemberPushSubscription> findAllByMemberId(Long memberId);

    void deleteByMemberIdAndEndpoint(Long memberId, String endpoint);
}
