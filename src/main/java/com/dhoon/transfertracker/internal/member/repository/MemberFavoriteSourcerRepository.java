package com.dhoon.transfertracker.internal.member.repository;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteSourcer;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberFavoriteSourcerRepository extends JpaRepository<MemberFavoriteSourcer, Long> {

    Optional<MemberFavoriteSourcer> findByMemberIdAndJournalist(Long memberId, PostJournalist journalist);

    List<MemberFavoriteSourcer> findAllByMemberId(Long memberId);

    List<MemberFavoriteSourcer> findAllByMemberIdAndJournalist(Long memberId, PostJournalist journalist);

    List<MemberFavoriteSourcer> findAllByJournalist(PostJournalist journalist);
}
