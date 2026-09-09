package com.consulos.testsupport;

import com.consulos.auth.JwtAuthenticationFilter;
import com.consulos.auth.RateLimitFilter;
import com.consulos.common.GlobalExceptionHandler;
import com.consulos.config.perf.MvpResponseTimingFilter;
import com.consulos.portal.PortalAuthFilter;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Slice WebMvc ciblé : controllers + method security, sans filtres servlet (@Component)
 * qui tireraient Redis / JWT / portail hors scope.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcMethodSecurityTestConfig.class, GlobalExceptionHandler.class})
public @interface ConsulosWebMvcTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] controllers();

    @AliasFor(annotation = WebMvcTest.class, attribute = "excludeFilters")
    ComponentScan.Filter[] excludeFilters() default {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {
                            JwtAuthenticationFilter.class,
                            RateLimitFilter.class,
                            PortalAuthFilter.class,
                            MvpResponseTimingFilter.class
                    }
            )
    };
}
