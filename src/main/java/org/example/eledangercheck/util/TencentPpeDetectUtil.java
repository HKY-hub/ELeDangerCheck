package org.example.eledangercheck.util;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.tiia.v20190529.TiiaClient;
import com.tencentcloudapi.tiia.v20190529.models.DetectSecurityRequest;
import com.tencentcloudapi.tiia.v20190529.models.DetectSecurityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TencentPpeDetectUtil {

    private static final Logger logger = LoggerFactory.getLogger(TencentPpeDetectUtil.class);

    @Autowired(required = false)
    private TiiaClient tiiaClient;

    /**
     * Detect PPE by image URL (public network image)
     * @param imageUrl public image URL
     * @return DetectSecurityResponse, null if failed
     */
    public DetectSecurityResponse detectByUrl(String imageUrl) {
        if (tiiaClient == null) {
            logger.warn("TiiaClient is not initialized");
            return null;
        }
        if (imageUrl == null || imageUrl.isEmpty()) {
            logger.warn("Image URL is empty");
            return null;
        }

        try {
            DetectSecurityRequest req = new DetectSecurityRequest();
            req.setImageUrl(imageUrl);
            req.setEnableDetect(true);
            req.setEnablePreferred(true);

            DetectSecurityResponse resp = tiiaClient.DetectSecurity(req);
            logger.debug("Tencent PPE detect by URL success, requestId: {}", resp.getRequestId());
            return resp;
        } catch (TencentCloudSDKException e) {
            logger.error("Tencent PPE detect by URL failed: {} - {}", e.getErrorCode(), e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Tencent PPE detect by URL unexpected error", e);
            return null;
        }
    }

    /**
     * Detect PPE by base64 encoded image (without data:image/xxx;base64, prefix)
     * @param pureBase64 pure base64 string of image
     * @return DetectSecurityResponse, null if failed
     */
    public DetectSecurityResponse detectByBase64(String pureBase64) {
        if (tiiaClient == null) {
            logger.warn("TiiaClient is not initialized");
            return null;
        }
        if (pureBase64 == null || pureBase64.isEmpty()) {
            logger.warn("Base64 string is empty");
            return null;
        }

        try {
            DetectSecurityRequest req = new DetectSecurityRequest();
            req.setImageBase64(pureBase64);
            req.setEnableDetect(true);
            req.setEnablePreferred(true);

            long start = System.currentTimeMillis();
            DetectSecurityResponse resp = tiiaClient.DetectSecurity(req);
            long cost = System.currentTimeMillis() - start;

            int bodyCount = resp.getBodies() != null ? resp.getBodies().length : 0;
            logger.info("Tencent PPE detect by base64 success, requestId: {}, bodies: {}, cost: {}ms",
                resp.getRequestId(), bodyCount, cost);
            return resp;
        } catch (TencentCloudSDKException e) {
            logger.error("Tencent PPE detect by base64 failed: {} - {}", e.getErrorCode(), e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Tencent PPE detect by base64 unexpected error", e);
            return null;
        }
    }

    /**
     * Check if Tencent PPE service is available
     */
    public boolean isAvailable() {
        return tiiaClient != null;
    }
}
