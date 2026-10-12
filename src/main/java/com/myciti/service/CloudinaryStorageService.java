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
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit (actual: " + (file.getSize() / 1024 / 1024) + " MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png") && !lower.endsWith(".webp")) {
                throw new IllegalArgumentException("Only JPG, PNG, and WEBP image formats are supported");
            }
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "myciti",
                "transformation", new Transformation<>()
                        .width(1200)
                        .height(1200)
                        .crop("limit")
                        .quality("auto")
                        .fetchFormat("auto")
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Successfully uploaded image to Cloudinary: {}", secureUrl);
        return secureUrl;
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
}
