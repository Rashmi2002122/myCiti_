package com.myciti.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryStorageService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final long CLIENT_MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5 MB

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(
            @Value("${app.storage.cloudinary.cloud-name}") String cloudName,
            @Value("${app.storage.cloudinary.api-key}") String apiKey,
            @Value("${app.storage.cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Initialized Cloudinary storage service for cloud '{}'", cloudName);
    }

    @Override
    public String storeImage(MultipartFile file) throws IOException {
        validateImageFile(file, MAX_FILE_SIZE);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "myciti",
                "transformation", new Transformation<>()
                        .width(1200)
                        .height(1200)
                        .crop("limit")
                        .quality("auto")
                        .fetchFormat("auto")
                        .flags("strip_profile")
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Successfully uploaded image to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    @Override
    public String storeClientLogo(MultipartFile file) throws IOException {
        validateImageFile(file, CLIENT_MAX_FILE_SIZE);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "myciti/clients/logos",
                "transformation", new Transformation<>()
                        .width(400)
                        .crop("limit")
                        .quality("auto")
                        .fetchFormat("auto")
                        .flags("strip_profile")
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Successfully uploaded client logo to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    @Override
    public String storeClientCover(MultipartFile file) throws IOException {
        validateImageFile(file, CLIENT_MAX_FILE_SIZE);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "myciti/clients/covers",
                "transformation", new Transformation<>()
                        .width(1200)
                        .crop("limit")
                        .quality("auto")
                        .fetchFormat("auto")
                        .flags("strip_profile")
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Successfully uploaded client cover to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    private void validateImageFile(MultipartFile file, long maxSizeBytes) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw new IllegalArgumentException("File size exceeds " + maxMb + " MB limit (actual: " + (file.getSize() / 1024 / 1024) + " MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png") && !lower.endsWith(".webp")) {
                throw new IllegalArgumentException("Only JPG, PNG, and WEBP image formats are supported");
            }
        }

        byte[] fileBytes = file.getBytes();
        if (!isValidImageMagicBytes(fileBytes)) {
            throw new IllegalArgumentException("File content does not match a valid JPG, PNG, or WEBP image header");
        }
    }

    @Override
    public void deleteImage(String url) {
        if (url == null || url.isBlank()) return;
        try {
            int folderIdx = url.indexOf("myciti/");
            if (folderIdx != -1) {
                int lastDotIdx = url.lastIndexOf('.');
                String publicId = (lastDotIdx > folderIdx) ? url.substring(folderIdx, lastDotIdx) : url.substring(folderIdx);
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted image '{}' from Cloudinary", publicId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete image '{}' from Cloudinary: {}", url, e.getMessage());
        }
    }

    private boolean isValidImageMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return false;
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true;
        }
        // WEBP: RIFF....WEBP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
            bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return true;
        }
        return false;
    }
}
