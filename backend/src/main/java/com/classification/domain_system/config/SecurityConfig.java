package com.classification.domain_system.config;

import com.classification.domain_system.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/dev/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/files/download/**").permitAll()
                // Inbound Webhook: 외부 시스템이 자체 채널 시크릿 토큰으로 호출하므로 JWT 인증 제외 (채널 자체 인증 로직에서 검증)
                .requestMatchers(HttpMethod.POST, "/api/integration/inbound/**").permitAll()
                // DQ Rules management requires ADMIN role
                .requestMatchers(HttpMethod.POST, "/api/fields/*/dq-rules").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/dq-rules/*").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/dq-rules/*").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Inbound Webhook: 외부 시스템에서 호출하므로 모든 Origin 허용 (인증은 채널 시크릿 토큰으로 처리)
        CorsConfiguration inboundConfig = new CorsConfiguration();
        inboundConfig.addAllowedOriginPattern("*");
        inboundConfig.setAllowedMethods(Arrays.asList("POST", "OPTIONS"));
        inboundConfig.setAllowedHeaders(Arrays.asList("*"));
        inboundConfig.setAllowCredentials(false);
        source.registerCorsConfiguration("/api/integration/inbound/**", inboundConfig);

        // 일반 API: 허용된 프론트엔드 Origin만 허용
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}

