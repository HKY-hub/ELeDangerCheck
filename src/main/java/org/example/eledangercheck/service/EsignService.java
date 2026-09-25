package org.example.eledangercheck.service;

import org.example.eledangercheck.config.EsignConfig;
import org.example.eledangercheck.config.SimulationConfig;
import org.example.eledangercheck.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EsignService {

    private final SimulationConfig simulationConfig;
    private final EsignConfig esignConfig;

    @Autowired
    public EsignService(SimulationConfig simulationConfig, EsignConfig esignConfig) {
        this.simulationConfig = simulationConfig;
        this.esignConfig = esignConfig;
    }

    public Map<String, Object> createSignature(String disclosureId, String userId, String signImageBase64) {
        Map<String, Object> result = new HashMap<>();
        
        if (!simulationConfig.isEsignEnabled() || 
            esignConfig.getAppId() == null || 
            esignConfig.getAppId().contains("your_esign_app_id")) {
            
            result.put("success", true);
            result.put("mode", "simulation");
            result.put("message", "【模拟模式】电子签名已完成");
            result.put("disclosureId", disclosureId);
            result.put("userId", userId);
            result.put("signId", "SIM" + System.currentTimeMillis());
            
            return result;
        }

        try {
            String signId = callEsignApi(disclosureId, userId, signImageBase64);
            result.put("success", true);
            result.put("mode", "real");
            result.put("message", "电子签名成功");
            result.put("disclosureId", disclosureId);
            result.put("userId", userId);
            result.put("signId", signId);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "电子签名失败：" + e.getMessage());
        }
        
        return result;
    }

    private String callEsignApi(String disclosureId, String userId, String signImageBase64) throws Exception {
        String appId = esignConfig.getAppId();
        String appSecret = esignConfig.getAppSecret();
        String apiUrl = esignConfig.getApiUrl();

        throw new BusinessException("e签宝SDK未配置，请在application-mysql.yml中配置e签宝参数");
    }
}