package com.consulos.auth;

import com.consulos.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void nonLoginRequest_shouldBypassRateLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/usagers");
        request.setServletPath("/usagers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(valueOperations, never()).increment(any());
    }

    @Test
    void loginWithinLimit_shouldContinueAndSetExpiryOnFirstAttempt() throws Exception {
        MockHttpServletRequest request = loginRequest();
        request.setRemoteAddr("10.0.0.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:login:10.0.0.8")).thenReturn(1L);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(redisTemplate).expire("rate_limit:login:10.0.0.8", Duration.ofMinutes(15));
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void loginFifthAttempt_shouldStillPass() throws Exception {
        MockHttpServletRequest request = loginRequest();
        request.setRemoteAddr("10.0.0.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:login:10.0.0.8")).thenReturn(5L);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(redisTemplate, never()).expire(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void loginSixthAttempt_shouldReturn429() throws Exception {
        MockHttpServletRequest request = loginRequest();
        request.setRemoteAddr("10.0.0.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:login:10.0.0.8")).thenReturn(6L);

        rateLimitFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        ArgumentCaptor<ApiResponse<?>> bodyCaptor = ArgumentCaptor.forClass(ApiResponse.class);
        verify(objectMapper).writeValue(eq(response.getWriter()), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue().errorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
        verify(filterChain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void login_shouldPreferFirstXForwardedForIp() throws Exception {
        MockHttpServletRequest request = loginRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:login:203.0.113.10")).thenReturn(1L);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(valueOperations).increment("rate_limit:login:203.0.113.10");
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServletPath("/auth/login");
        return request;
    }
}
