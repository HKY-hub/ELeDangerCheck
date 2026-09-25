package org.example.eledangercheck.service;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import org.example.eledangercheck.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 轻量级PPE检测服务
 * 使用专用PPE YOLO模型（YOLO11-Nano）检测安全帽、反光衣等装备
 * 专为2核2G低配置服务器优化
 */
@Service
public class PpeDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(PpeDetectionService.class);

    // 模型配置 - 使用专用PPE检测模型
    private static final String MODEL_PATH = "models/best.onnx";
    private static final int INPUT_SIZE = 640;
    private static final float CONF_THRESHOLD = 0.25f;
    private static final float NMS_THRESHOLD = 0.5f;

    private OrtEnvironment env;
    private OrtSession session;
    private boolean modelLoaded = false;

    // PPE模型类别
    private static final String[] PPE_CLASSES = {
        "Helmet", "NoHelmet", "NoVest", "Vest"
    };

    @PostConstruct
    public void init() {
        logger.info("开始初始化PPE检测服务，模型路径: {}", MODEL_PATH);
        try {
            env = OrtEnvironment.getEnvironment();
            logger.info("ONNX Runtime环境初始化成功，版本: {}", env.getVersion());

            File modelFile = getModelFile();
            if (modelFile == null || !modelFile.exists()) {
                logger.error("PPE模型文件未找到: {}", MODEL_PATH);
                modelLoaded = false;
                loadError = "模型文件未找到，请确保best.onnx文件存在";
                return;
            }

            logger.info("开始加载模型文件: {} (大小: {}字节)", modelFile.getAbsolutePath(), modelFile.length());

            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            options.setIntraOpNumThreads(2); // 2核CPU，使用2线程
            options.setInterOpNumThreads(1);
            session = env.createSession(modelFile.getAbsolutePath(), options);

            modelLoaded = true;
            loadError = null;
            logger.info("PPE YOLO11-Nano模型加载成功: {}, 输入尺寸: {}, 类别数: {}",
                modelFile.getAbsolutePath(), INPUT_SIZE, PPE_CLASSES.length);
        } catch (UnsatisfiedLinkError e) {
            logger.error("PPE模型加载失败 - ONNX Runtime原生库缺失: {}", e.getMessage(), e);
            modelLoaded = false;
            loadError = "ONNX Runtime原生库缺失，无法加载模型: " + e.getMessage();
        } catch (Exception e) {
            logger.error("PPE模型加载失败: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            modelLoaded = false;
            loadError = "模型加载失败: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    private String loadError = null;

    public String getLoadError() {
        return loadError;
    }

    private File getModelFile() {
        // 方式1：尝试从classpath读取（支持开发环境和jar包内资源）
        try {
            ClassPathResource resource = new ClassPathResource(MODEL_PATH);
            if (resource.exists()) {
                // 尝试直接获取文件（开发环境有效）
                try {
                    File file = resource.getFile();
                    if (file != null && file.exists() && file.length() > 1024) {
                        logger.info("从classpath直接加载模型文件: {}", file.getAbsolutePath());
                        return file;
                    }
                } catch (Exception ignored) {}

                // Jar包内：复制到临时文件
                try {
                    File tempFile = File.createTempFile("best-", ".onnx");
                    tempFile.deleteOnExit();
                    java.nio.file.Files.copy(resource.getInputStream(), tempFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    if (tempFile.exists() && tempFile.length() > 1024) {
                        logger.info("从classpath复制模型文件到临时目录: {} (大小: {}字节)",
                            tempFile.getAbsolutePath(), tempFile.length());
                        return tempFile;
                    }
                } catch (Exception e) {
                    logger.warn("从classpath复制模型到临时文件失败: {}", e.getMessage());
                }
            }
        } catch (Exception ignored) {}

        // 方式2：尝试外部路径
        String[] paths = {
            "src/main/resources/" + MODEL_PATH,
            "models/best.onnx",
            "best.onnx",
            "../best.onnx",
            "./best.onnx"
        };

        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.length() > 1024) {
                logger.info("从外部路径加载模型文件: {}", file.getAbsolutePath());
                return file;
            }
        }

        logger.error("PPE模型文件未找到，尝试过的路径: {}", Arrays.toString(paths));
        return null;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public String[] getPpeClasses() {
        return PPE_CLASSES;
    }

    /**
     * PPE检测结果
     */
    public static class PpeDetectionResult {
        public int width;
        public int height;
        public List<PersonPpeInfo> persons = new ArrayList<>();

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public List<PersonPpeInfo> getPersons() { return persons; }
    }

    /**
     * 人员PPE信息
     */
    public static class PersonPpeInfo {
        public double x;
        public double y;
        public double width;
        public double height;
        public double detectConfidence;
        public boolean hasHelmet;
        public double helmetConfidence;
        public boolean hasVest;
        public double vestConfidence;
        public boolean hasSafetyBelt;
        public double safetyBeltConfidence;
        public boolean usingPhone;
        public double phoneConfidence;
        public boolean smoking;
        public double smokingConfidence;

        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public double getDetectConfidence() { return detectConfidence; }
        public boolean isHasHelmet() { return hasHelmet; }
        public double getHelmetConfidence() { return helmetConfidence; }
        public boolean isHasVest() { return hasVest; }
        public double getVestConfidence() { return vestConfidence; }
        public boolean isHasSafetyBelt() { return hasSafetyBelt; }
        public double getSafetyBeltConfidence() { return safetyBeltConfidence; }
        public boolean isUsingPhone() { return usingPhone; }
        public double getPhoneConfidence() { return phoneConfidence; }
        public boolean isSmoking() { return smoking; }
        public double getSmokingConfidence() { return smokingConfidence; }
    }

    /**
     * 检测图片中的PPE违规
     */
    public PpeDetectionResult detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        try {
            byte[] imageBytes = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                logger.warn("PPE检测: ImageIO直接读取失败，尝试备用方式. 文件名={}, 大小={}字节", 
                    file.getOriginalFilename(), imageBytes.length);
                // 尝试备用方式读取
                try {
                    javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
                    if (iis != null) {
                        java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
                        while (readers.hasNext()) {
                            javax.imageio.ImageReader reader = readers.next();
                            try {
                                reader.setInput(iis, true, true);
                                image = reader.read(0);
                                if (image != null) break;
                            } finally {
                                reader.dispose();
                            }
                        }
                        iis.close();
                    }
                } catch (Exception e) {
                    logger.warn("备用图片读取方式也失败: {}", e.getMessage());
                }
                if (image == null) {
                    throw new BusinessException("无法解码图片文件，请确保图片格式为JPG、PNG、GIF、BMP等常见格式");
                }
            }
            return detect(imageBytes, image.getWidth(), image.getHeight());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("PPE检测失败", e);
            throw new BusinessException("PPE检测失败: " + e.getMessage());
        }
    }

    public PpeDetectionResult detect(byte[] imageBytes, int width, int height) {
        PpeDetectionResult result = new PpeDetectionResult();
        result.width = width;
        result.height = height;

        if (!modelLoaded) {
            logger.warn("PPE模型未加载，跳过检测");
            return result;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return result;
            }

            // 运行YOLO检测
            List<Detection> detections = detectYolo(image);

            // 将检测结果按人员分组
            List<PersonPpeInfo> persons = groupDetectionsToPersons(detections, width, height);
            result.persons = persons;

            logger.info("PPE检测完成: 检测到 {} 人, 检测框 {} 个", persons.size(), detections.size());

            return result;
        } catch (Exception e) {
            logger.error("PPE检测异常", e);
            return result;
        }
    }

    /**
     * 内部检测框结构
     */
    private static class Detection {
        float x, y, w, h;
        float conf;
        int classId;
        String className;
        int matchIdx; // 用于匹配的索引

        Detection(float x, float y, float w, float h, float conf, int classId, String className) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.conf = conf;
            this.classId = classId;
            this.className = className;
            this.matchIdx = -1;
        }
    }

    /**
     * 使用YOLO模型检测
     */
    private List<Detection> detectYolo(BufferedImage image) {
        List<Detection> detections = new ArrayList<>();

        try {
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();

            // Letterbox预处理
            float scale = Math.min((float) INPUT_SIZE / imgWidth, (float) INPUT_SIZE / imgHeight);
            int newWidth = Math.round(imgWidth * scale);
            int newHeight = Math.round(imgHeight * scale);
            int padX = (INPUT_SIZE - newWidth) / 2;
            int padY = (INPUT_SIZE - newHeight) / 2;

            // 调整图像大小并填充
            BufferedImage resized = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setColor(new Color(114, 114, 114));
            g2d.fillRect(0, 0, INPUT_SIZE, INPUT_SIZE);
            g2d.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH),
                padX, padY, null);
            g2d.dispose();

            // 转换为CHW格式的float数组
            float[] inputData = new float[3 * INPUT_SIZE * INPUT_SIZE];
            int idx = 0;
            for (int c = 0; c < 3; c++) {
                for (int h = 0; h < INPUT_SIZE; h++) {
                    for (int w = 0; w < INPUT_SIZE; w++) {
                        int rgb = resized.getRGB(w, h);
                        float val;
                        if (c == 0) val = ((rgb >> 16) & 0xFF) / 255.0f;
                        else if (c == 1) val = ((rgb >> 8) & 0xFF) / 255.0f;
                        else val = (rgb & 0xFF) / 255.0f;
                        inputData[idx++] = val;
                    }
                }
            }

            // 准备输入
            String inputName = session.getInputNames().iterator().next();
            OnnxTensor inputTensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(inputData), new long[]{1, 3, INPUT_SIZE, INPUT_SIZE});

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(inputName, inputTensor);

            // 推理
            try (OrtSession.Result output = session.run(inputs)) {
                OnnxTensor outputTensor = (OnnxTensor) output.get(0);

                // 处理输出 - YOLO11输出格式: [batch, num_classes+4, num_anchors]
                float[][][] output3d = (float[][][]) outputTensor.getValue();
                float[][] outputData = output3d[0]; // 取第一个batch

                int numAttributes = outputData.length;
                int numAnchors = outputData[0].length;

                int numClasses = numAttributes - 4;

                List<float[]> boxes = new ArrayList<>();
                List<Float> confidences = new ArrayList<>();
                List<Integer> classIds = new ArrayList<>();

                for (int i = 0; i < numAnchors; i++) {
                    float cx = outputData[0][i];
                    float cy = outputData[1][i];
                    float w = outputData[2][i];
                    float h = outputData[3][i];

                    // 找最大置信度的类别
                    float maxConf = 0;
                    int maxClassId = 0;
                    for (int c = 0; c < numClasses && c < PPE_CLASSES.length; c++) {
                        float conf = outputData[4 + c][i];
                        if (conf > maxConf) {
                            maxConf = conf;
                            maxClassId = c;
                        }
                    }

                    if (maxConf < CONF_THRESHOLD) continue;

                    // 转换坐标（从letterbox坐标还原到原图坐标）
                    float left = (cx - w / 2 - padX) / scale;
                    float top = (cy - h / 2 - padY) / scale;
                    float boxW = w / scale;
                    float boxH = h / scale;

                    // 边界检查
                    left = Math.max(0, Math.min(left, imgWidth - 1));
                    top = Math.max(0, Math.min(top, imgHeight - 1));
                    boxW = Math.max(1, Math.min(boxW, imgWidth - left));
                    boxH = Math.max(1, Math.min(boxH, imgHeight - top));

                    boxes.add(new float[]{left, top, boxW, boxH});
                    confidences.add(maxConf);
                    classIds.add(maxClassId);
                }

                // NMS
                List<Integer> indices = nms(boxes, confidences, NMS_THRESHOLD);

                for (int i : indices) {
                    float[] box = boxes.get(i);
                    float conf = confidences.get(i);
                    int classId = classIds.get(i);
                    String className = classId < PPE_CLASSES.length ? PPE_CLASSES[classId] : "unknown";
                    detections.add(new Detection(box[0], box[1], box[2], box[3], conf, classId, className));
                }
            }

            inputTensor.close();
        } catch (Exception e) {
            logger.error("YOLO PPE检测失败", e);
        }

        logger.info("YOLO检测原始结果: 检测框 {} 个", detections.size());
        for (Detection det : detections) {
            logger.debug("  {}: conf={}, box=[{}, {}, {}, {}]",
                det.className, String.format("%.3f", det.conf),
                (int)det.x, (int)det.y, (int)det.w, (int)det.h);
        }

        return detections;
    }

    /**
     * 将检测结果按人员分组
     * PPE模型检测的是装备（头盔/反光衣等），需要将它们关联到具体的人
     * 策略：找到每个Helmet/NoHelmet检测框，向下扩展找对应的Vest/NoVest
     */
    private List<PersonPpeInfo> groupDetectionsToPersons(List<Detection> detections, int imgWidth, int imgHeight) {
        List<PersonPpeInfo> persons = new ArrayList<>();

        // 分离头部检测和身体检测
        List<Detection> helmetDetections = new ArrayList<>();
        List<Detection> vestDetections = new ArrayList<>();

        for (int i = 0; i < detections.size(); i++) {
            Detection det = detections.get(i);
            det.matchIdx = i;
            if ("Helmet".equals(det.className) || "NoHelmet".equals(det.className)) {
                helmetDetections.add(det);
            } else if ("Vest".equals(det.className) || "NoVest".equals(det.className)) {
                vestDetections.add(det);
            }
        }

        // 如果没有头盔检测，但有反光衣检测，用反光衣推断人的位置
        if (helmetDetections.isEmpty() && !vestDetections.isEmpty()) {
            for (Detection vest : vestDetections) {
                PersonPpeInfo person = new PersonPpeInfo();
                // 假设人的高度大约是反光衣的3倍
                float personY = Math.max(0, vest.y - vest.h * 1.5f);
                float personH = vest.h * 3f;
                person.x = (double) vest.x / imgWidth;
                person.y = (double) personY / imgHeight;
                person.width = (double) vest.w / imgWidth;
                person.height = (double) Math.min(personH, imgHeight - personY) / imgHeight;
                person.detectConfidence = vest.conf;
                person.hasHelmet = false;
                person.helmetConfidence = 0.0;
                person.hasVest = "Vest".equals(vest.className);
                person.vestConfidence = vest.conf;
                person.hasSafetyBelt = false;
                person.usingPhone = false;
                person.smoking = false;
                persons.add(person);
            }
            return persons;
        }

        // 为每个头盔检测创建一个人，并匹配对应的反光衣
        Set<Integer> matchedVests = new HashSet<>();

        for (Detection helmet : helmetDetections) {
            PersonPpeInfo person = new PersonPpeInfo();

            // 人的大致范围：从头盔向下扩展
            float personX = helmet.x - helmet.w * 0.3f;
            float personY = helmet.y;
            float personW = helmet.w * 1.6f;
            float personH = helmet.h * 6f; // 人大约是头的6-7倍高

            // 边界检查
            personX = Math.max(0, personX);
            personY = Math.max(0, personY);
            personW = Math.min(personW, imgWidth - personX);
            personH = Math.min(personH, imgHeight - personY);

            person.x = (double) personX / imgWidth;
            person.y = (double) personY / imgHeight;
            person.width = (double) personW / imgWidth;
            person.height = (double) personH / imgHeight;
            person.detectConfidence = helmet.conf;

            // 头盔状态
            person.hasHelmet = "Helmet".equals(helmet.className);
            person.helmetConfidence = helmet.conf;

            // 找匹配的反光衣（在人的身体区域内）
            float bodyTop = helmet.y + helmet.h;
            float bodyBottom = personY + personH;
            float bodyCenterX = helmet.x + helmet.w / 2f;

            Detection bestVest = null;
            float bestVestScore = 0;

            for (int i = 0; i < vestDetections.size(); i++) {
                if (matchedVests.contains(i)) continue;

                Detection vest = vestDetections.get(i);
                float vestCenterY = vest.y + vest.h / 2f;
                float vestCenterX = vest.x + vest.w / 2f;

                // 检查反光衣是否在身体区域内
                if (vestCenterY >= bodyTop && vestCenterY <= bodyBottom) {
                    // 水平距离评分
                    float xDist = Math.abs(vestCenterX - bodyCenterX);
                    float xScore = Math.max(0, 1 - xDist / (personW / 2f));
                    float score = xScore * vest.conf;

                    if (score > bestVestScore) {
                        bestVestScore = score;
                        bestVest = vest;
                    }
                }
            }

            if (bestVest != null) {
                matchedVests.add(bestVest.matchIdx);
                person.hasVest = "Vest".equals(bestVest.className);
                person.vestConfidence = bestVest.conf;
            } else {
                person.hasVest = false;
                person.vestConfidence = 0.0;
            }

            person.hasSafetyBelt = false;
            person.safetyBeltConfidence = 0.0;
            person.usingPhone = false;
            person.phoneConfidence = 0.0;
            person.smoking = false;
            person.smokingConfidence = 0.0;

            persons.add(person);
        }

        // 处理未匹配的反光衣（可能检测到了反光衣但没检测到头）
        for (int i = 0; i < vestDetections.size(); i++) {
            if (matchedVests.contains(i)) continue;

            Detection vest = vestDetections.get(i);
            PersonPpeInfo person = new PersonPpeInfo();

            float personY = Math.max(0, vest.y - vest.h * 1.5f);
            float personH = vest.h * 3f;

            person.x = (double) vest.x / imgWidth;
            person.y = (double) personY / imgHeight;
            person.width = (double) vest.w / imgWidth;
            person.height = (double) Math.min(personH, imgHeight - personY) / imgHeight;
            person.detectConfidence = vest.conf;
            person.hasHelmet = false;
            person.helmetConfidence = 0.0;
            person.hasVest = "Vest".equals(vest.className);
            person.vestConfidence = vest.conf;
            person.hasSafetyBelt = false;
            person.usingPhone = false;
            person.smoking = false;

            persons.add(person);
        }

        // 人员去重：移除高度重叠的检测框（IOU > 0.5 视为同一人）
        persons = deduplicatePersons(persons);
        logger.debug("人员去重完成: 去重前 {} 人, 去重后 {} 人", persons.size() + 0, persons.size());

        return persons;
    }

    /**
     * 人员去重：基于IOU移除高度重叠的人员检测框
     * 当两个人员检测框的重叠度超过阈值时，保留置信度更高的那个
     */
    private List<PersonPpeInfo> deduplicatePersons(List<PersonPpeInfo> persons) {
        if (persons == null || persons.size() <= 1) {
            return persons;
        }

        List<PersonPpeInfo> result = new ArrayList<>();
        boolean[] removed = new boolean[persons.size()];

        // 按置信度降序排列
        Integer[] indices = new Integer[persons.size()];
        for (int i = 0; i < persons.size(); i++) {
            indices[i] = i;
        }
        Arrays.sort(indices, (a, b) -> Double.compare(
            persons.get(b).getDetectConfidence(),
            persons.get(a).getDetectConfidence()
        ));

        // 逐个检查，移除重叠度高的
        for (int i = 0; i < indices.length; i++) {
            int idx = indices[i];
            if (removed[idx]) continue;

            PersonPpeInfo p1 = persons.get(idx);
            result.add(p1);

            // 与剩余的比较
            for (int j = i + 1; j < indices.length; j++) {
                int jdx = indices[j];
                if (removed[jdx]) continue;

                PersonPpeInfo p2 = persons.get(jdx);
                double iou = calculatePersonIou(p1, p2);

                // IOU > 0.5 视为同一人，移除置信度低的
                if (iou > 0.5) {
                    removed[jdx] = true;
                    // 合并属性：如果p1没有检测到的属性，p2检测到了，就合并
                    if (!p1.hasHelmet && p2.hasHelmet) {
                        p1.hasHelmet = true;
                        p1.helmetConfidence = Math.max(p1.helmetConfidence, p2.helmetConfidence);
                    }
                    if (!p1.hasVest && p2.hasVest) {
                        p1.hasVest = true;
                        p1.vestConfidence = Math.max(p1.vestConfidence, p2.vestConfidence);
                    }
                    if (!p1.hasSafetyBelt && p2.hasSafetyBelt) {
                        p1.hasSafetyBelt = true;
                        p1.safetyBeltConfidence = Math.max(p1.safetyBeltConfidence, p2.safetyBeltConfidence);
                    }
                    if (!p1.usingPhone && p2.usingPhone) {
                        p1.usingPhone = true;
                        p1.phoneConfidence = Math.max(p1.phoneConfidence, p2.phoneConfidence);
                    }
                    if (!p1.smoking && p2.smoking) {
                        p1.smoking = true;
                        p1.smokingConfidence = Math.max(p1.smokingConfidence, p2.smokingConfidence);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 计算两个人体检测框的IOU（交并比）
     */
    private double calculatePersonIou(PersonPpeInfo p1, PersonPpeInfo p2) {
        double x1 = Math.max(p1.getX(), p2.getX());
        double y1 = Math.max(p1.getY(), p2.getY());
        double x2 = Math.min(p1.getX() + p1.getWidth(), p2.getX() + p2.getWidth());
        double y2 = Math.min(p1.getY() + p1.getHeight(), p2.getY() + p2.getHeight());

        if (x2 <= x1 || y2 <= y1) return 0;

        double intersection = (x2 - x1) * (y2 - y1);
        double area1 = p1.getWidth() * p1.getHeight();
        double area2 = p2.getWidth() * p2.getHeight();
        double union = area1 + area2 - intersection;

        return union > 0 ? intersection / union : 0;
    }

    /**
     * 非极大值抑制 (NMS)
     */
    private List<Integer> nms(List<float[]> boxes, List<Float> confidences, float threshold) {
        List<Integer> indices = new ArrayList<>();
        int n = boxes.size();
        if (n == 0) return indices;

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Float.compare(confidences.get(b), confidences.get(a)));

        boolean[] suppressed = new boolean[n];

        for (int i = 0; i < n; i++) {
            int idx = order[i];
            if (suppressed[idx]) continue;

            indices.add(idx);

            for (int j = i + 1; j < n; j++) {
                int jdx = order[j];
                if (suppressed[jdx]) continue;

                if (iou(boxes.get(idx), boxes.get(jdx)) > threshold) {
                    suppressed[jdx] = true;
                }
            }
        }

        return indices;
    }

    private float iou(float[] box1, float[] box2) {
        float x1 = Math.max(box1[0], box2[0]);
        float y1 = Math.max(box1[1], box2[1]);
        float x2 = Math.min(box1[0] + box1[2], box2[0] + box2[2]);
        float y2 = Math.min(box1[1] + box1[3], box2[1] + box2[3]);

        if (x2 <= x1 || y2 <= y1) return 0;

        float intersection = (x2 - x1) * (y2 - y1);
        float area1 = box1[2] * box1[3];
        float area2 = box2[2] * box2[3];
        float union = area1 + area2 - intersection;

        return union > 0 ? intersection / union : 0;
    }
}
