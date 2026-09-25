package org.example.eledangercheck.service;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.tiia.v20190529.TiiaClient;
import com.tencentcloudapi.tiia.v20190529.models.DetectLabelRequest;
import com.tencentcloudapi.tiia.v20190529.models.DetectLabelResponse;
import com.tencentcloudapi.tiia.v20190529.models.DetectLabelItem;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.util.FileToBase64Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class YoloDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(YoloDetectionService.class);

    @Autowired(required = false)
    private TiiaClient tiiaClient;

    private static final double CONF_THRESHOLD = 0.25;

    /**
     * 检测图片中的通用目标（使用腾讯云DetectLabel API）
     * 返回格式与原YOLO检测保持兼容
     */
    public Map<String, Object> detect(MultipartFile file) {
        if (tiiaClient == null) {
            throw new BusinessException("腾讯云TIIA API未配置，请配置 tencent.tiia.secret-id 和 tencent.tiia.secret-key");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        try {
            byte[] imageBytes = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new BusinessException("无法解码图片文件");
            }

            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();

            // 自动压缩图片到腾讯云限制范围内（Base64后≤4MB）
            String base64 = org.example.eledangercheck.util.ImageCompressUtil.toCompressedBase64(imageBytes);
            if (base64 == null || base64.isEmpty()) {
                throw new BusinessException("图片转Base64失败");
            }

            DetectLabelRequest req = new DetectLabelRequest();
            req.setImageBase64(base64);

            long startTime = System.currentTimeMillis();
            DetectLabelResponse resp = tiiaClient.DetectLabel(req);
            long cost = System.currentTimeMillis() - startTime;

            DetectLabelItem[] labels = resp.getLabels();
            // 如果Web版标签为空，尝试使用Camera版标签
            if (labels == null || labels.length == 0) {
                labels = resp.getCameraLabels();
            }
            // 如果Camera版也为空，尝试使用Album版标签
            if (labels == null || labels.length == 0) {
                labels = resp.getAlbumLabels();
            }

            List<Map<String, Object>> detections = new ArrayList<>();

            if (labels != null) {
                int classId = 0;
                for (DetectLabelItem label : labels) {
                    String name = label.getName();
                    Long confidence = label.getConfidence();
                    if (name == null || confidence == null) continue;

                    double conf = confidence / 100.0; // 腾讯云返回0-100的置信度
                    if (conf < CONF_THRESHOLD) continue;

                    // 腾讯云DetectLabel是图像标签识别，不提供检测框坐标
                    // 为了保持返回格式兼容，使用整张图片作为占位框
                    Map<String, Object> detection = new HashMap<>();
                    detection.put("classId", classId++);
                    detection.put("className", name);
                    detection.put("x", 0.0);
                    detection.put("y", 0.0);
                    detection.put("width", 1.0);
                    detection.put("height", 1.0);
                    detection.put("confidence", conf);
                    detection.put("firstCategory", label.getFirstCategory());
                    detection.put("secondCategory", label.getSecondCategory());

                    detections.add(detection);
                }
            }

            logger.info("腾讯云DetectLabel检测完成, 标签数: {}, 耗时: {}ms, 图片: {}x{}",
                detections.size(), cost, imgWidth, imgHeight);

            Map<String, Object> response = new HashMap<>();
            response.put("width", imgWidth);
            response.put("height", imgHeight);
            response.put("detections", detections);
            response.put("count", detections.size());
            response.put("success", true);
            response.put("source", "tencent-detectlabel");
            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            logger.error("腾讯云DetectLabel API调用失败: {} - {}", e.getErrorCode(), e.getMessage(), e);
            throw new BusinessException("图像检测失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("图像检测失败", e);
            throw new BusinessException("图像检测失败: " + e.getMessage());
        }
    }

    /**
     * 检测图片中的人员（基于DetectLabel的person标签）
     * 注意：DetectLabel是图像标签识别，不提供精确坐标
     */
    public List<Map<String, Object>> detectPersons(MultipartFile file) {
        List<Map<String, Object>> persons = new ArrayList<>();
        Map<String, Object> result = detect(file);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detections = (List<Map<String, Object>>) result.get("detections");

        for (Map<String, Object> det : detections) {
            String className = (String) det.get("className");
            if (className != null && className.toLowerCase().contains("person")) {
                Map<String, Object> person = new HashMap<>();
                person.put("x", det.get("x"));
                person.put("y", det.get("y"));
                person.put("width", det.get("width"));
                person.put("height", det.get("height"));
                person.put("confidence", det.get("confidence"));
                person.put("hasHelmet", false);
                persons.add(person);
            }
        }

        return persons;
    }

    /**
     * 检查腾讯云API是否可用
     */
    public boolean isModelLoaded() {
        return tiiaClient != null;
    }
}
