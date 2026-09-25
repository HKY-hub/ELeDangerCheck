package org.example.eledangercheck.config;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tiia.v20190529.TiiaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TencentTiiaConfig {

    private static final Logger logger = LoggerFactory.getLogger(TencentTiiaConfig.class);

    @Value("${tencent.tiia.secret-id:}")
    private String secretId;

    @Value("${tencent.tiia.secret-key:}")
    private String secretKey;

    @Value("${tencent.tiia.region:ap-guangzhou}")
    private String region;

    @Value("${tencent.tiia.enabled:false}")
    private boolean enabled;

    @Bean
    public TiiaClient tiiaClient() {
        if (!enabled || secretId.isEmpty() || secretKey.isEmpty()) {
            logger.warn("Tencent TIIA service is disabled or credentials not configured");
            return null;
        }

        try {
            Credential cred = new Credential(secretId, secretKey);
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("tiia.tencentcloudapi.com");
            httpProfile.setReqMethod("POST");
            httpProfile.setConnTimeout(10);
            httpProfile.setWriteTimeout(10);
            httpProfile.setReadTimeout(30);

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            TiiaClient client = new TiiaClient(cred, region, clientProfile);
            logger.info("Tencent TIIA client initialized successfully, region: {}", region);
            return client;
        } catch (Exception e) {
            logger.error("Failed to initialize Tencent TIIA client", e);
            return null;
        }
    }

    public boolean isEnabled() {
        return enabled && secretId != null && !secretId.isEmpty();
    }
}
