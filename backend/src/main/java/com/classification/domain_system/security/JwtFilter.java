package com.classification.domain_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Inbound Webhook 경로는 채널 자체 시크릿 토큰으로 인증하므로 JWT 필터 완전 제외
        return path.startsWith("/api/integration/inbound/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // Invalid token
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.isTokenValid(jwt)) {
                String tokenIp = jwtUtil.extractIpAddress(jwt);
                String currentIp = request.getRemoteAddr();
                
                // Normalize localhost IPs
                if ("0:0:0:0:0:0:0:1".equals(currentIp) || "::1".equals(currentIp)) currentIp = "127.0.0.1";
                if ("0:0:0:0:0:0:0:1".equals(tokenIp) || "::1".equals(tokenIp)) tokenIp = "127.0.0.1";
                
                if (tokenIp != null && tokenIp.equals(currentIp)) {
                    String roleStr = jwtUtil.extractAllClaims(jwt).get("role", String.class);
                    List<GrantedAuthority> authorities = new java.util.ArrayList<>();
                    if (roleStr != null) {
                        for (String r : roleStr.split(",")) {
                            String trimmed = r.trim();
                            if (!trimmed.isEmpty()) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + trimmed));
                                authorities.add(new SimpleGrantedAuthority(trimmed));
                            }
                        }
                    }
                    if (authorities.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "IP Address mismatch");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
