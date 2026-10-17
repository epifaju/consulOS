package com.consulos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdSecretsStartupGuardTest {

    private MockEnvironment env;
    private ProdSecretsStartupGuard guard;

    @BeforeEach
    void setUp() {
        env = new MockEnvironment();
        guard = new ProdSecretsStartupGuard(env);
    }

    @Test
    void run_shouldPass_whenSecretsAreUniqueAndJwtPathSet() {
        withValidSecrets(env);

        assertThatCode(() -> guard.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_shouldRejectDemoMinioPassword() {
        withValidSecrets(env);
        env.setProperty("consulos.minio.secret-key", "minioadmin123");

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("consulos.minio.secret-key");
    }

    @Test
    void run_shouldRejectDemoPostgresPassword() {
        withValidSecrets(env);
        env.setProperty("spring.datasource.password", "consulos_dev_pass");

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password");
    }

    @Test
    void run_shouldRejectMissingJwtPath() {
        withValidSecrets(env);
        env.setProperty("consulos.jwt.private-key-path", "");

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY_PATH");
    }

    @Test
    void rejectIfForbiddenOrBlank_shouldRejectBlank() {
        assertThatThrownBy(() -> guard.rejectIfForbiddenOrBlank("x", "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("manquant");
    }

    private static void withValidSecrets(MockEnvironment environment) {
        environment.setProperty("spring.datasource.password", "unique-db-secret-9f3a");
        environment.setProperty("spring.data.redis.password", "unique-redis-secret-9f3a");
        environment.setProperty("consulos.minio.access-key", "consulos_minio_prod");
        environment.setProperty("consulos.minio.secret-key", "unique-minio-secret-9f3a");
        environment.setProperty("consulos.jwt.private-key-path", "/run/secrets/jwt_private");
    }
}
