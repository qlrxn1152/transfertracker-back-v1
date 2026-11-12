package com.dhoon.transfertracker.internal.notification.repository;

import com.dhoon.transfertracker.internal.notification.domain.MemberFcmToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberFcmTokenRepository extends JpaRepository<MemberFcmToken, Long> {

    Optional<MemberFcmToken> findByMemberIdAndToken(Long memberId, String token);

    Optional<MemberFcmToken> findByToken(String token);

    List<MemberFcmToken> findAllByMemberId(Long memberId);

    @Modifying
    @Query(value = """
            INSERT INTO member_fcm_token (member_id, platform, token, created_at)
            VALUES (:memberId, :platform, :token, CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                member_id = :memberId,
                platform = :platform
            """, nativeQuery = true)
    void upsertByToken(
            @Param("memberId") Long memberId,
            @Param("platform") String platform,
            @Param("token") String token
    );

}
