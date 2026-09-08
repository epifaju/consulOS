package com.consulos.reseau;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consulos.reseau")
public class ReseauProperties {

    /** Désactivé par défaut (Phase 3 Vague A). */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
