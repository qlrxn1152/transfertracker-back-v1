package com.dhoon.transfertracker.internal.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_member", columnNames = {"login_id"}))
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    private Member(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }

    public static Member signUp(String loginId, String password) {
        return new Member(loginId, password);
    }
}
