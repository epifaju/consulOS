package com.consulos.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthCookieHelper {

    public static final String REFRESH_TOKEN_COOKIE = "consulos_refresh";
    public static final String USER_ID_COOKIE = "consulos_uid";

    private static final Duration REFRESH_MAX_AGE = Duration.ofDays(7);

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public void setRefreshCookies(HttpServletResponse response, UUID userId, String refreshToken) {
        response.addHeader("Set-Cookie", buildCookie(REFRESH_TOKEN_COOKIE, refreshToken).toString());
        response.addHeader("Set-Cookie", buildCookie(USER_ID_COOKIE, userId.toString()).toString());
    }

    public void clearRefreshCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildExpiredCookie(REFRESH_TOKEN_COOKIE).toString());
        response.addHeader("Set-Cookie", buildExpiredCookie(USER_ID_COOKIE).toString());
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    public Optional<UUID> getUserId(HttpServletRequest request) {
        return getCookieValue(request, USER_ID_COOKIE).map(UUID::fromString);
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie buildCookie(String name, String value) {
        boolean secure = "prod".equals(activeProfile);
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(REFRESH_MAX_AGE)
                .build();
    }

    private ResponseCookie buildExpiredCookie(String name) {
        boolean secure = "prod".equals(activeProfile);
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
