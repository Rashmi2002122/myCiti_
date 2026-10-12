package com.myciti.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    /**
     * Store an uploaded image, resize to max 1200px width/height maintaining aspect ratio,
     * compress to JPEG/PNG, and return the relative or public accessible URL.
     */
    String storeImage(MultipartFile file) throws IOException;

    /**
     * Delete an image by its URL or relative path if applicable.
     */
    void deleteImage(String url);
}
