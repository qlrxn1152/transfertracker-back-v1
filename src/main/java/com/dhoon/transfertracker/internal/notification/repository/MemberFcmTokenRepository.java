package com.dhoon.transfertracker.internal.notification.repository;

import com.dhoon.transfertracker.internal.notification.domain.MemberFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberFcmTokenRepository extends JpaRepository<MemberFcmToken, Long> {

    Optional<MemberFcmToken> findByMemberIdAndToken(Long memberId, String token);

    Optional<MemberFcmToken> findByToken(String token);

    List<MemberFcmToken> findAllByMemberId(Long memberId);


}
