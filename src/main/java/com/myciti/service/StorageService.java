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
     * Store client logo: type checked by content, max 5 MB, resizes to 400px, re-encodes, strips EXIF.
     */
    default String storeClientLogo(MultipartFile file) throws IOException {
        return storeImage(file);
    }

    /**
     * Store client cover: type checked by content, max 5 MB, resizes to 1200px, re-encodes, strips EXIF.
     */
    default String storeClientCover(MultipartFile file) throws IOException {
        return storeImage(file);
    }

    /**
     * Delete an image by its URL or relative path if applicable.
     */
    void deleteImage(String url);
}
