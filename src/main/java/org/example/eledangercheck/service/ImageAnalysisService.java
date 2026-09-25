package org.example.eledangercheck.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.example.eledangercheck.config.DeepSeekConfig;
import org.example.eledangercheck.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class ImageAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisService.class);

    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;
    private final OpenCvDetectionService openCvDetectionService;

    @Autowired
    public ImageAnalysisService(DeepSeekConfig deepSeekConfig, RestTemplate restTemplate, OpenCvDetectionService openCvDetectionService) {
        this.deepSeekConfig = deepSeekConfig;
        this.restTemplate = restTemplate;
        this.openCvDetectionService = openCvDetectionService;
    }

    public Map<String, Object> analyzeSceneImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        logger.info("开始分析图片，文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        Map<String, Object> preprocessResult = preprocessImage(file);
        logger.info("图像预处理完成");

        Map<String, Object> detectionResult;
        try {
            detectionResult = openCvDetectionService.detectAll(file);
            logger.info("目标检测完成");
        } catch (Exception e) {
            logger.warn("目标检测失败", e);
            detectionResult = new HashMap<>();
        }

        Map<String, Object> deepSeekResult;
        try {
            deepSeekResult = analyzeWithDeepSeek(file, preprocessResult);
            logger.info("DeepSeek分析完成");
        } catch (Exception e) {
            logger.warn("DeepSeek分析失败，使用检测结果兜底: {}", e.getMessage());
            deepSeekResult = buildFallbackAnalysis(preprocessResult, detectionResult);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("preprocess", preprocessResult);
        result.put("detection", detectionResult);
        result.put("analysis", deepSeekResult);
        result.put("success", true);

        return result;
    }

    private Map<String, Object> buildFallbackAnalysis(Map<String, Object> preprocess, Map<String, Object> detection) {
        Map<String, Object> result = new HashMap<>();

        int personCount = detection.containsKey("personCount") ? ((Number) detection.get("personCount")).intValue() : 0;
        int helmetCount = detection.containsKey("helmetCount") ? ((Number) detection.get("helmetCount")).intValue() : 0;
        int fireCount = detection.containsKey("fireCount") ? ((Number) detection.get("fireCount")).intValue() : 0;
        int noHelmetCount = detection.containsKey("noHelmetCount") ? ((Number) detection.get("noHelmetCount")).intValue() : 0;
        int noVestCount = detection.containsKey("noVestCount") ? ((Number) detection.get("noVestCount")).intValue() : 0;

        StringBuilder analysisText = new StringBuilder();
        analysisText.append("基于图像检测的初步分析：\n\n");
        analysisText.append("• 检测到人员：").append(personCount).append("人\n");
        analysisText.append("• 检测到安全帽：").append(helmetCount).append("顶\n");
        analysisText.append("• 检测到反光衣：").append(detection.getOrDefault("vestCount", 0)).append("件\n");
        analysisText.append("• 检测到火焰隐患：").append(fireCount).append("处\n\n");

        if (noHelmetCount > 0) {
            analysisText.append("⚠️ 发现").append(noHelmetCount).append("人未佩戴安全帽，存在违章风险！\n");
        }
        if (fireCount > 0) {
            analysisText.append("🔥 检测到火焰区域，请注意消防安全！\n");
        }

        Double brightness = preprocess.containsKey("brightness") ? ((Number) preprocess.get("brightness")).doubleValue() : null;
        Double clarity = preprocess.containsKey("clarity") ? ((Number) preprocess.get("clarity")).doubleValue() : null;

        if (brightness != null) {
            analysisText.append("\n📷 图像质量：亮度").append(String.format("%.1f", brightness)).append("/255");
            if (brightness < 50) {
                analysisText.append("（偏暗）");
            } else if (brightness > 200) {
                analysisText.append("（偏亮）");
            }
            if (clarity != null) {
                analysisText.append("，清晰度").append(String.format("%.0f", clarity)).append("%");
            }
        }

        result.put("analysisText", analysisText.toString());
        result.put("sceneType", "电力作业现场");

        List<Map<String, Object>> violations = new ArrayList<>();

        if (noHelmetCount > 0) {
            Map<String, Object> v = new HashMap<>();
            v.put("name", "未佩戴安全帽");
            v.put("level", "high");
            v.put("description", "检测到" + noHelmetCount + "名作业人员未佩戴安全帽，违反安全操作规程");
            v.put("category", "个人防护");
            violations.add(v);
        }

        if (noVestCount > 0) {
            Map<String, Object> v = new HashMap<>();
            v.put("name", "未穿反光衣");
            v.put("level", "high");
            v.put("description", "检测到" + noVestCount + "名作业人员未穿反光衣，违反安全操作规程");
            v.put("category", "个人防护");
            violations.add(v);
        }

        if (fireCount > 0) {
            Map<String, Object> v = new HashMap<>();
            v.put("name", "火焰隐患");
            v.put("level", "high");
            v.put("description", "检测到明火区域，存在火灾风险");
            v.put("category", "消防安全");
            violations.add(v);
        }

        if (brightness != null && brightness < 60) {
            Map<String, Object> v = new HashMap<>();
            v.put("name", "光线不足");
            v.put("level", "medium");
            v.put("description", "作业现场光线较暗，可能影响作业安全");
            v.put("category", "环境条件");
            violations.add(v);
        }

        result.put("violations", violations);

        return result;
    }

    /**
     * 图像预处理（使用纯Java/BufferedImage实现）
     */
    public Map<String, Object> preprocessImage(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try {
            byte[] imageBytes = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (image == null) {
                throw new BusinessException("无法解码图片文件");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            result.put("width", width);
            result.put("height", height);

            double brightness = calculateBrightness(image);
            result.put("brightness", brightness);

            double clarity = estimateClarity(image);
            result.put("clarity", clarity);

            Map<String, Object> dominantColor = detectDominantColor(image);
            result.put("dominantColor", dominantColor);

            double complexity = estimateSceneComplexity(image);
            result.put("complexity", complexity);

            result.put("success", true);
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("图像预处理失败", e);
            throw new BusinessException("图片预处理失败: " + e.getMessage());
        }
    }

    /**
     * 兼容旧接口名
     */
    public Map<String, Object> preprocessWithOpenCV(MultipartFile file) {
        return preprocessImage(file);
    }

    /**
     * 计算图像平均亮度（0-255）
     */
    private double calculateBrightness(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 采样计算，避免全像素遍历影响性能
        int stepX = Math.max(1, width / 100);
        int stepY = Math.max(1, height / 100);

        long totalBrightness = 0;
        int count = 0;

        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // 使用亮度公式
                double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                totalBrightness += (long) luminance;
                count++;
            }
        }

        double avgBrightness = count > 0 ? (double) totalBrightness / count : 0;
        return Math.round(avgBrightness * 100.0) / 100.0;
    }

    /**
     * 估算图像清晰度（基于相邻像素差异）
     */
    private double estimateClarity(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 采样计算
        int stepX = Math.max(1, width / 80);
        int stepY = Math.max(1, height / 80);

        long totalDiff = 0;
        int count = 0;

        for (int y = 0; y < height - stepY; y += stepY) {
            for (int x = 0; x < width - stepX; x += stepX) {
                int rgb1 = image.getRGB(x, y);
                int rgb2 = image.getRGB(x + stepX, y);

                int gray1 = (int) (0.299 * ((rgb1 >> 16) & 0xFF)
                        + 0.587 * ((rgb1 >> 8) & 0xFF)
                        + 0.114 * (rgb1 & 0xFF));
                int gray2 = (int) (0.299 * ((rgb2 >> 16) & 0xFF)
                        + 0.587 * ((rgb2 >> 8) & 0xFF)
                        + 0.114 * (rgb2 & 0xFF));

                totalDiff += Math.abs(gray1 - gray2);
                count++;
            }
        }

        // 将梯度值归一化到0-100的清晰度分数
        double avgDiff = count > 0 ? (double) totalDiff / count : 0;
        double clarity = Math.min(100.0, avgDiff * 2.5);
        return Math.round(clarity * 100.0) / 100.0;
    }

    /**
     * 检测图像主色调
     */
    private Map<String, Object> detectDominantColor(BufferedImage image) {
        Map<String, Object> colorResult = new HashMap<>();

        int width = image.getWidth();
        int height = image.getHeight();

        // 缩小到32x32采样
        int sampleSize = 32;
        int stepX = Math.max(1, width / sampleSize);
        int stepY = Math.max(1, height / sampleSize);

        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;

        long[] hueBuckets = new long[12];
        int maxBucket = 0;
        long maxCount = 0;

        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                sumR += r;
                sumG += g;
                sumB += b;
                count++;

                float[] hsv = rgbToHsv(r, g, b);
                int bucket = (int) (hsv[0] * 12 / 360) % 12;
                if (hsv[1] > 0.15 && hsv[2] > 0.15) {
                    hueBuckets[bucket]++;
                    if (hueBuckets[bucket] > maxCount) {
                        maxCount = hueBuckets[bucket];
                        maxBucket = bucket;
                    }
                }
            }
        }

        int avgR = (int) (sumR / count);
        int avgG = (int) (sumG / count);
        int avgB = (int) (sumB / count);

        float[] avgHsv = rgbToHsv(avgR, avgG, avgB);

        colorResult.put("h", Math.round(avgHsv[0] * 10.0) / 10.0);
        colorResult.put("s", Math.round(avgHsv[1] * 100 * 10.0) / 10.0);
        colorResult.put("v", Math.round(avgHsv[2] * 10.0) / 10.0);
        colorResult.put("r", avgR);
        colorResult.put("g", avgG);
        colorResult.put("b", avgB);
        colorResult.put("hex", String.format("#%02x%02x%02x", avgR, avgG, avgB));
        colorResult.put("name", getColorName(avgHsv[0], avgHsv[1], avgHsv[2]));

        return colorResult;
    }

    /**
     * RGB转HSV
     */
    private float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0;
        float s = max == 0 ? 0 : delta / max;
        float v = max;

        if (delta > 0) {
            if (max == rf) {
                h = 60 * (((gf - bf) / delta) % 6);
            } else if (max == gf) {
                h = 60 * (((bf - rf) / delta) + 2);
            } else {
                h = 60 * (((rf - gf) / delta) + 4);
            }
            if (h < 0) h += 360;
        }

        return new float[]{h, s, v};
    }

    private String getColorName(float h, float s, float v) {
        if (s < 0.12 && v > 0.8) return "白色";
        if (v < 0.2) return "黑色";
        if (s < 0.15) return "灰色";

        if (h < 10 || h >= 350) return "红色";
        if (h < 35) return "橙色";
        if (h < 60) return "黄色";
        if (h < 160) return "绿色";
        if (h < 200) return "青色";
        if (h < 260) return "蓝色";
        if (h < 300) return "紫色";
        if (h < 350) return "品红";

        return "其他";
    }

    /**
     * 估算场景复杂度（基于颜色变化）
     */
    private double estimateSceneComplexity(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int stepX = Math.max(1, width / 60);
        int stepY = Math.max(1, height / 60);

        long edgeCount = 0;
        int totalPixels = 0;

        // 简单的边缘检测：比较相邻像素的亮度差异
        int threshold = 30;

        for (int y = 0; y < height - stepY; y += stepY) {
            for (int x = 0; x < width - stepX; x += stepX) {
                int rgb1 = image.getRGB(x, y);
                int rgb2 = image.getRGB(x + stepX, y);
                int rgb3 = image.getRGB(x, y + stepY);

                int gray1 = (int) (0.299 * ((rgb1 >> 16) & 0xFF)
                        + 0.587 * ((rgb1 >> 8) & 0xFF)
                        + 0.114 * (rgb1 & 0xFF));
                int gray2 = (int) (0.299 * ((rgb2 >> 16) & 0xFF)
                        + 0.587 * ((rgb2 >> 8) & 0xFF)
                        + 0.114 * (rgb2 & 0xFF));
                int gray3 = (int) (0.299 * ((rgb3 >> 16) & 0xFF)
                        + 0.587 * ((rgb3 >> 8) & 0xFF)
                        + 0.114 * (rgb3 & 0xFF));

                if (Math.abs(gray1 - gray2) > threshold || Math.abs(gray1 - gray3) > threshold) {
                    edgeCount++;
                }
                totalPixels++;
            }
        }

        double edgeRatio = totalPixels > 0 ? (double) edgeCount / totalPixels * 100 : 0;
        return Math.round(edgeRatio * 100.0) / 100.0;
    }

    private Map<String, Object> analyzeWithDeepSeek(MultipartFile file, Map<String, Object> preprocessResult) {
        String apiKey = deepSeekConfig.getApiKey();
        String baseUrl = deepSeekConfig.getBaseUrl();

        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your_deepseek_api_key")) {
            throw new BusinessException("DeepSeek API Key 未配置，请在配置文件中设置 deepseek.api-key");
        }

        try {
            // 自动压缩图片，确保API调用成功
            byte[] compressedImage = org.example.eledangercheck.util.ImageCompressUtil.compress(file.getBytes());
            String base64Image = Base64.getEncoder().encodeToString(compressedImage);
            String imageType = "image/jpeg"; // 压缩后统一为JPEG

            String prompt = buildPrompt(preprocessResult);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "deepseek-vl2");
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2048);

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
            imageUrl.put("url", "data:" + imageType + ";base64," + base64Image);
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
            logger.info("调用DeepSeek API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException("DeepSeek API调用失败，状态码: " + response.getStatusCode());
            }

            JSONObject responseBody = JSON.parseObject(response.getBody());
            JSONArray choices = responseBody.getJSONArray("choices");

            if (choices == null || choices.isEmpty()) {
                throw new BusinessException("DeepSeek API返回结果为空");
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject messageObj = choice.getJSONObject("message");
            String contentStr = messageObj.getString("content");

            JSONObject analysisResult = parseJsonContent(contentStr);

            Map<String, Object> result = new HashMap<>();
            result.put("sceneType", analysisResult.getOrDefault("sceneType", "未知场景"));
            result.put("equipmentList", analysisResult.getOrDefault("equipmentList", new JSONArray()));
            result.put("violations", analysisResult.getOrDefault("violations", new JSONArray()));
            result.put("analysisText", analysisResult.getOrDefault("analysisText", ""));
            result.put("rawResponse", contentStr);

            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("DeepSeek图像分析失败", e);
            throw new BusinessException("图像分析失败: " + e.getMessage());
        }
    }

    private String buildPrompt(Map<String, Object> preprocessResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个电力安全检查专家，请仔细分析这张电力作业现场照片，识别其中的违章隐患。\n\n");
        prompt.append("【图像预处理信息】\n");
        prompt.append("- 图像尺寸: ").append(preprocessResult.get("width")).append("x").append(preprocessResult.get("height")).append("\n");
        prompt.append("- 亮度值: ").append(preprocessResult.get("brightness")).append("\n");
        prompt.append("- 清晰度值: ").append(preprocessResult.get("clarity")).append("\n");
        Object domColor = preprocessResult.get("dominantColor");
        if (domColor instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> colorMap = (Map<String, Object>) domColor;
            prompt.append("- 主色调: ").append(colorMap.get("name")).append("\n");
        }
        prompt.append("- 场景复杂度(边缘占比): ").append(preprocessResult.get("complexity")).append("%\n\n");
        prompt.append("【分析要求】\n");
        prompt.append("请输出严格的JSON格式，包含以下字段：\n");
        prompt.append("1. sceneType: 作业场景类型（如：变电站巡检、线路检修、带电作业、设备安装等）\n");
        prompt.append("2. equipmentList: 识别到的设备列表（数组，每项包含name和type）\n");
        prompt.append("3. violations: 违章列表（数组，每项包含：\n");
        prompt.append("   - name: 违章名称\n");
        prompt.append("   - level: 违章等级（high或medium）\n");
        prompt.append("   - description: 违章描述\n");
        prompt.append("   - bbox: 目标位置（包含x,y,width,height的相对坐标，范围0-1）\n");
        prompt.append("4. analysisText: 综合分析文本，详细说明现场情况和安全建议\n\n");
        prompt.append("请只输出JSON，不要有其他文字说明。");
        return prompt.toString();
    }

    private String getImageType(String filename) {
        if (filename == null) return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private JSONObject parseJsonContent(String content) {
        try {
            String jsonStr = content.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            int jsonStart = jsonStr.indexOf('{');
            int jsonEnd = jsonStr.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonStr = jsonStr.substring(jsonStart, jsonEnd + 1);
            }

            return JSON.parseObject(jsonStr);
        } catch (Exception e) {
            logger.warn("解析DeepSeek返回的JSON失败，返回原始内容", e);
            JSONObject fallback = new JSONObject();
            fallback.put("sceneType", "解析失败");
            fallback.put("equipmentList", new JSONArray());
            fallback.put("violations", new JSONArray());
            fallback.put("analysisText", content);
            return fallback;
        }
    }
}
