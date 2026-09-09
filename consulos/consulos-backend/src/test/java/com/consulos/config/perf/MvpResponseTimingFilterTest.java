package com.consulos.config.perf;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MvpResponseTimingFilterTest {

    @Test
    void doFilter_shouldSetResponseTimeHeader() throws Exception {
        MvpResponseTimingFilter filter = new MvpResponseTimingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            // no-op
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Response-Time-Ms")).isNotBlank();
    }
}
