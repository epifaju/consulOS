package com.consulos.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieHelperTest {

    private AuthCookieHelper helper;

    @BeforeEach
    void setUp() {
        helper = new AuthCookieHelper();
        ReflectionTestUtils.setField(helper, "activeProfile", "dev");
    }

    @Test
    void setRefreshCookies_inDev_shouldBeHttpOnlyNotSecure() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        UUID userId = UUID.randomUUID();

        helper.setRefreshCookies(response, userId, "refresh-token-value");

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).contains(AuthCookieHelper.REFRESH_TOKEN_COOKIE + "=refresh-token-value");
        assertThat(cookies.get(0)).contains("HttpOnly");
        assertThat(cookies.get(0)).contains("Path=/api/v1/auth");
        assertThat(cookies.get(0)).contains("SameSite=Strict");
        assertThat(cookies.get(0)).doesNotContain("Secure");
        assertThat(cookies.get(1)).contains(AuthCookieHelper.USER_ID_COOKIE + "=" + userId);
    }

    @Test
    void setRefreshCookies_inProd_shouldSetSecureFlag() {
        ReflectionTestUtils.setField(helper, "activeProfile", "prod");
        MockHttpServletResponse response = new MockHttpServletResponse();

        helper.setRefreshCookies(response, UUID.randomUUID(), "tok");

        assertThat(response.getHeaders("Set-Cookie").getFirst()).contains("Secure");
    }

    @Test
    void clearRefreshCookies_shouldExpireBoth() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        helper.clearRefreshCookies(response);

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).contains("Max-Age=0");
        assertThat(cookies.get(1)).contains("Max-Age=0");
    }

    @Test
    void getRefreshToken_andUserId_shouldReadCookies() {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(AuthCookieHelper.REFRESH_TOKEN_COOKIE, "rt"),
                new Cookie(AuthCookieHelper.USER_ID_COOKIE, userId.toString())
        );

        assertThat(helper.getRefreshToken(request)).contains("rt");
        assertThat(helper.getUserId(request)).contains(userId);
    }

    @Test
    void getRefreshToken_whenNoCookies_shouldReturnEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(helper.getRefreshToken(request)).isEmpty();
        assertThat(helper.getUserId(request)).isEmpty();
    }
}
