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
     * Generate JWT token for farmers (backwards compatibility)
     */
    public String generateToken(String farmerId, String farmerName, String email) {
        return generateToken(farmerId, email, farmerName, "farmer");
    }

    /**
     * Generate JWT token with user type - matches SimpleBackend exactly
     */
    public static String generateToken(String userId, String email, String name, String userType) {
        String SECRET = "mysecretkeythatislongenoughforjwthmacsha256algorithm";
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .claim("name", name)
                .claim("type", userType)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("livestock360-backend")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate JWT token for vets
     */
    public String generateTokenForVet(String vetId, String vetName, String email) {
        return generateToken(vetId, email, vetName, "vet");
    }

    /**
     * Generate JWT token for customers
     */
    public String generateTokenForCustomer(String customerId, String customerName, String email) {
        return generateToken(customerId, email, customerName, "customer");
    }

    /**
     * Extract user ID from JWT token - matches SimpleBackend
     */
    public static String getUserIdFromToken(String token) {
        Claims claims = validateTokenStatic(token);
        if (claims != null) {
            return claims.getSubject();
        }
        return null;
    }

    /**
     * Extract user type from JWT token - matches SimpleBackend
     */
    public static String getUserTypeFromToken(String token) {
        Claims claims = validateTokenStatic(token);
        if (claims != null) {
            return (String) claims.get("type");
        }
        return null;
    }

    /**
     * Validate JWT token and extract claims - static version for SimpleBackend compatibility
     */
    public static Claims validateTokenStatic(String token) {
        try {
            String SECRET = "mysecretkeythatislongenoughforjwthmacsha256algorithm";
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
            
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
            return null;
        }
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
        return claims != null ? (String) claims.get("type") : null;
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
                System.out.println("Token refreshed successfully. UserType preserved: " + claims.get("type"));
                return newToken;
            }
            
            return token; // Return original token if no refresh needed
        } catch (Exception e) {
            System.out.println("Error refreshing token: " + e.getMessage());
            return null;
        }
    }
    
    public static boolean isTokenExpiredStatic(String token) {
        try {
            // Use the same secret as defined in the static methods
            String SECRET = "mysecretkeythatislongenoughforjwthmacsha256algorithm";
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // Consider expired if parsing fails
        }
    }
}
