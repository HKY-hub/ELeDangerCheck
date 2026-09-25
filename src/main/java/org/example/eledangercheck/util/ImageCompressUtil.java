package org.example.eledangercheck.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;

/**
 * 图片压缩工具类
 * 用于将图片压缩到AI接口可接受的体积范围内（Base64后不超过4MB）
 */
public class ImageCompressUtil {

    private static final Logger logger = LoggerFactory.getLogger(ImageCompressUtil.class);

    // 接口限制：Base64后不超过4MB
    private static final int MAX_BASE64_SIZE = 4 * 1024 * 1024;
    // 原始图片最大尺寸（约3MB，因为Base64会增加约33%）
    private static final int MAX_RAW_SIZE = (int) (MAX_BASE64_SIZE / 1.34);
    // 最长边最大像素
    private static final int MAX_SIDE_PIXELS = 4096;

    /**
     * 压缩图片到Base64后不超过4MB
     * 注意：所有图片统一转换为JPEG格式，确保API兼容性
     * @param imageBytes 原始图片字节数组
     * @return 压缩后的图片字节数组（JPEG格式）
     */
    public static byte[] compress(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return imageBytes;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                logger.warn("无法解码图片，返回原始数据");
                return imageBytes;
            }

            int origWidth = image.getWidth();
            int origHeight = image.getHeight();
            int maxSide = Math.max(origWidth, origHeight);

            // 第一步：如果尺寸太大，先缩放
            if (maxSide > MAX_SIDE_PIXELS) {
                double scale = (double) MAX_SIDE_PIXELS / maxSide;
                int newWidth = (int) (origWidth * scale);
                int newHeight = (int) (origHeight * scale);
                image = resizeImage(image, newWidth, newHeight);
                logger.info("图片尺寸过大，已从 {}x{} 缩放到 {}x{}", origWidth, origHeight, newWidth, newHeight);
            }

            // 第二步：统一转为JPEG格式，逐步降低质量直到满足大小限制
            float quality = 0.92f;
            byte[] result = imageToJpegBytes(image, quality);
            int base64Size = (int) (result.length * 1.34);

            while (base64Size > MAX_BASE64_SIZE && quality > 0.3f) {
                quality -= 0.1f;
                result = imageToJpegBytes(image, quality);
                base64Size = (int) (result.length * 1.34);
            }

            // 如果质量降到0.3还是太大，继续缩小尺寸
            if (base64Size > MAX_BASE64_SIZE) {
                int currentWidth = image.getWidth();
                int currentHeight = image.getHeight();
                while (base64Size > MAX_BASE64_SIZE && currentWidth > 400) {
                    currentWidth = (int) (currentWidth * 0.7);
                    currentHeight = (int) (currentHeight * 0.7);
                    BufferedImage resized = resizeImage(image, currentWidth, currentHeight);
                    result = imageToJpegBytes(resized, 0.8f);
                    base64Size = (int) (result.length * 1.34);
                }
            }

            logger.info("图片压缩完成: 原始大小={}KB, 压缩后大小={}KB, Base64估算={}KB, 尺寸={}x{}",
                    imageBytes.length / 1024, result.length / 1024, base64Size / 1024,
                    image.getWidth(), image.getHeight());
            return result;

        } catch (Exception e) {
            logger.error("图片压缩失败，返回原始图片: {}", e.getMessage());
            return imageBytes;
        }
    }

    /**
     * 将图片转换为Base64字符串（自动压缩到接口限制范围内）
     */
    public static String toCompressedBase64(byte[] imageBytes) {
        byte[] compressed = compress(imageBytes);
        return Base64.getEncoder().encodeToString(compressed);
    }

    /**
     * 缩放图片
     */
    private static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    /**
     * 将BufferedImage转换为JPEG字节数组
     */
    private static byte[] imageToJpegBytes(BufferedImage image, float quality) throws Exception {
        // 确保是RGB格式
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            // fallback
            ImageIO.write(rgbImage, "jpg", baos);
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        }
        writer.dispose();

        return baos.toByteArray();
    }
}
