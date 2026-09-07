package com.consulos.portal;

import com.consulos.common.ApiResponse;
import com.consulos.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class PortalAuthFilter extends OncePerRequestFilter {

    private final PortalAccessService portalAccessService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relative = path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;
        if (!relative.startsWith("/portal")) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return ("POST".equalsIgnoreCase(request.getMethod())
                && ("/portal/access".equals(relative) || "/portal/session".equals(relative)));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
                writeUnauthorized(response, "Token de session portail manquant");
                return;
            }
            String token = authorization.substring(7).trim();
            PortalSession session = portalAccessService.resolveSession(token);
            PortalSessionHolder.set(session);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            writeUnauthorized(response, ex.getMessage());
        } finally {
            PortalSessionHolder.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error("PORTAL_UNAUTHORIZED", message));
    }
}
