package com.sport.court.booking.controller;

import com.sport.court.booking.domain.Feature;
import com.sport.court.booking.dto.FeatureRequest;
import com.sport.court.booking.service.FeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
@Tag(name = "Features", description = "Características de canchas (HU17, HU18)")
public class FeatureController {

    private final FeatureService featureService;

    @Operation(summary = "Listar características (público)")
    @GetMapping
    public ResponseEntity<List<Feature>> list() {
        return ResponseEntity.ok(featureService.listAll());
    }

    @Operation(summary = "Claves de íconos disponibles")
    @GetMapping("/icons")
    public ResponseEntity<List<String>> icons() {
        return ResponseEntity.ok(FeatureService.ALLOWED_ICONS);
    }

    @Operation(summary = "Añadir característica (ADMIN) — nombre e ícono")
    @PostMapping
    public ResponseEntity<Feature> create(@Valid @RequestBody FeatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(featureService.create(request));
    }

    @Operation(summary = "Editar característica (ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<Feature> update(@PathVariable Long id, @Valid @RequestBody FeatureRequest request) {
        return ResponseEntity.ok(featureService.update(id, request));
    }

    @Operation(summary = "Eliminar característica (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        featureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
