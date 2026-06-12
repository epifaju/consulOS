package com.consulos.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final ResourceLoader resourceLoader;

    private static final long ACCESS_TOKEN_VALIDITY_MINUTES = 15;

    @Value("${consulos.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${consulos.jwt.public-key-path}")
    private String publicKeyPath;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    void init() {
        try {
            this.privateKey = loadPrivateKey(readPem(privateKeyPath));
            this.publicKey = loadPublicKey(readPem(publicKeyPath));
            log.info("Clés JWT RSA chargées depuis {} et {}", privateKeyPath, publicKeyPath);
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de charger les clés JWT RSA", ex);
        }
    }

    public String generateAccessToken(UserDetails user, UUID organisationId) {
        ConsulosUserDetails consulosUser = (ConsulosUserDetails) user;
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ACCESS_TOKEN_VALIDITY_MINUTES * 60);

        return Jwts.builder()
                .subject(consulosUser.getEmail())
                .claim("userId", consulosUser.getUserId().toString())
                .claim("organisationId", organisationId.toString())
                .claim("role", consulosUser.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return parseClaims(token);
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public UUID extractOrganisationId(String token) {
        return UUID.fromString(extractClaims(token).get("organisationId", String.class));
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).get("userId", String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String readPem(String location) throws Exception {
        if (location.startsWith("classpath:")) {
            try (InputStream inputStream = resourceLoader.getResource(location).getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return Files.readString(Path.of(location));
    }

    private PrivateKey loadPrivateKey(String pemContent) throws Exception {
        String pem = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey loadPublicKey(String pemContent) throws Exception {
        String pem = pemContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
