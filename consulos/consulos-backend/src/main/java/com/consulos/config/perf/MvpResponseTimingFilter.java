package com.consulos.config.perf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mesure le temps de réponse API (en-tête X-Response-Time-Ms + log si &gt; 2 s, cible PRD).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class MvpResponseTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MvpResponseTimingFilter.class);
    private static final long SLOW_REQUEST_MS = 2_000L;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            response.setHeader("X-Response-Time-Ms", String.valueOf(elapsedMs));
            if (elapsedMs >= SLOW_REQUEST_MS && !isActuatorOrSwagger(request)) {
                log.warn("Requête lente {} {} — {} ms", request.getMethod(), request.getRequestURI(), elapsedMs);
            }
        }
    }

    private static boolean isActuatorOrSwagger(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/actuator")
                || uri.contains("/swagger-ui")
                || uri.contains("/api-docs");
    }
}
