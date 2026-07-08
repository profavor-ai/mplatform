package com.classification.domain_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final String secret = "my_super_secret_key_for_jwt_which_must_be_long_enough_for_hs256";
    private final Key key = Keys.hmacShaKeyFor(secret.getBytes());
    private final long expirationTime = 86400000; // 24 hours

    public String generateToken(String username, String role, String uuid, String ipAddress) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("uuid", uuid);
        claims.put("ipAddress", ipAddress);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
    
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
    
    public String extractIpAddress(String token) {
        return (String) extractAllClaims(token).get("ipAddress");
    }
    
    public boolean isTokenValid(String token) {
        try {
            return !extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
