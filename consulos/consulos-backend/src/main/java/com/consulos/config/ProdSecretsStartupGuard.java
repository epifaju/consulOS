package com.consulos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Refuse le profil prod si les secrets démo (ou vides) sont encore en place.
 */
@Component
@Profile("prod")
public class ProdSecretsStartupGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdSecretsStartupGuard.class);

    static final Set<String> FORBIDDEN = Set.of(
            "consulos_dev_pass",
            "redis_dev_pass",
            "minioadmin",
            "minioadmin123",
            "admin1234!",
            "password",
            "changeme"
    );

    private final Environment environment;

    public ProdSecretsStartupGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        rejectIfForbiddenOrBlank("spring.datasource.password", environment.getProperty("spring.datasource.password"));
        rejectIfForbiddenOrBlank("spring.data.redis.password", environment.getProperty("spring.data.redis.password"));
        rejectIfForbiddenOrBlank("consulos.minio.access-key", environment.getProperty("consulos.minio.access-key"));
        rejectIfForbiddenOrBlank("consulos.minio.secret-key", environment.getProperty("consulos.minio.secret-key"));
        String jwtPrivate = environment.getProperty("consulos.jwt.private-key-path", "");
        if (jwtPrivate.isBlank()) {
            throw new IllegalStateException("Prod: consulos.jwt.private-key-path (JWT_PRIVATE_KEY_PATH) est obligatoire");
        }
        log.info("Contrôle secrets prod : aucun secret démo détecté");
    }

    void rejectIfForbiddenOrBlank(String property, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Prod: " + property + " est manquant");
        }
        if (FORBIDDEN.contains(value.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Prod: secret de démo interdit pour " + property);
        }
    }
}
