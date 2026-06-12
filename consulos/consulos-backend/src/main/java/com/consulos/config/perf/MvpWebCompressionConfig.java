package com.consulos.config.perf;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class MvpWebCompressionConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> mvpResponseCompressionCustomizer() {
        return factory -> {
            Compression compression = new Compression();
            compression.setEnabled(true);
            compression.setMinResponseSize(DataSize.ofKilobytes(1));
            factory.setCompression(compression);
        };
    }
}
