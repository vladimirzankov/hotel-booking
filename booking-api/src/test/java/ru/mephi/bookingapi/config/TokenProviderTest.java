package ru.mephi.bookingapi.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TokenProviderTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-test-secret";
    private TokenProvider jwtService;
    private SecretKey verificationKey;

    @BeforeEach
    void setUp() {
        jwtService = new TokenProvider(SECRET);
        verificationKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    void issue_createsValidToken_withCorrectSubject() {
        String token = jwtService.issue("testUser", "ROLE_USER", 3600);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = parseToken(token);
        assertEquals("testUser", claims.getSubject());
    }

    @Test
    void issue_createsToken_withCorrectRole() {
        String token = jwtService.issue("admin", "ROLE_ADMIN", 3600);

        Claims claims = parseToken(token);
        assertEquals("ROLE_ADMIN", claims.get("role", String.class));
    }

    @Test
    void issue_createsToken_withUserRole() {
        String token = jwtService.issue("user1", "ROLE_USER", 3600);

        Claims claims = parseToken(token);
        assertEquals("ROLE_USER", claims.get("role", String.class));
    }

    @Test
    void issue_createsToken_withCorrectExpiration() {
        long ttlSeconds = 3600;
        long beforeIssue = System.currentTimeMillis();
        
        String token = jwtService.issue("user", "ROLE_USER", ttlSeconds);
        
        long afterIssue = System.currentTimeMillis();

        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();

        assertNotNull(expiration);
        assertNotNull(issuedAt);
        
        long expectedExpirationMin = beforeIssue + (ttlSeconds * 1000) - 1000;
        long expectedExpirationMax = afterIssue + (ttlSeconds * 1000) + 1000;
        
        assertTrue(expiration.getTime() >= expectedExpirationMin);
        assertTrue(expiration.getTime() <= expectedExpirationMax);
    }

    @Test
    void issue_createsToken_withIssuedAtSet() {
        long beforeIssue = System.currentTimeMillis();
        
        String token = jwtService.issue("user", "ROLE_USER", 3600);
        
        long afterIssue = System.currentTimeMillis();

        Claims claims = parseToken(token);
        Date issuedAt = claims.getIssuedAt();
        
        assertNotNull(issuedAt);
        assertTrue(issuedAt.getTime() >= beforeIssue - 1000);
        assertTrue(issuedAt.getTime() <= afterIssue + 1000);
    }

    @Test
    void issue_withShortTtl_createsValidToken() {
        String token = jwtService.issue("user", "ROLE_USER", 1);

        assertNotNull(token);
        
        Claims claims = parseToken(token);
        assertEquals("user", claims.getSubject());
    }

    @Test
    void issue_withLongTtl_createsValidToken() {
        long oneYearInSeconds = 365L * 24 * 60 * 60;
        String token = jwtService.issue("user", "ROLE_USER", oneYearInSeconds);

        Claims claims = parseToken(token);
        assertEquals("user", claims.getSubject());
        
        Date expiration = claims.getExpiration();
        long expectedMin = System.currentTimeMillis() + (oneYearInSeconds * 1000) - 60000;
        assertTrue(expiration.getTime() >= expectedMin);
    }

    @Test
    void issue_differentSubjects_createDifferentTokens() {
        String token1 = jwtService.issue("user1", "ROLE_USER", 3600);
        String token2 = jwtService.issue("user2", "ROLE_USER", 3600);

        assertNotEquals(token1, token2);
    }

    @Test
    void issue_differentRoles_createDifferentTokens() {
        String token1 = jwtService.issue("user", "ROLE_USER", 3600);
        String token2 = jwtService.issue("user", "ROLE_ADMIN", 3600);

        assertNotEquals(token1, token2);
    }

    @Test
    void issue_tokenHasThreeParts() {
        String token = jwtService.issue("user", "ROLE_USER", 3600);
        
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void issue_withEmptySubject_createsToken() {
        String token = jwtService.issue("", "ROLE_USER", 3600);

        Claims claims = parseToken(token);
        assertNull(claims.getSubject());
    }

    @Test
    void issue_serviceAccountToken_hasAdminRole() {
        String token = jwtService.issue("booking-api", "ROLE_ADMIN", 300);

        Claims claims = parseToken(token);
        assertEquals("booking-api", claims.getSubject());
        assertEquals("ROLE_ADMIN", claims.get("role", String.class));
    }
}
