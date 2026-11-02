package com.dhoon.transfertracker.config;

import com.dhoon.transfertracker.internal.member.auth.MemberJwtAuthenticationConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({
        SecurityConfig.class,
        SecurityConfigTest.TestController.class
})
@EnableWebSecurity
public class SecurityConfigTest {

    private MockMvc mockMvc;

    @MockitoBean
    private MemberJwtAuthenticationConverter converter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }


    @RestController
    static class TestController {

        @PostMapping("/external/test")
        public String externalTest() {
            return "OK";
        }


    }

    @Test
    @DisplayName("인증되지 않은 사용자는 관리자 API 에 접근할 수 없다.")
    void external_unAuthorized() throws Exception {
        // when && then
        mockMvc.perform(post("/external/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER 권한으로 관리자 API 에 접근시, 403 을 반환한다.")
    @WithMockUser(username = "user", roles = {"USER"})
    void external_userForbidden() throws Exception {
        // when && then
        mockMvc.perform(post("/external/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 권한으로 관리자 API 에 접근시, 200 을 반환한다.")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void external_adminSuccess() throws Exception {
        // when && then
        mockMvc.perform(post("/external/test"))
                .andExpect(status().isOk());
    }








}
