package com.sport.court.booking.service;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Favorite;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.CourtRepository;
import com.sport.court.booking.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** HU24 / HU25 — Gestión de favoritos de usuarios autenticados. */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public List<Court> getUserFavorites(User user) {
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(Favorite::getCourt)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Long> getUserFavoriteCourtIds(User user) {
        return favoriteRepository.findCourtIdsByUserId(user.getId());
    }

    @Transactional
    public boolean toggleFavorite(User user, Long courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("La cancha no existe"));
        Optional<Favorite> existing = favoriteRepository.findByUserAndCourt(user, court);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false; // Desmarcado
        } else {
            favoriteRepository.save(new Favorite(user, court));
            return true; // Marcado
        }
    }

    @Transactional
    public void addFavorite(User user, Long courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("La cancha no existe"));
        if (!favoriteRepository.existsByUserAndCourt(user, court)) {
            favoriteRepository.save(new Favorite(user, court));
        }
    }

    @Transactional
    public void removeFavorite(User user, Long courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("La cancha no existe"));
        favoriteRepository.findByUserAndCourt(user, court).ifPresent(favoriteRepository::delete);
    }
}
