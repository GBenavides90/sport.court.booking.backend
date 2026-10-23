package com.sport.court.booking.controller;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** HU24 / HU25 — API de favoritos para usuarios autenticados. */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Gestión de canchas favoritas de usuarios autenticados (HU24, HU25)")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "Listar canchas favoritas del usuario autenticado (HU25)")
    @GetMapping
    public ResponseEntity<List<Court>> getFavorites(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(favoriteService.getUserFavorites(user));
    }

    @Operation(summary = "Listar IDs de canchas favoritas del usuario autenticado (HU24)")
    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getFavoriteIds(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(favoriteService.getUserFavoriteCourtIds(user));
    }

    @Operation(summary = "Alternar favorito (marcar/desmarcar con un clic) (HU24)")
    @PostMapping("/{courtId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable Long courtId,
            @AuthenticationPrincipal User user) {
        boolean isFavorite = favoriteService.toggleFavorite(user, courtId);
        return ResponseEntity.ok(Map.of("courtId", courtId, "isFavorite", isFavorite));
    }

    @Operation(summary = "Marcar cancha como favorita (HU24)")
    @PostMapping("/{courtId}")
    public ResponseEntity<Map<String, Object>> addFavorite(
            @PathVariable Long courtId,
            @AuthenticationPrincipal User user) {
        favoriteService.addFavorite(user, courtId);
        return ResponseEntity.ok(Map.of("courtId", courtId, "isFavorite", true));
    }

    @Operation(summary = "Eliminar cancha de favoritos (HU24, HU25)")
    @DeleteMapping("/{courtId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(
            @PathVariable Long courtId,
            @AuthenticationPrincipal User user) {
        favoriteService.removeFavorite(user, courtId);
        return ResponseEntity.ok(Map.of("courtId", courtId, "isFavorite", false));
    }
}
