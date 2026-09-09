package com.consulos.portal;

import com.consulos.common.ApiResponse;
import com.consulos.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalAuthFilterTest {

    @Mock
    private PortalAccessService portalAccessService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private PortalAuthFilter portalAuthFilter;

    private PortalSession session;

    @BeforeEach
    void setUp() {
        session = new PortalSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DOS-1"
        );
        PortalSessionHolder.clear();
    }

    @AfterEach
    void tearDown() {
        PortalSessionHolder.clear();
    }

    @Test
    void nonPortalPath_shouldBypassAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/usagers");
        request.setRequestURI("/api/v1/usagers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        portalAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(portalAccessService, never()).resolveSession(any());
    }

    @Test
    void optionsPortal_shouldBypassAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/portal/me");
        request.setRequestURI("/portal/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        portalAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(portalAccessService, never()).resolveSession(any());
    }

    @Test
    void postAccessAndSession_shouldBypassAuth() throws Exception {
        for (String path : new String[]{"/portal/access", "/portal/session"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            request.setRequestURI(path);
            MockHttpServletResponse response = new MockHttpServletResponse();

            portalAuthFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
        verify(portalAccessService, never()).resolveSession(any());
    }

    @Test
    void protectedPortal_withoutBearer_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/portal/me");
        request.setRequestURI("/portal/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        portalAuthFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        ArgumentCaptor<ApiResponse<?>> bodyCaptor = ArgumentCaptor.forClass(ApiResponse.class);
        verify(objectMapper).writeValue(eq(response.getOutputStream()), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue().errorCode()).isEqualTo("PORTAL_UNAUTHORIZED");
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(PortalSessionHolder.get()).isNull();
    }

    @Test
    void protectedPortal_withValidSession_shouldContinueAndClearHolder() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/portal/me");
        request.setRequestURI("/portal/me");
        request.addHeader("Authorization", "Bearer session-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(portalAccessService.resolveSession("session-token")).thenReturn(session);

        portalAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(PortalSessionHolder.get()).isNull();
    }

    @Test
    void protectedPortal_withInvalidSession_shouldReturn401AndClearHolder() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/portal/me");
        request.setRequestURI("/portal/me");
        request.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(portalAccessService.resolveSession("bad"))
                .thenThrow(new BusinessException(
                        "PORTAL_SESSION_INVALID", "Session portail invalide ou expirée", HttpStatus.UNAUTHORIZED));

        portalAuthFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(PortalSessionHolder.get()).isNull();
    }
}
