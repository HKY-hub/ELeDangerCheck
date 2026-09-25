package org.example.eledangercheck.service;

import org.example.eledangercheck.config.AliyunTtsConfig;
import org.example.eledangercheck.config.SimulationConfig;
import org.example.eledangercheck.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class TtsService {

    private final SimulationConfig simulationConfig;
    private final AliyunTtsConfig aliyunTtsConfig;

    @Autowired
    public TtsService(SimulationConfig simulationConfig, AliyunTtsConfig aliyunTtsConfig) {
        this.simulationConfig = simulationConfig;
        this.aliyunTtsConfig = aliyunTtsConfig;
    }

    public Map<String, Object> synthesize(String text) {
        Map<String, Object> result = new HashMap<>();
        
        if (!simulationConfig.isTtsEnabled() || 
            aliyunTtsConfig.getAccessKeyId() == null || 
            aliyunTtsConfig.getAccessKeyId().contains("your_aliyun_access_key_id")) {
            
            result.put("success", true);
            result.put("mode", "simulation");
            result.put("message", "【模拟模式】语音播报已生成");
            result.put("text", text);
            result.put("audioBase64", generateMockAudio(text));
            
            return result;
        }

        try {
            byte[] audioData = callAliyunTts(text);
            result.put("success", true);
            result.put("mode", "real");
            result.put("message", "语音播报生成成功");
            result.put("text", text);
            result.put("audioBase64", Base64.getEncoder().encodeToString(audioData));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "语音播报生成失败：" + e.getMessage());
        }
        
        return result;
    }

    private byte[] callAliyunTts(String text) throws Exception {
        String accessKeyId = aliyunTtsConfig.getAccessKeyId();
        String accessKeySecret = aliyunTtsConfig.getAccessKeySecret();
        String regionId = aliyunTtsConfig.getRegionId();
        String voiceName = aliyunTtsConfig.getVoiceName();

        throw new BusinessException("阿里云TTS SDK未配置，请在application-mysql.yml中配置阿里云密钥");
    }

    private String generateMockAudio(String text) {
        byte[] mockData = new byte[1024];
        for (int i = 0; i < mockData.length; i++) {
            mockData[i] = (byte) (Math.sin(i * 0.1) * 127);
        }
        return Base64.getEncoder().encodeToString(mockData);
    }
}