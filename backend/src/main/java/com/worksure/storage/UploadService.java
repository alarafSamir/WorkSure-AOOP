package com.worksure.storage;

import com.worksure.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {
    private static final Set<String> ALLOWED = Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf");

    private final Path uploadDir;
    private final String publicBase;

    public UploadService(
            @Value("${app.upload-dir:uploads}") String uploadDir,
            @Value("${app.api-public-url:http://localhost:5000}") String publicBase
    ) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.publicBase = publicBase.endsWith("/") ? publicBase.substring(0, publicBase.length() - 1) : publicBase;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "File required");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED.contains(ext)) {
            throw new ApiException(400, "Only images and PDF are allowed");
        }
        String name = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path dest = uploadDir.resolve(name);
        try {
            file.transferTo(dest.toFile());
        } catch (IOException e) {
            throw new ApiException(500, "Could not save file");
        }
        return publicBase + "/uploads/" + name;
    }
}
