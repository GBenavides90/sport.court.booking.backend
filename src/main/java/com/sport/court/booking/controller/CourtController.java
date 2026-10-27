package com.sport.court.booking.controller;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.service.CourtService;
import com.sport.court.booking.service.CourtService.CourtInput;
import com.sport.court.booking.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
@Tag(name = "Courts", description = "API para gestión de canchas deportivas")
public class CourtController {

    private final CourtService courtService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Crear nueva cancha (ADMIN)",
            description = "Crea una cancha con categoría, características y opcionalmente galería de imágenes (HU6)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCourt(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Long categoryId,
            @RequestParam Integer capacity,
            @RequestParam(value = "featureIds", required = false) List<Long> featureIds,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "galleryImages", required = false) List<String> galleryImages) {
        try {
            String mainImageUrl = fileStorageService.store(image);
            List<String> allImages = new java.util.ArrayList<>();
            if (mainImageUrl != null && !mainImageUrl.isBlank()) allImages.add(mainImageUrl);
            if (galleryImages != null) allImages.addAll(galleryImages);
            CourtInput in = new CourtInput(name, description, categoryId, capacity, toSet(featureIds),
                    mainImageUrl, allImages);
            return new ResponseEntity<>(courtService.createCourt(in), HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al subir imagen"));
        }
    }

    @Operation(summary = "Crear nueva cancha vía JSON (ADMIN)")
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCourtJson(@RequestBody CourtInput in) {
        return new ResponseEntity<>(courtService.createCourt(in), HttpStatus.CREATED);
    }

    @Operation(summary = "Editar cancha (ADMIN)", description = "Permite cambiar categoría, características e imágenes")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCourt(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Long categoryId,
            @RequestParam Integer capacity,
            @RequestParam(value = "featureIds", required = false) List<Long> featureIds,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "galleryImages", required = false) List<String> galleryImages) {
        try {
            String mainImageUrl = fileStorageService.store(image);
            List<String> allImages = new java.util.ArrayList<>();
            if (mainImageUrl != null && !mainImageUrl.isBlank()) allImages.add(mainImageUrl);
            if (galleryImages != null) allImages.addAll(galleryImages);
            CourtInput in = new CourtInput(name, description, categoryId, capacity, toSet(featureIds),
                    mainImageUrl, allImages);
            return ResponseEntity.ok(courtService.updateCourt(id, in));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al subir imagen"));
        }
    }

    @Operation(summary = "Editar cancha vía JSON (ADMIN)")
    @PutMapping(value = "/{id}/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateCourtJson(@PathVariable Long id, @RequestBody CourtInput in) {
        return ResponseEntity.ok(courtService.updateCourt(id, in));
    }

    @Operation(summary = "Listar canchas paginadas",
            description = "random=true devuelve orden aleatorio (HU4). categoryIds filtra por una o varias categorías (HU20)")
    @GetMapping
    public ResponseEntity<Page<Court>> getAllCourts(
            @RequestParam(defaultValue = "0")     int page,
            @RequestParam(defaultValue = "10")    int size,
            @RequestParam(defaultValue = "false") boolean random,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds) {
        return ResponseEntity.ok(courtService.getAllCourts(PageRequest.of(page, size), random, toSet(categoryIds)));
    }

    @Operation(summary = "Total de canchas registradas (sin filtros)")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("total", courtService.count()));
    }

    @Operation(summary = "Obtener cancha por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Court> getCourtById(@PathVariable Long id) {
        return courtService.getCourtById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Búsqueda interactiva de canchas (HU22)")
    @GetMapping("/search")
    public ResponseEntity<List<Court>> searchCourts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds) {
        return ResponseEntity.ok(courtService.search(query, startDate, endDate, toSet(categoryIds)));
    }

    @Operation(summary = "Sugerencias de autocompletado de búsqueda (HU22)")
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(courtService.getSuggestions(q));
    }

    @Operation(summary = "Visualizar fechas ocupadas de la cancha (HU23)")
    @GetMapping("/{id}/availability")
    public ResponseEntity<List<String>> getCourtAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(courtService.getOccupiedDates(id));
    }

    @Operation(summary = "Eliminar cancha por ID (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourt(@PathVariable Long id) {
        courtService.deleteCourt(id);
        return ResponseEntity.ok().build();
    }

    private static Set<Long> toSet(List<Long> ids) {
        return ids == null ? new HashSet<>() : new HashSet<>(ids);
    }
}
