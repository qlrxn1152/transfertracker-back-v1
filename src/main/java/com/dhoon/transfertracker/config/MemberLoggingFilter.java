package com.dhoon.transfertracker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MemberLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();

            if (authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated()) {
                MDC.put("memberId", authentication.getName());
                request.setAttribute("memberId", authentication.getName());
            }


            filterChain.doFilter(request,response);



        }
        finally {
            MDC.remove("memberId");
        }



    }
}
