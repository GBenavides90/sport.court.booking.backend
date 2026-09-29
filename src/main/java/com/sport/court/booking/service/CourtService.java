package com.sport.court.booking.service;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
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

    /**
     * HU4 — Retorna canchas en orden aleatorio cuando random=true.
     * Implementado en memoria para evitar errores de dialecto SQL (como RAND() vs RANDOM())
     * que estaban causando fallos de despliegue en producción.
     */
    public Page<Court> getAllCourts(Pageable pageable, boolean random) {
        if (random) {
            List<Court> allCourts = courtRepository.findAll();
            Collections.shuffle(allCourts);
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allCourts.size());
            
            List<Court> pageContent = allCourts.subList(start, end);
            return new PageImpl<>(pageContent, pageable, allCourts.size());
        }
        return courtRepository.findAll(pageable);
    }

    /** Compatibilidad hacia atrás — usado por Admin (listado no aleatorio) */
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
