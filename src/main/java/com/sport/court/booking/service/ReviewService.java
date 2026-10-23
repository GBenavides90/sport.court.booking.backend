package com.sport.court.booking.service;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Review;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.ReviewRequest;
import com.sport.court.booking.dto.ReviewResponse;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.CourtRepository;
import com.sport.court.booking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** HU28 — Gestión de valoraciones y reseñas con cálculo dinámico en tiempo real. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForCourt(Long courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("La cancha no existe"));
        return reviewRepository.findByCourtOrderByCreatedAtDesc(court)
                .stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse addOrUpdateReview(User user, Long courtId, ReviewRequest request) {
        if (request.score() == null || request.score() < 1 || request.score() > 5) {
            throw new IllegalArgumentException("La puntuación debe ser entre 1 y 5 estrellas");
        }
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("La cancha no existe"));

        Optional<Review> existing = reviewRepository.findByCourtAndUser(court, user);
        Review review;
        if (existing.isPresent()) {
            review = existing.get();
            review.setScore(request.score());
            review.setComment(request.comment());
        } else {
            review = new Review(court, user, request.score(), request.comment());
        }
        Review saved = reviewRepository.save(review);

        // Actualizar media y conteo dinámicamente en tiempo real en la cancha
        updateCourtRating(court);

        return ReviewResponse.from(saved);
    }

    @Transactional
    public void updateCourtRating(Court court) {
        Double avg = reviewRepository.getAverageScoreByCourtId(court.getId());
        long count = reviewRepository.countByCourtId(court.getId());
        if (avg != null && count > 0) {
            BigDecimal rounded = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
            court.setAverageRating(rounded.doubleValue());
            court.setReviewCount((int) count);
        } else {
            court.setAverageRating(5.0);
            court.setReviewCount(0);
        }
        courtRepository.save(court);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRatingSummary(Long courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("La cancha no existe"));
        return Map.of(
                "courtId", courtId,
                "averageRating", court.getAverageRating() != null ? court.getAverageRating() : 5.0,
                "reviewCount", court.getReviewCount() != null ? court.getReviewCount() : 0
        );
    }
}
