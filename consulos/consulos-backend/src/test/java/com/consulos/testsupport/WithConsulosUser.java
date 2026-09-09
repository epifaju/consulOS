package com.consulos.testsupport;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithConsulosUserSecurityContextFactory.class)
public @interface WithConsulosUser {
    String role() default "ROLE_ADMIN";
    String email() default "admin@consulos.local";
}
