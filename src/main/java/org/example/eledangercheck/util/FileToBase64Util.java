package org.example.eledangercheck.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileToBase64Util {

    private static final Logger logger = LoggerFactory.getLogger(FileToBase64Util.class);

    /**
     * Convert local image file to pure base64 string (without data:image/xxx;base64, prefix)
     * @param filePath absolute path of image file
     * @return pure base64 string, null if failed
     */
    public static String fileToBase64(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("File path is empty");
            return null;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("File not found: {}", filePath);
            return null;
        }
        if (!file.isFile()) {
            logger.warn("Not a file: {}", filePath);
            return null;
        }

        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")
                && !fileName.endsWith(".png") && !fileName.endsWith(".bmp")) {
            logger.warn("Unsupported image format: {}", fileName);
            return null;
        }

        try (InputStream is = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int offset = 0;
            int read;
            while (offset < bytes.length && (read = is.read(bytes, offset, bytes.length - offset)) > 0) {
                offset += read;
            }
            return Base64Utils.encodeToString(bytes);
        } catch (IOException e) {
            logger.error("Failed to read file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Convert byte array to pure base64 string
     * @param bytes image byte array
     * @return pure base64 string
     */
    public static String bytesToBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return Base64Utils.encodeToString(bytes);
    }
}
