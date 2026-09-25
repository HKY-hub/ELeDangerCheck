package org.example.eledangercheck.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.example.eledangercheck.config.DeepSeekConfig;
import org.example.eledangercheck.util.ImageCompressUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * DeepSeek AI服务
 * 提供火焰检测确认、图像分析等AI能力
 */
@Service
public class DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);

    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;

    @Autowired
    public DeepSeekService(DeepSeekConfig deepSeekConfig, RestTemplate restTemplate) {
        this.deepSeekConfig = deepSeekConfig;
        this.restTemplate = restTemplate;
    }

    /**
     * 验证图片中是否存在真实火焰
     * 使用DeepSeek-VL2视觉模型进行确认，大幅降低颜色误检
     *
     * @param imageBytes 图片字节数组
     * @return true-有火焰, false-无火焰
     */
    public boolean verifyFire(byte[] imageBytes) {
        String apiKey = deepSeekConfig.getApiKey();
        String baseUrl = deepSeekConfig.getBaseUrl();

        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your_deepseek_api_key")) {
            logger.warn("DeepSeek API Key 未配置，跳过AI火焰确认");
            return false;
        }

        try {
            // 压缩图片
            byte[] compressedImage = ImageCompressUtil.compress(imageBytes);
            String base64Image = Base64.getEncoder().encodeToString(compressedImage);

            String prompt = "请仔细分析这张图片，判断图片中是否存在火焰或明火。"
                    + "请只回答一个单词：有火焰 或 无火焰。"
                    + "注意：红色的衣服、红色的设备、红色的标志都不是火焰。"
                    + "只有真正的明火、火焰、燃烧才算是有火焰。";

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "deepseek-vl2");
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 50);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");

            JSONArray content = new JSONArray();

            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            content.add(textContent);

            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
            imageContent.put("image_url", imageUrl);
            content.add(imageContent);

            message.put("content", content);
            messages.add(message);

            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

            String url = baseUrl + "/chat/completions";
            logger.debug("调用DeepSeek API进行火焰确认: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                logger.warn("DeepSeek API调用失败，状态码: {}", response.getStatusCode());
                return false;
            }

            JSONObject responseBody = JSON.parseObject(response.getBody());
            JSONArray choices = responseBody.getJSONArray("choices");

            if (choices == null || choices.isEmpty()) {
                logger.warn("DeepSeek API返回结果为空");
                return false;
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject messageObj = choice.getJSONObject("message");
            String contentStr = messageObj.getString("content");

            logger.info("DeepSeek火焰确认结果: {}", contentStr);

            // 判断结果
            if (contentStr == null || contentStr.isEmpty()) {
                return false;
            }

            String lowerContent = contentStr.toLowerCase();
            // 包含"有火焰"或"有火"且不包含"没有"等否定词
            boolean hasFire = (lowerContent.contains("有火焰") || lowerContent.contains("有火") || lowerContent.contains("存在火焰"))
                    && !lowerContent.contains("没有") && !lowerContent.contains("无火焰") && !lowerContent.contains("没有火焰");

            return hasFire;

        } catch (Exception e) {
            logger.warn("DeepSeek火焰确认失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检测是否有烟雾
     * @param imageBytes 图片字节数组
     * @return true-有烟雾
     */
    public boolean detectSmoke(byte[] imageBytes) {
        String apiKey = deepSeekConfig.getApiKey();
        String baseUrl = deepSeekConfig.getBaseUrl();

        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your_deepseek_api_key")) {
            return false;
        }

        try {
            byte[] compressedImage = ImageCompressUtil.compress(imageBytes);
            String base64Image = Base64.getEncoder().encodeToString(compressedImage);

            String prompt = "请分析这张图片中是否有烟雾或烟气。"
                    + "请只回答：有烟雾 或 无烟雾。";

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "deepseek-vl2");
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 50);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");

            JSONArray content = new JSONArray();
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            content.add(textContent);

            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
            imageContent.put("image_url", imageUrl);
            content.add(imageContent);

            message.put("content", content);
            messages.add(message);
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                return false;
            }

            JSONObject responseBody = JSON.parseObject(response.getBody());
            JSONArray choices = responseBody.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return false;
            }

            String contentStr = choices.getJSONObject(0).getJSONObject("message").getString("content");
            logger.info("DeepSeek烟雾检测结果: {}", contentStr);

            if (contentStr == null) return false;
            return contentStr.contains("有烟雾") && !contentStr.contains("无烟雾") && !contentStr.contains("没有");

        } catch (Exception e) {
            logger.warn("DeepSeek烟雾检测失败: {}", e.getMessage());
            return false;
        }
    }
}
