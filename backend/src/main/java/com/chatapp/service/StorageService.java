package com.chatapp.service;

import com.chatapp.config.ChatProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final ChatProperties properties;

    public String store(MultipartFile file) throws IOException {
        String extension = extension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;

        String provider = properties.getStorage().getProvider();
        return switch (provider.toUpperCase()) {
            case "S3" -> storeS3(file, filename);
            case "R2" -> storeR2(file, filename);
            default -> storeLocal(file, filename);
        };
    }

    private String storeLocal(MultipartFile file, String filename) throws IOException {
        Path uploadDir = Paths.get(properties.getStorage().getLocal().getUploadDir());
        Files.createDirectories(uploadDir);
        Files.copy(file.getInputStream(), uploadDir.resolve(filename));
        return "/uploads/" + filename;
    }

    private String storeS3(MultipartFile file, String filename) {
        throw new UnsupportedOperationException("S3 storage not yet configured");
    }

    private String storeR2(MultipartFile file, String filename) {
        throw new UnsupportedOperationException("R2 storage not yet configured");
    }

    private static String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
