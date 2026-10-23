package com.sport.court.booking.controller;

import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.ReviewRequest;
import com.sport.court.booking.dto.ReviewResponse;
import com.sport.court.booking.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** HU28 — API de valoraciones y reseñas de canchas. */
@RestController
@RequestMapping("/api/courts/{courtId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Puntuación y reseñas de canchas deportivas (HU28)")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Listar valoraciones de una cancha (público)")
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long courtId) {
        return ResponseEntity.ok(reviewService.getReviewsForCourt(courtId));
    }

    @Operation(summary = "Obtener resumen de puntuación media (público)")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable Long courtId) {
        return ResponseEntity.ok(reviewService.getRatingSummary(courtId));
    }

    @Operation(summary = "Puntuar y valorar cancha (usuario autenticado con reserva)")
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long courtId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal User user) {
        ReviewResponse created = reviewService.addOrUpdateReview(user, courtId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
