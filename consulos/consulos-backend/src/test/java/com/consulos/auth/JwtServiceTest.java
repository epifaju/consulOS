package com.consulos.auth;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private ConsulosUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new DefaultResourceLoader());
        ReflectionTestUtils.setField(jwtService, "privateKeyPath", "classpath:test-secrets/jwt-private.pem");
        ReflectionTestUtils.setField(jwtService, "publicKeyPath", "classpath:test-secrets/jwt-public.pem");
        jwtService.init();

        userDetails = new ConsulosUserDetails(
                UUID.randomUUID(),
                "admin@consulos.local",
                "hash",
                UUID.randomUUID(),
                "ROLE_ADMIN",
                true
        );
    }

    @Test
    void generateAccessToken_shouldContainExpectedClaims() {
        UUID organisationId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userDetails, organisationId);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@consulos.local");
        assertThat(jwtService.extractOrganisationId(token)).isEqualTo(organisationId);
        assertThat(jwtService.extractClaims(token).get("role")).isEqualTo("ROLE_ADMIN");
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() throws Exception {
        var privateKey = (java.security.PrivateKey) ReflectionTestUtils.getField(jwtService, "privateKey");
        String expiredToken = Jwts.builder()
                .subject("admin@consulos.local")
                .claim("userId", userDetails.getUserId().toString())
                .claim("organisationId", userDetails.getOrganisationId().toString())
                .claim("role", "ROLE_ADMIN")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        String token = jwtService.generateAccessToken(userDetails, userDetails.getOrganisationId());
        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@consulos.local");
    }
}
