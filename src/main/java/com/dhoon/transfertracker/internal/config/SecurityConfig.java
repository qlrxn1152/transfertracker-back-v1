package com.dhoon.transfertracker.internal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/external/**", "/api/admin/**"))
                .authorizeHttpRequests(auth -> auth

                        // 현재 회원 기능 사용하지 않음
                        .requestMatchers(
                                "/api/member/signup",
                                "/api/member/login"
                        ).denyAll()

                        .requestMatchers("/actuator/health")
                        .permitAll()

                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")

                        // 관리자 운영 API는 조회/실행 모두 ADMIN만
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // 외부 API 호출 / 데이터 동기화는 ADMIN만
                        .requestMatchers("/external/**")
                        .hasRole("ADMIN")

                        // 사용자 조회 API
                        .requestMatchers(HttpMethod.GET, "/api/**")
                        .permitAll()

                        // 그 외 요청은 기본적으로 차단
                        .anyRequest()
                        .denyAll()
                )

                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder
    ) {

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
