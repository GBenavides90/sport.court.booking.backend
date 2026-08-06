package com.sport.court.booking.controller;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.service.CourtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
@Tag(name = "Courts", description = "API para gestión de canchas deportivas")
public class CourtController {

    private final CourtService courtService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Operation(summary = "Crear nueva cancha", description = "Crea una cancha con datos y opcionalmente una imagen")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCourt(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam Integer capacity,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            Court court = new Court();
            court.setName(name);
            court.setDescription(description);
            court.setCategory(category);
            court.setCapacity(capacity);

            if (image != null && !image.isEmpty()) {
                String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Files.copy(image.getInputStream(), uploadPath.resolve(filename));
                court.setImageUrl("/uploads/" + filename);
            }
            Court savedCourt = courtService.createCourt(court);
            return new ResponseEntity<>(savedCourt, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al subir imagen");
        }
    }

    @Operation(summary = "Listar canchas paginadas")
    @GetMapping
    public ResponseEntity<Page<Court>> getAllCourts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courtService.getAllCourts(PageRequest.of(page, size)));
    }

    @Operation(summary = "Obtener cancha por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Court> getCourtById(@PathVariable Long id) {
        return courtService.getCourtById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar cancha por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourt(@PathVariable Long id) {
        try {
            courtService.deleteCourt(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
