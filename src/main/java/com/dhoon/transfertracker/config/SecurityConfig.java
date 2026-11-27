package com.dhoon.transfertracker.config;

import com.dhoon.transfertracker.internal.member.auth.MemberJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MemberJwtAuthenticationConverter memberJwtAuthenticationConverter
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(new MemberLoggingFilter(), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth



                                    // 회원가입 / 로그인
                                    .requestMatchers(
                                            "/api/members/signup",
                                            "/api/members/login",
                                            "/api/auth/kakao/**"
                                    )
                                    .permitAll()

                                    // Health Check
                                    .requestMatchers(
                                            "/actuator/health",
                                            "/actuator/prometheus"
                                    )
                                    .permitAll()

                                    // Actuator
                                    .requestMatchers(
                                            "/actuator/**"
                                    )
                                    .hasRole("ADMIN")

                                    // 외부 데이터 동기화
                                    .requestMatchers(
                                            "/external/**"
                                    )
                                    .hasRole("ADMIN")

                                    .requestMatchers(
                                            "/api/admin/**"
                                    )
                                    .hasRole("ADMIN")

                                    // 공개 경기 조회 (동기화·관리 API는 /api/admin 아래에서 계속 보호)
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            "/api/fixtures/**"
                                    )
                                    .permitAll()

                                    // 일반 API
                                    .requestMatchers(
                                            "/api/teams/**",
                                            "/api/members/**",
                                            "/api/players/**",
                                            "/api/team-players/**",
                                            "/api/transfers/**",
                                            "/api/posts/**"
                                    )
                                    .permitAll()

                                    .anyRequest()
                                    .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(
                                jwt -> jwt
                                        .jwtAuthenticationConverter(memberJwtAuthenticationConverter)
                        )
                );


        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> originPatterns = new ArrayList<>(Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        // 관리자 화면은 로컬에서만 제공한다. 실제 권한 검증은 JWT의 ADMIN 역할로 수행된다.
        originPatterns.add("http://localhost:*");
        originPatterns.add("http://127.0.0.1:*");
        configuration.setAllowedOriginPatterns(originPatterns.stream().distinct().toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Request-ID"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }


}
