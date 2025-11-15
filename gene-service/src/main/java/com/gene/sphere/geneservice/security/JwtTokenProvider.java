package com.gene.sphere.geneservice.security;


import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;


/**
 * Creates and validates JWT tokens.
 * Keeps only the secret and expiration settings.
 */
@Component
public class JwtTokenProvider {

    /**
     * Secret key bytes used to sign and verify tokens.
     */
    private final byte[] secretKey;

    /**
     * How long (in milliseconds) a token is valid.
     */
    private final long expiryMs;

    /**
     * Build the provider with secret and expiration values from properties.
     *
     * @param secret   the raw secret string
     * @param expiryMs token lifetime in milliseconds
     */
    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String secret,
                            @Value("${app.security.jwt.expiration:3600000}") long expiryMs) {
        // JWT library needs a byte array to create cryptographic signature, but @Value gives a String
        // Cryptographic algorithms work at a binary level
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expiryMs = expiryMs;
    }

    /**
     * Generate a JWT for the authenticated user.
     *
     * @param authentication current authenticated principal
     * @return signed JWT string
     */
    public String generateToken(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);
        return Jwts.builder()
                .setSubject(principal.getUsername()) // Set who this token belongs to
                .setIssuedAt(now) // Set the date when it was created
                .setExpiration(expiry) // Set the expiration date
                .signWith(SignatureAlgorithm.HS256, secretKey) // Secure it
                .compact(); // Convert it to a string
    }

    /**
     * Get the username (subject) stored in the token.
     *
     * @param token JWT string
     * @return username inside the token
     */
    public String getUserName(String token) {
        return claims(token).getSubject();
    }

    /**
     * Check if the token is well-formed, signed correctly, and not expired.
     *
     * @param token JWT string
     * @return true if valid; false otherwise
     */
    public boolean validate(String token) {
        try {
            claims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            return false;
        } catch (JwtException ex) { 
            return false;
        }
    }

    /**
     * Parse the token and return its claims (payload).
     * Will throw if signature is bad or token expired.
     *
     * @param token JWT string
     * @return claims payload
     */
    private Claims claims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }
}