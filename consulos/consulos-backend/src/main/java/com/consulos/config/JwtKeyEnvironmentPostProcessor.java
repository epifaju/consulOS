package com.consulos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Définit des chemins JWT par défaut en dev si JWT_PRIVATE_KEY_PATH / JWT_PUBLIC_KEY_PATH
 * ne sont pas renseignés (évite Path.of("") → AccessDeniedException).
 */
public class JwtKeyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PRIVATE_KEY_PROP = "consulos.jwt.private-key-path";
    private static final String PUBLIC_KEY_PROP = "consulos.jwt.public-key-path";
    private static final String CLASSPATH_PRIVATE = "classpath:jwt-dev/jwt-private.pem";
    private static final String CLASSPATH_PUBLIC = "classpath:jwt-dev/jwt-public.pem";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new HashMap<>();

        if (isBlank(environment.getProperty(PRIVATE_KEY_PROP))) {
            properties.put(PRIVATE_KEY_PROP, resolveKeyPath("jwt-private.pem", CLASSPATH_PRIVATE));
        }
        if (isBlank(environment.getProperty(PUBLIC_KEY_PROP))) {
            properties.put(PUBLIC_KEY_PROP, resolveKeyPath("jwt-public.pem", CLASSPATH_PUBLIC));
        }

        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("consulosJwtKeys", properties));
        }
    }

    private static String resolveKeyPath(String fileName, String classpathFallback) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                userDir.resolve("..").resolve("..").resolve("secrets").resolve(fileName),
                userDir.resolve("..").resolve("secrets").resolve(fileName),
                userDir.resolve("secrets").resolve(fileName)
        );

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.isRegularFile(normalized) && Files.isReadable(normalized)) {
                return normalized.toString();
            }
        }

        return classpathFallback;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
