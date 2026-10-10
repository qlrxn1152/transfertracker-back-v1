package com.dhoon.transfertracker.internal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // 회원가입 / 로그인
                        .requestMatchers(
                                "/api/member/signup",
                                "/api/member/login"
                        )
                        .permitAll()

                        // Health Check
                        .requestMatchers(
                                "/actuator/health"
                        )
                        .permitAll()

                        // Actuator
                        .requestMatchers(
                                "/actuator/**"
                        )
                        .hasRole("ADMIN")

                        // 관리자 API
                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMIN")

                        // 외부 데이터 동기화
                        .requestMatchers(
                                "/external/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET,"/api/member/me")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST,"/api/member/favorite-teams")
                        .authenticated()


                        // 일반 조회 API
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/**"
                        )
                        .permitAll()

                        .anyRequest()
                        .denyAll()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(
                                Customizer.withDefaults()
                        )
                )

                .httpBasic(
                        Customizer.withDefaults()
                );

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
