package com.sport.court.booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** Guarda imágenes subidas (canchas y categorías) en el directorio configurado y devuelve su URL pública. */
@Service
public class FileStorageService {

    private final String uploadDir;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /** @return URL relativa (/uploads/...) o null si no se envió archivo. */
    public String store(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return null;
        String original = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
        String safeName = original.replaceAll("[^A-Za-z0-9._-]", "_");
        String filename = UUID.randomUUID() + "_" + safeName;
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Files.copy(image.getInputStream(), uploadPath.resolve(filename));
        return "/uploads/" + filename;
    }
}
