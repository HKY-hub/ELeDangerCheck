package org.example.eledangercheck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "simulation")
public class SimulationConfig {

    private boolean enabled = true;

    private boolean ttsEnabled = true;

    private boolean esignEnabled = true;

    private boolean deepseekEnabled = true;

    private boolean fastchatEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTtsEnabled() {
        return ttsEnabled;
    }

    public void setTtsEnabled(boolean ttsEnabled) {
        this.ttsEnabled = ttsEnabled;
    }

    public boolean isEsignEnabled() {
        return esignEnabled;
    }

    public void setEsignEnabled(boolean esignEnabled) {
        this.esignEnabled = esignEnabled;
    }

    public boolean isDeepseekEnabled() {
        return deepseekEnabled;
    }

    public void setDeepseekEnabled(boolean deepseekEnabled) {
        this.deepseekEnabled = deepseekEnabled;
    }

    public boolean isFastchatEnabled() {
        return fastchatEnabled;
    }

    public void setFastchatEnabled(boolean fastchatEnabled) {
        this.fastchatEnabled = fastchatEnabled;
    }
}