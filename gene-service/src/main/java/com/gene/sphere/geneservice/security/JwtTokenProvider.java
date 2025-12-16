package com.gene.sphere.geneservice.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final byte[] secretKey;
    private final long expiryMs;

    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String secret,
                            @Value("${app.security.jwt.expiration:3600000}") long expiryMs) {
        if (expiryMs <= 0) {
            throw new IllegalArgumentException("JWT expiration (expiryMs) must be a positive value.");
        }
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expiryMs = expiryMs;
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiryMs);

        // Add roles to token
        String roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.joining(","));

        System.out.println("Generating token for user: " + username + " with roles: " + roles);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    public String getUserName(String token) {
        return claims(token).getSubject();
    }

    public boolean validate(String token) {
        try {
            claims(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }
}