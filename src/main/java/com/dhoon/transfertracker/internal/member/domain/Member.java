package com.dhoon.transfertracker.internal.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_member", columnNames = {"login_id"})
)
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(name = "kakao_id", unique = true)
    private String kakaoId;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_grade", nullable = false)
    private MemberGrade memberGrade;

    private Member(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
        this.memberGrade = MemberGrade.USER;
    }

    public static Member signUp(String loginId, String password) {
        return new Member(loginId, password);
    }

    public static Member signUpWithKakao(String kakaoId) {
        Member member = new Member();
        member.loginId = "kakao_" + kakaoId;
        member.kakaoId = kakaoId;
        member.memberGrade = MemberGrade.USER;
        return member;
    }

    public boolean isKakaoMember() {
        return kakaoId != null;
    }

}
