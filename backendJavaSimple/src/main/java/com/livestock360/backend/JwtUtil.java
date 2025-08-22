package com.livestock360.backend;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Utility class for token generation and validation
 * Implements proper JWT with secret key, expiration, and claims
 */
public class JwtUtil {
    
    // Strong secret key for JWT signing (in production, this should be environment variable)
    private static final String SECRET = "livestock360-super-secret-key-that-is-256-bits-long-and-very-secure-for-jwt-tokens-in-production";
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
    
    // Token expiration time (24 hours)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    
    /**
     * Generate JWT token for a farmer
     * @param farmerId The farmer's ID
     * @param email The farmer's email
     * @param name The farmer's name
     * @return JWT token string
     */
    /**
     * Generate JWT token for farmers (backwards compatibility)
     * @param farmerId User ID
     * @param email User email
     * @param name User name
     * @return JWT token string
     */
    public static String generateToken(String farmerId, String email, String name) {
        return generateToken(farmerId, email, name, "farmer");
    }
    
    /**
     * Generate JWT token with user type
     * @param userId User ID  
     * @param email User email
     * @param name User name
     * @param userType User type ("farmer" or "vet")
     * @return JWT token string
     */
    public static String generateToken(String userId, String email, String name, String userType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .setSubject(userId)  // Subject is the user ID
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
     * Validate JWT token and extract claims
     * @param token JWT token string
     * @return Claims object if valid, null if invalid
     */
    public static Claims validateToken(String token) {
        try {
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
     * Extract farmer ID from JWT token
     * @param token JWT token string
     * @return Farmer ID if valid token, null if invalid
     */
    public static String getFarmerIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.getSubject();
        }
        return null;
    }
    
    /**
     * Extract email from JWT token
     * @param token JWT token string
     * @return Email if valid token, null if invalid
     */
    public static String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return (String) claims.get("email");
        }
        return null;
    }
    
    /**
     * Extract name from JWT token
     * @param token JWT token string
     * @return Name if valid token, null if invalid
     */
    public static String getNameFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return (String) claims.get("name");
        }
        return null;
    }
    
    /**
     * Extract user type from JWT token
     * @param token JWT token string
     * @return User type ("farmer" or "vet") if valid token, null if invalid
     */
    public static String getUserTypeFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return (String) claims.get("type");
        }
        return null;
    }
    
    /**
     * Check if token is expired
     * @param token JWT token string
     * @return true if expired, false if still valid
     */
    public static boolean isTokenExpired(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.getExpiration().before(new Date());
        }
        return true; // Consider invalid tokens as expired
    }
    
    /**
     * Get remaining time until token expires
     * @param token JWT token string
     * @return Remaining time in minutes, -1 if invalid/expired
     */
    public static long getRemainingMinutes(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            Date expiration = claims.getExpiration();
            Date now = new Date();
            if (expiration.after(now)) {
                return (expiration.getTime() - now.getTime()) / (60 * 1000);
            }
        }
        return -1;
    }
    
    /**
     * Refresh token if it's close to expiry (within 2 hours)
     * @param token Current JWT token
     * @return New token if refreshed, original token if not needed, null if invalid
     */
    public static String refreshTokenIfNeeded(String token) {
        long remainingMinutes = getRemainingMinutes(token);
        
        // Refresh if less than 2 hours remaining
        if (remainingMinutes > 0 && remainingMinutes < 120) {
            Claims claims = validateToken(token);
            if (claims != null) {
                String userId = claims.getSubject();
                String email = (String) claims.get("email");
                String name = (String) claims.get("name");
                String userType = (String) claims.get("type");
                
                if (userId != null && email != null && name != null && userType != null) {
                    return generateToken(userId, email, name, userType);
                }
            }
        }
        
        return remainingMinutes > 0 ? token : null;
    }
}
