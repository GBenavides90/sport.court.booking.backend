package com.sport.court.booking.controller;

import com.sport.court.booking.domain.Category;
import com.sport.court.booking.service.CategoryService;
import com.sport.court.booking.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Categorías de canchas (HU12, HU20, HU21)")
public class CategoryController {

    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Listar categorías (público)")
    @GetMapping
    public ResponseEntity<List<Category>> list() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    @Operation(summary = "Agregar categoría (ADMIN) — título, descripción e imagen representativa")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@RequestParam(required = false) String title,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            Category saved = categoryService.create(title, description, fileStorageService.store(image));
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al subir imagen"));
        }
    }
}
