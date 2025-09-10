package com.livestock360.springbackend.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate JWT token for farmers
     */
    public String generateToken(String farmerId, String farmerName, String email) {
        return Jwts.builder()
                .setSubject(farmerId)
                .claim("name", farmerName)
                .claim("email", email)
                .claim("userType", "farmer")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate JWT token for vets
     */
    public String generateTokenForVet(String vetId, String vetName, String email) {
        return Jwts.builder()
                .setSubject(vetId)
                .claim("name", vetName)
                .claim("email", email)
                .claim("userType", "vet")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate token from existing claims (for refresh)
     */
    public String generateTokenFromClaims(Claims claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("JWT validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract claims from JWT token
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Failed to extract claims: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extract user type from token
     */
    public String extractUserType(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? (String) claims.get("userType") : null;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }

    /**
     * Refresh token if needed (within 1 hour of expiration)
     */
    public String refreshTokenIfNeeded(String token) {
        try {
            Claims claims = extractClaims(token);
            if (claims == null) {
                System.out.println("Cannot refresh: Invalid token");
                return null;
            }

            Date expiration = claims.getExpiration();
            Date now = new Date();
            
            // Check if token expires within 1 hour (3600000 ms)
            long timeUntilExpiration = expiration.getTime() - now.getTime();
            
            if (timeUntilExpiration > 0 && timeUntilExpiration <= 3600000) {
                System.out.println("Refreshing token - expires in " + (timeUntilExpiration / 1000) + " seconds");
                
                // Preserve all claims including userType
                String newToken = generateTokenFromClaims(claims);
                System.out.println("Token refreshed successfully. UserType preserved: " + claims.get("userType"));
                return newToken;
            }
            
            return token; // Return original token if no refresh needed
        } catch (Exception e) {
            System.out.println("Error refreshing token: " + e.getMessage());
            return null;
        }
    }
}
