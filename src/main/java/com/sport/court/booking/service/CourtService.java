package com.sport.court.booking.service;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;

    public Court createCourt(Court court) {
        if (courtRepository.existsByName(court.getName())) {
            throw new IllegalArgumentException("Ya existe una cancha con ese nombre");
        }
        return courtRepository.save(court);
    }

    public Page<Court> getAllCourts(Pageable pageable) {
        return courtRepository.findAll(pageable);
    }

    public Optional<Court> getCourtById(Long id) {
        return courtRepository.findById(id);
    }

    public void deleteCourt(Long id) {
        if (!courtRepository.existsById(id)) {
            throw new IllegalArgumentException("La cancha no existe");
        }
        courtRepository.deleteById(id);
    }
}
