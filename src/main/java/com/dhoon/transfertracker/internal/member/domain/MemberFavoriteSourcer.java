package com.dhoon.transfertracker.internal.member.domain;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_favorite_sourcer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_favorite_sourcer_member_journalist", columnNames = {"member_id", "journalist"})
        }
)
public class MemberFavoriteSourcer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_favorite_sourcer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "journalist", nullable = false)
    private TransferPostSource journalist;

    public static MemberFavoriteSourcer of(Member member, TransferPostSource journalist) {
        return new MemberFavoriteSourcer(member, journalist);
    }

    private MemberFavoriteSourcer(Member member, TransferPostSource journalist) {
        this.member = member;
        this.journalist = journalist;
    }

}
