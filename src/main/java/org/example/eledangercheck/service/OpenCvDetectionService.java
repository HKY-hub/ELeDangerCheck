package org.example.eledangercheck.service;

import org.example.eledangercheck.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * 综合视觉检测服务
 * 使用本地YOLO PPE模型进行安全帽、反光衣等检测
 * 专为2核2G低配置服务器优化
 */
@Service
public class OpenCvDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(OpenCvDetectionService.class);

    @Autowired(required = false)
    private PpeDetectionService ppeDetectionService;

    @Autowired(required = false)
    private org.example.eledangercheck.util.TencentPpeDetectUtil tencentPpeDetectUtil;

    @Autowired(required = false)
    private DeepSeekService deepSeekService;

    /**
     * 综合检测：调用本地YOLO模型进行PPE检测
     * 返回格式保持与前端兼容
     */
    public Map<String, Object> detectAll(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        try {
            byte[] imageBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            logger.info("收到图片检测请求: 文件名={}, 大小={}字节, MIME类型={}", 
                originalFilename, imageBytes.length, contentType);
            
            BufferedImage image = readImage(imageBytes, originalFilename);
            if (image == null) {
                logger.error("无法解码图片文件: 文件名={}, 大小={}字节, MIME类型={}. 支持的格式: {}", 
                    originalFilename, imageBytes.length, contentType, 
                    java.util.Arrays.toString(ImageIO.getReaderFormatNames()));
                throw new BusinessException("无法解码图片文件，请确保图片格式为JPG、PNG、GIF、BMP等常见格式");
            }

            int origWidth = image.getWidth();
            int origHeight = image.getHeight();

            long startTime = System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("width", origWidth);
            result.put("height", origHeight);

            // ===== PPE检测（本地YOLO模型）=====
            List<Map<String, Object>> persons = new ArrayList<>();
            List<Map<String, Object>> helmets = new ArrayList<>();
            List<Map<String, Object>> vests = new ArrayList<>();
            List<Map<String, Object>> phones = new ArrayList<>();
            List<Map<String, Object>> safetyBelts = new ArrayList<>();
            List<Map<String, Object>> smokes = new ArrayList<>();
            List<Map<String, Object>> workers = new ArrayList<>();

            boolean ppeAvailable = ppeDetectionService != null && ppeDetectionService.isModelLoaded();
            boolean tencentAvailable = tencentPpeDetectUtil != null && tencentPpeDetectUtil.isAvailable();

            if (!ppeAvailable && !tencentAvailable) {
                // 本地模型和腾讯云都不可用，返回明确的错误信息
                String errorMsg = "PPE检测服务不可用：本地YOLO模型未加载，腾讯云API也不可用";
                logger.error("PPE检测服务不可用: {}", errorMsg);
                result.put("ppeSource", "disabled");
                result.put("ppeError", errorMsg);
                result.put("modelLoaded", false);
                result.put("modelError", errorMsg);
                result.put("success", false);
                result.put("message", errorMsg);
            } else if (ppeAvailable) {
                try {
                    org.example.eledangercheck.service.PpeDetectionService.PpeDetectionResult ppeResult =
                        ppeDetectionService.detect(file);

                    if (ppeResult != null && ppeResult.getPersons() != null) {
                        logger.info("本地YOLO PPE检测结果: {}人", ppeResult.getPersons().size());

                        persons = convertPersons(ppeResult);
                        helmets = convertHelmets(ppeResult);
                        vests = convertVests(ppeResult);
                        safetyBelts = convertSafetyBelts(ppeResult);

                        // 构建worker信息
                        for (int i = 0; i < ppeResult.getPersons().size(); i++) {
                            var p = ppeResult.getPersons().get(i);

                            Map<String, Object> worker = new HashMap<>();
                            worker.put("index", i);
                            worker.put("hasHelmet", p.isHasHelmet());
                            worker.put("helmetConfidence", p.getHelmetConfidence());
                            worker.put("hasSafetyBelt", p.isHasSafetyBelt());
                            worker.put("safetyBeltConfidence", p.getSafetyBeltConfidence());
                            worker.put("hasReflectiveVest", p.isHasVest());
                            worker.put("vestConfidence", p.getVestConfidence());
                            worker.put("usingPhone", p.isUsingPhone());
                            worker.put("phoneConfidence", p.getPhoneConfidence());
                            worker.put("smoking", p.isSmoking());
                            worker.put("smokingConfidence", p.getSmokingConfidence());

                            List<String> violationTypes = new ArrayList<>();
                            if (!p.isHasHelmet()) violationTypes.add("未戴安全帽");
                            if (!p.isHasSafetyBelt()) violationTypes.add("未系安全带");
                            if (!p.isHasVest()) violationTypes.add("未穿反光衣");
                            if (p.isUsingPhone()) violationTypes.add("玩手机");
                            if (p.isSmoking()) violationTypes.add("吸烟");
                            worker.put("violationTypes", violationTypes);
                            worker.put("hasViolation", !violationTypes.isEmpty());
                            workers.add(worker);
                        }

                        result.put("ppeSource", "local-yolo");
                        result.put("modelLoaded", true);
                    } else {
                        result.put("ppeSource", "error");
                        result.put("ppeError", "检测结果为空");
                    }
                } catch (Exception e) {
                    logger.error("本地YOLO PPE检测失败: {}", e.getMessage(), e);
                    result.put("ppeSource", "error");
                    result.put("ppeError", e.getMessage());
                }
            } else if (tencentAvailable) {
                // 本地模型不可用，使用腾讯云PPE检测
                try {
                    logger.info("本地YOLO模型不可用，降级使用腾讯云PPE检测");
                    
                    // 将图片转为base64
                    String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                    
                    com.tencentcloudapi.tiia.v20190529.models.DetectSecurityResponse tencentResult =
                        tencentPpeDetectUtil.detectByBase64(base64);
                    
                    if (tencentResult != null && tencentResult.getBodies() != null) {
                        int bodyCount = tencentResult.getBodies().length;
                        logger.info("腾讯云PPE检测成功: 检测到 {} 人", bodyCount);
                        
                        // 解析腾讯云结果并转换为统一格式
                        for (int i = 0; i < bodyCount; i++) {
                            var body = tencentResult.getBodies()[i];
                            
                            Map<String, Object> person = new HashMap<>();
                            Map<String, Object> worker = new HashMap<>();
                            
                            // 坐标转换为相对比例（0-1）
                            var rect = body.getRect();
                            double x = rect != null && rect.getX() != null ? rect.getX() / (double)origWidth : 0;
                            double y = rect != null && rect.getY() != null ? rect.getY() / (double)origHeight : 0;
                            double w = rect != null && rect.getWidth() != null ? rect.getWidth() / (double)origWidth : 0;
                            double h = rect != null && rect.getHeight() != null ? rect.getHeight() / (double)origHeight : 0;
                            
                            person.put("x", x);
                            person.put("y", y);
                            person.put("width", w);
                            person.put("height", h);
                            person.put("confidence", body.getDetectConfidence() != null ? body.getDetectConfidence() : 0.9);
                            
                            boolean hasHelmet = false;
                            boolean hasVest = false;
                            boolean hasSafetyBelt = false;
                            boolean usingPhone = false;
                            boolean smoking = false;
                            double helmetConf = 0, vestConf = 0, beltConf = 0, phoneConf = 0, smokeConf = 0;
                            
                            var attrs = body.getAttributes();
                            if (attrs != null) {
                                for (var attr : attrs) {
                                    String name = attr.getName() != null ? attr.getName() : "";
                                    String label = attr.getLabel() != null ? attr.getLabel() : "";
                                    double conf = attr.getConfidence() != null ? attr.getConfidence() : 0;
                                    
                                    switch (name) {
                                        case "安全帽识别" -> {
                                            hasHelmet = "有安全帽".equals(label);
                                            helmetConf = conf;
                                        }
                                        case "反光衣识别" -> {
                                            hasVest = "有反光衣".equals(label);
                                            vestConf = conf;
                                        }
                                        case "工地安全带识别" -> {
                                            hasSafetyBelt = "有工地安全带".equals(label);
                                            beltConf = conf;
                                        }
                                        case "workerPhone" -> {
                                            usingPhone = "看手机".equals(label) || "玩手机".equals(label);
                                            phoneConf = conf;
                                        }
                                        case "workerSmoke" -> {
                                            smoking = "吸烟".equals(label) || "抽烟".equals(label);
                                            smokeConf = conf;
                                        }
                                    }
                                }
                            }
                            
                            person.put("hasHelmet", hasHelmet);
                            person.put("hasVest", hasVest);
                            person.put("hasSafetyBelt", hasSafetyBelt);
                            person.put("usingPhone", usingPhone);
                            person.put("smoking", smoking);
                            persons.add(person);
                            
                            // 安全帽检测框
                            if (hasHelmet) {
                                Map<String, Object> helmetMap = new HashMap<>();
                                helmetMap.put("x", x + w * 0.15);
                                helmetMap.put("y", y);
                                helmetMap.put("width", w * 0.7);
                                helmetMap.put("height", h * 0.28);
                                helmetMap.put("confidence", helmetConf);
                                helmetMap.put("color", "腾讯云检测");
                                helmetMap.put("hasHelmet", true);
                                helmets.add(helmetMap);
                            }
                            
                            // 反光衣检测框
                            if (hasVest) {
                                Map<String, Object> vestMap = new HashMap<>();
                                vestMap.put("x", x + w * 0.1);
                                vestMap.put("y", y + h * 0.25);
                                vestMap.put("width", w * 0.8);
                                vestMap.put("height", h * 0.6);
                                vestMap.put("confidence", vestConf);
                                vestMap.put("hasVest", true);
                                vests.add(vestMap);
                            }
                            
                            // 安全带检测框
                            if (hasSafetyBelt) {
                                Map<String, Object> beltMap = new HashMap<>();
                                beltMap.put("x", x + w * 0.15);
                                beltMap.put("y", y + h * 0.3);
                                beltMap.put("width", w * 0.7);
                                beltMap.put("height", h * 0.25);
                                beltMap.put("confidence", beltConf);
                                beltMap.put("hasSafetyBelt", true);
                                safetyBelts.add(beltMap);
                            }
                            
                            // 玩手机检测框
                            if (usingPhone) {
                                Map<String, Object> phoneMap = new HashMap<>();
                                phoneMap.put("x", x + w * 0.6);
                                phoneMap.put("y", y + h * 0.1);
                                phoneMap.put("width", w * 0.25);
                                phoneMap.put("height", h * 0.15);
                                phoneMap.put("confidence", phoneConf);
                                phoneMap.put("usingPhone", true);
                                phones.add(phoneMap);
                            }
                            
                            // 吸烟检测框
                            if (smoking) {
                                Map<String, Object> smokeMap = new HashMap<>();
                                smokeMap.put("x", x + w * 0.3);
                                smokeMap.put("y", y + h * 0.08);
                                smokeMap.put("width", w * 0.2);
                                smokeMap.put("height", h * 0.1);
                                smokeMap.put("confidence", smokeConf);
                                smokeMap.put("smoking", true);
                                smokes.add(smokeMap);
                            }
                            
                            // worker信息
                            worker.put("index", i);
                            worker.put("hasHelmet", hasHelmet);
                            worker.put("helmetConfidence", helmetConf);
                            worker.put("hasSafetyBelt", hasSafetyBelt);
                            worker.put("safetyBeltConfidence", beltConf);
                            worker.put("hasReflectiveVest", hasVest);
                            worker.put("vestConfidence", vestConf);
                            worker.put("usingPhone", usingPhone);
                            worker.put("phoneConfidence", phoneConf);
                            worker.put("smoking", smoking);
                            worker.put("smokingConfidence", smokeConf);
                            
                            List<String> violationTypes = new ArrayList<>();
                            if (!hasHelmet) violationTypes.add("未戴安全帽");
                            if (!hasSafetyBelt) violationTypes.add("未系安全带");
                            if (!hasVest) violationTypes.add("未穿反光衣");
                            if (usingPhone) violationTypes.add("玩手机");
                            if (smoking) violationTypes.add("吸烟");
                            worker.put("violationTypes", violationTypes);
                            worker.put("hasViolation", !violationTypes.isEmpty());
                            workers.add(worker);
                        }
                        
                        result.put("ppeSource", "tencent-cloud");
                        result.put("modelLoaded", true);
                        result.put("detectionSource", "tencent-cloud");
                    } else {
                        result.put("ppeSource", "error");
                        result.put("ppeError", "腾讯云PPE检测返回空结果");
                    }
                } catch (Exception e) {
                    logger.error("腾讯云PPE检测失败: {}", e.getMessage(), e);
                    result.put("ppeSource", "error");
                    result.put("ppeError", "腾讯云检测失败: " + e.getMessage());
                }
            }

            result.put("persons", persons);
            result.put("personCount", persons.size());
            result.put("helmets", helmets);
            result.put("helmetCount", helmets.size());
            result.put("vests", vests);
            result.put("vestCount", vests.size());
            result.put("phones", phones);
            result.put("phoneCount", phones.size());
            result.put("safetyBelts", safetyBelts);
            result.put("safetyBeltCount", safetyBelts.size());
            result.put("smokes", smokes);
            result.put("smokeCount", smokes.size());
            result.put("workers", workers);
            result.put("violationsFound", workers.stream().anyMatch(w -> Boolean.TRUE.equals(w.get("hasViolation"))));

            // 火焰检测：先用颜色快速初筛，有疑似火焰再用AI确认（降低误检率）
            List<Map<String, Object>> fires = detectFireWithAIVerify(image, imageBytes, origWidth, origHeight);
            result.put("fires", fires);
            result.put("fireCount", fires.size());

            // 统计未佩戴数量
            long noHelmetCount = persons.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.get("hasHelmet")))
                    .count();
            result.put("noHelmetCount", noHelmetCount);

            long noVestCount = persons.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.get("hasVest")))
                    .count();
            result.put("noVestCount", noVestCount);

            long violationCount = workers.stream()
                    .filter(w -> Boolean.TRUE.equals(w.get("hasViolation")))
                    .count();
            result.put("violationCount", violationCount);

            result.put("totalTimeMs", System.currentTimeMillis() - startTime);
            result.put("success", true);
            result.put("detectionSource", "local-yolo");
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("图像检测失败", e);
            throw new BusinessException("图像检测失败: " + e.getMessage());
        }
    }

    /**
     * 火焰检测：颜色初筛 + AI确认
     * 先用颜色快速检测疑似火焰区域，再用DeepSeek视觉模型确认
     * 既保证速度，又大幅降低误检率
     */
    private List<Map<String, Object>> detectFireWithAIVerify(
            BufferedImage image, byte[] imageBytes, int imgWidth, int imgHeight) {

        // 第一步：颜色快速初筛
        List<Map<String, Object>> suspectedFires = detectFireByColor(image, imgWidth, imgHeight);

        if (suspectedFires.isEmpty()) {
            // 没有疑似火焰，直接返回空
            return new ArrayList<>();
        }

        logger.info("颜色检测发现 {} 处疑似火焰区域，调用AI确认", suspectedFires.size());

        // 第二步：AI确认（如果DeepSeek服务可用）
        if (deepSeekService != null) {
            try {
                boolean hasRealFire = deepSeekService.verifyFire(imageBytes);
                if (hasRealFire) {
                    logger.info("AI确认存在火焰，返回检测结果");
                    return suspectedFires;
                } else {
                    logger.info("AI确认无火焰，过滤掉颜色误检");
                    return new ArrayList<>();
                }
            } catch (Exception e) {
                logger.warn("AI火焰确认失败，返回颜色检测结果（可能有误差）: {}", e.getMessage());
                // AI不可用时，只返回高置信度的结果（降低误检）
                List<Map<String, Object>> highConfidenceFires = new ArrayList<>();
                for (Map<String, Object> fire : suspectedFires) {
                    double conf = (double) fire.getOrDefault("confidence", 0.0);
                    if (conf > 0.75) { // 只有置信度很高才返回
                        highConfidenceFires.add(fire);
                    }
                }
                return highConfidenceFires;
            }
        }

        // 没有AI服务时，只返回高置信度结果
        List<Map<String, Object>> highConfidenceFires = new ArrayList<>();
        for (Map<String, Object> fire : suspectedFires) {
            double conf = (double) fire.getOrDefault("confidence", 0.0);
            if (conf > 0.8) {
                highConfidenceFires.add(fire);
            }
        }
        return highConfidenceFires;
    }

    /**
     * 基于颜色的火焰检测算法
     * 使用RGB + HSV 双重颜色空间检测火焰区域，结合形状特征验证
     * 经过严格筛选，大幅降低误检率
     */
    private List<Map<String, Object>> detectFireByColor(BufferedImage image, int imgWidth, int imgHeight) {
        List<Map<String, Object>> fires = new ArrayList<>();

        if (image == null || imgWidth < 10 || imgHeight < 10) {
            return fires;
        }

        try {
            // 缩小图像加速计算（采样）
            int sampleStep = Math.max(1, Math.min(imgWidth, imgHeight) / 250);
            int sampleW = imgWidth / sampleStep;
            int sampleH = imgHeight / sampleStep;

            // 创建火焰像素掩码和像素分数（用于置信度计算）
            boolean[][] fireMask = new boolean[sampleH][sampleW];
            int firePixelCount = 0;

            for (int y = 0; y < sampleH; y++) {
                for (int x = 0; x < sampleW; x++) {
                    int px = x * sampleStep;
                    int py = y * sampleStep;
                    if (px >= imgWidth || py >= imgHeight) continue;

                    int rgb = image.getRGB(px, py);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // RGB空间火焰颜色判定（严格条件）
                    boolean rgbFire = isFireColorRGB(r, g, b);

                    // HSV空间火焰颜色判定（补充验证）
                    boolean hsvFire = isFireColorHSV(r, g, b);

                    // 双重验证：RGB和HSV都满足才认为是火焰像素
                    // 这样可以大幅降低红色衣服、红色设备的误检
                    if (rgbFire && hsvFire) {
                        fireMask[y][x] = true;
                        firePixelCount++;
                    }
                }
            }

            // 火焰像素占比太低，认为没有火焰（提高阈值减少误检）
            double fireRatio = (double) firePixelCount / (sampleW * sampleH);
            if (fireRatio < 0.003) { // 至少0.3%的像素
                return fires;
            }

            // 使用连通域分析找到火焰区域
            boolean[][] visited = new boolean[sampleH][sampleW];
            List<int[]> regions = new ArrayList<>();

            for (int y = 0; y < sampleH; y++) {
                for (int x = 0; x < sampleW; x++) {
                    if (fireMask[y][x] && !visited[y][x]) {
                        // BFS 找到连通域
                        List<int[]> queue = new ArrayList<>();
                        queue.add(new int[]{x, y});
                        visited[y][x] = true;

                        int minX = x, maxX = x, minY = y, maxY = y;
                        int count = 0;

                        while (!queue.isEmpty()) {
                            int[] curr = queue.remove(queue.size() - 1);
                            int cx = curr[0], cy = curr[1];
                            count++;

                            minX = Math.min(minX, cx);
                            maxX = Math.max(maxX, cx);
                            minY = Math.min(minY, cy);
                            maxY = Math.max(maxY, cy);

                            // 8邻域
                            int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
                            for (int[] dir : dirs) {
                                int nx = cx + dir[0];
                                int ny = cy + dir[1];
                                if (nx >= 0 && nx < sampleW && ny >= 0 && ny < sampleH
                                        && fireMask[ny][nx] && !visited[ny][nx]) {
                                    visited[ny][nx] = true;
                                    queue.add(new int[]{nx, ny});
                                }
                            }
                        }

                        // 过滤掉太小的区域（噪声）
                        int regionW = maxX - minX + 1;
                        int regionH = maxY - minY + 1;
                        if (count > 20 && regionW > 4 && regionH > 4) {
                            // 形状验证：火焰通常是不规则形状，宽高比有一定范围
                            double aspectRatio = (double) Math.max(regionW, regionH) / Math.min(regionW, regionH);
                            if (aspectRatio < 5.0) { // 排除细长条状（可能是红色线条/标志）
                                // 密度验证：区域内火焰像素占比要足够高
                                double density = (double) count / (regionW * regionH);
                                if (density > 0.35) {
                                    regions.add(new int[]{minX, minY, maxX, maxY, count});
                                }
                            }
                        }
                    }
                }
            }

            // 合并高度重叠的区域
            List<int[]> mergedRegions = mergeOverlappingRegions(regions);

            // 转换为输出格式
            for (int[] region : mergedRegions) {
                int minX = region[0];
                int minY = region[1];
                int maxX = region[2];
                int maxY = region[3];
                int count = region[4];

                int regionW = maxX - minX + 1;
                int regionH = maxY - minY + 1;
                double density = (double) count / (regionW * regionH);

                // 计算区域颜色特征（用于更精确的置信度）
                double[] colorScore = calculateRegionColorScore(image, minX * sampleStep, minY * sampleStep,
                        regionW * sampleStep, regionH * sampleStep, sampleStep);

                // 置信度计算：综合密度、区域大小、颜色特征
                double confidence = Math.min(0.95,
                        0.3 + density * 0.35 + Math.min(count / 300.0, 0.15) + colorScore[0] * 0.2);

                // 严格过滤
                double relW = (double) (regionW * sampleStep) / imgWidth;
                double relH = (double) (regionH * sampleStep) / imgHeight;
                // 排除占比过大的区域（可能是整张图偏红）和过小的区域
                if (confidence > 0.6 && relW < 0.6 && relH < 0.6 && relW > 0.01 && relH > 0.01 && density > 0.4) {
                    Map<String, Object> fire = new HashMap<>();
                    fire.put("x", (double) (minX * sampleStep) / imgWidth);
                    fire.put("y", (double) (minY * sampleStep) / imgHeight);
                    fire.put("width", (double) (regionW * sampleStep) / imgWidth);
                    fire.put("height", (double) (regionH * sampleStep) / imgHeight);
                    fire.put("confidence", confidence);
                    fire.put("fireType", "color-based");
                    fire.put("pixelCount", count);
                    fires.add(fire);
                }
            }

            logger.info("基于颜色的火焰检测完成: 检测到 {} 处火焰区域", fires.size());

        } catch (Exception e) {
            logger.warn("火焰检测失败，返回空结果: {}", e.getMessage());
        }

        return fires;
    }

    /**
     * RGB空间火焰颜色判定
     * 严格的火焰颜色条件
     */
    private boolean isFireColorRGB(int r, int g, int b) {
        // 1. 红色分量足够高
        if (r < 190) return false;
        // 2. 红色 > 绿色 > 蓝色（火焰的典型颜色顺序）
        if (!(r > g && g > b)) return false;
        // 3. 绿色分量不能太低（排除纯红，火焰通常带点黄/橙）
        if (g < 70) return false;
        // 4. 蓝色分量不能太高（排除白色/灰色/粉色）
        if (b > 110) return false;
        // 5. 饱和度足够高
        double saturation = r > 0 ? (r - Math.min(g, b)) / (double) r : 0;
        if (saturation < 0.3) return false;
        // 6. 亮度不能太低也不能太高（排除太暗或过曝）
        double brightness = (r + g + b) / 3.0;
        if (brightness < 100 || brightness > 245) return false;

        return true;
    }

    /**
     * HSV空间火焰颜色判定
     * 火焰色调主要分布在红色到橙黄色范围
     */
    private boolean isFireColorHSV(int r, int g, int b) {
        float[] hsv = rgbToHsv(r, g, b);
        float h = hsv[0]; // 色相 0-360
        float s = hsv[1]; // 饱和度 0-1
        float v = hsv[2]; // 明度 0-1

        // 火焰的色调范围：红色(0-20) + 橙色(20-45) + 黄色(45-60)
        // 注意：红色在HSV中跨越0度，需要特殊处理
        boolean hueInFireRange = (h >= 0 && h <= 55) || (h >= 350 && h <= 360);

        if (!hueInFireRange) return false;

        // 饱和度不能太低（排除白色/灰色/淡色）
        if (s < 0.25) return false;

        // 明度要足够高（火焰是明亮的）
        if (v < 0.5) return false;

        // 橙黄色区域（20-50度）需要更高的饱和度和明度（更严格）
        if (h >= 20 && h <= 50) {
            if (s < 0.35 || v < 0.6) return false;
        }

        return true;
    }

    /**
     * RGB转HSV颜色空间
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

    /**
     * 计算区域颜色特征得分
     * 返回 [颜色得分, 平均饱和度, 平均亮度]
     */
    private double[] calculateRegionColorScore(BufferedImage image, int startX, int startY,
                                                 int width, int height, int step) {
        if (width <= 0 || height <= 0) {
            return new double[]{0.3, 0.3, 0.5};
        }

        int sampleStep = Math.max(1, step / 2);
        int count = 0;
        double totalSat = 0;
        double totalVal = 0;
        double hueMatchCount = 0;

        int imgW = image.getWidth();
        int imgH = image.getHeight();

        for (int y = startY; y < startY + height && y < imgH; y += sampleStep) {
            for (int x = startX; x < startX + width && x < imgW; x += sampleStep) {
                if (x < 0 || y < 0) continue;
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                float[] hsv = rgbToHsv(r, g, b);
                totalSat += hsv[1];
                totalVal += hsv[2];

                // 检查是否在火焰色调范围内
                if ((hsv[0] >= 0 && hsv[0] <= 55) || (hsv[0] >= 350 && hsv[0] <= 360)) {
                    if (hsv[1] > 0.3 && hsv[2] > 0.5) {
                        hueMatchCount++;
                    }
                }
                count++;
            }
        }

        if (count == 0) return new double[]{0.3, 0.3, 0.5};

        double avgSat = totalSat / count;
        double avgVal = totalVal / count;
        double hueRatio = hueMatchCount / count;

        // 综合颜色得分：色调匹配占主要，饱和度和明度作为辅助
        double colorScore = hueRatio * 0.6 + avgSat * 0.2 + avgVal * 0.2;

        return new double[]{Math.min(1.0, colorScore), avgSat, avgVal};
    }

    /**
     * 合并高度重叠的检测区域
     */
    private List<int[]> mergeOverlappingRegions(List<int[]> regions) {
        if (regions == null || regions.size() <= 1) {
            return regions;
        }

        List<int[]> result = new ArrayList<>();
        boolean[] merged = new boolean[regions.size()];

        for (int i = 0; i < regions.size(); i++) {
            if (merged[i]) continue;

            int[] curr = regions.get(i).clone();

            for (int j = i + 1; j < regions.size(); j++) {
                if (merged[j]) continue;

                int[] other = regions.get(j);

                // 计算重叠度（IOU）
                int x1 = Math.max(curr[0], other[0]);
                int y1 = Math.max(curr[1], other[1]);
                int x2 = Math.min(curr[2], other[2]);
                int y2 = Math.min(curr[3], other[3]);

                if (x2 > x1 && y2 > y1) {
                    int interArea = (x2 - x1) * (y2 - y1);
                    int area1 = (curr[2] - curr[0]) * (curr[3] - curr[1]);
                    int area2 = (other[2] - other[0]) * (other[3] - other[1]);
                    double iou = (double) interArea / Math.min(area1, area2);

                    if (iou > 0.3) {
                        // 合并区域
                        curr[0] = Math.min(curr[0], other[0]);
                        curr[1] = Math.min(curr[1], other[1]);
                        curr[2] = Math.max(curr[2], other[2]);
                        curr[3] = Math.max(curr[3], other[3]);
                        curr[4] += other[4];
                        merged[j] = true;
                    }
                }
            }

            result.add(curr);
        }

        return result;
    }

    private List<Map<String, Object>> convertPersons(
            org.example.eledangercheck.service.PpeDetectionService.PpeDetectionResult result) {
        List<Map<String, Object>> persons = new ArrayList<>();
        for (var p : result.getPersons()) {
            Map<String, Object> person = new HashMap<>();
            person.put("x", p.getX());
            person.put("y", p.getY());
            person.put("width", p.getWidth());
            person.put("height", p.getHeight());
            person.put("confidence", p.getDetectConfidence());
            person.put("hasHelmet", p.isHasHelmet());
            person.put("hasVest", p.isHasVest());
            person.put("hasSafetyBelt", p.isHasSafetyBelt());
            person.put("usingPhone", p.isUsingPhone());
            person.put("smoking", p.isSmoking());
            persons.add(person);
        }
        return persons;
    }

    private List<Map<String, Object>> convertHelmets(
            org.example.eledangercheck.service.PpeDetectionService.PpeDetectionResult result) {
        List<Map<String, Object>> helmets = new ArrayList<>();
        for (var p : result.getPersons()) {
            if (p.isHasHelmet()) {
                Map<String, Object> h = new HashMap<>();
                double headX = p.getX() + p.getWidth() * 0.15;
                double headY = p.getY();
                double headW = p.getWidth() * 0.7;
                double headH = p.getHeight() * 0.28;
                h.put("x", headX);
                h.put("y", headY);
                h.put("width", headW);
                h.put("height", headH);
                h.put("confidence", p.getHelmetConfidence());
                h.put("color", "AI检测");
                h.put("hasHelmet", true);
                helmets.add(h);
            }
        }
        return helmets;
    }

    private List<Map<String, Object>> convertVests(
            org.example.eledangercheck.service.PpeDetectionService.PpeDetectionResult result) {
        List<Map<String, Object>> vests = new ArrayList<>();
        for (var p : result.getPersons()) {
            if (p.isHasVest()) {
                Map<String, Object> v = new HashMap<>();
                double vestX = p.getX() + p.getWidth() * 0.1;
                double vestY = p.getY() + p.getHeight() * 0.25;
                double vestW = p.getWidth() * 0.8;
                double vestH = p.getHeight() * 0.6;
                v.put("x", vestX);
                v.put("y", vestY);
                v.put("width", vestW);
                v.put("height", vestH);
                v.put("confidence", p.getVestConfidence());
                v.put("hasVest", true);
                vests.add(v);
            }
        }
        return vests;
    }

    private List<Map<String, Object>> convertSafetyBelts(
            org.example.eledangercheck.service.PpeDetectionService.PpeDetectionResult result) {
        List<Map<String, Object>> belts = new ArrayList<>();
        for (var p : result.getPersons()) {
            if (p.isHasSafetyBelt()) {
                Map<String, Object> b = new HashMap<>();
                double beltX = p.getX() + p.getWidth() * 0.15;
                double beltY = p.getY() + p.getHeight() * 0.3;
                double beltW = p.getWidth() * 0.7;
                double beltH = p.getHeight() * 0.25;
                b.put("x", beltX);
                b.put("y", beltY);
                b.put("width", beltW);
                b.put("height", beltH);
                b.put("confidence", p.getSafetyBeltConfidence());
                b.put("hasSafetyBelt", true);
                belts.add(b);
            }
        }
        return belts;
    }

    /**
     * 增强的图片读取方法，尝试多种方式解码图片
     */
    private BufferedImage readImage(byte[] imageBytes, String filename) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        
        // 方式1：标准ImageIO读取
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img != null) {
                return img;
            }
        } catch (Exception e) {
            logger.warn("ImageIO标准读取失败: {}", e.getMessage());
        }
        
        // 方式2：尝试通过ImageReader读取（遍历所有可用reader）
        try {
            javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
            if (iis != null) {
                java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
                while (readers.hasNext()) {
                    javax.imageio.ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis, true, true);
                        BufferedImage img = reader.read(0);
                        if (img != null) {
                            return img;
                        }
                    } catch (Exception e) {
                        logger.debug("Reader {} 读取失败: {}", reader.getFormatName(), e.getMessage());
                    } finally {
                        reader.dispose();
                    }
                }
                iis.close();
            }
        } catch (Exception e) {
            logger.warn("ImageReader方式读取失败: {}", e.getMessage());
        }
        
        // 方式3：检测文件头并尝试修复
        if (imageBytes.length > 8) {
            int b0 = imageBytes[0] & 0xFF;
            int b1 = imageBytes[1] & 0xFF;
            int b2 = imageBytes[2] & 0xFF;
            int b3 = imageBytes[3] & 0xFF;
            
            logger.warn("图片文件头检测: 0x{} 0x{} 0x{} 0x{} (文件名: {})", 
                Integer.toHexString(b0), Integer.toHexString(b1), 
                Integer.toHexString(b2), Integer.toHexString(b3), filename);
            
            // JPEG: FF D8 FF
            // PNG: 89 50 4E 47
            // GIF: 47 49 46 38
            // BMP: 42 4D
            // WebP: 52 49 46 46 ... 57 45 42 50
        }
        
        return null;
    }
}
