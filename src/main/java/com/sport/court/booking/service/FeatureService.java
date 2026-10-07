package com.sport.court.booking.service;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Feature;
import com.sport.court.booking.dto.FeatureRequest;
import com.sport.court.booking.exception.ConflictException;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.CourtRepository;
import com.sport.court.booking.repository.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** HU17 — Administrar características (alta, edición, eliminación). */
@Service
@RequiredArgsConstructor
public class FeatureService {

    /** Claves de íconos disponibles; el frontend las traduce a íconos Material. */
    public static final List<String> ALLOWED_ICONS = List.of(
            "wifi", "parking", "shower", "lockers", "lighting", "cafe", "water",
            "restroom", "ac", "tv", "firstaid", "security", "bleachers", "equipment");

    private final FeatureRepository featureRepository;
    private final CourtRepository courtRepository;

    public List<Feature> listAll() {
        return featureRepository.findAll();
    }

    public Feature create(FeatureRequest req) {
        String name = req.name().trim();
        validateIcon(req.icon());
        if (featureRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una característica con ese nombre");
        }
        Feature f = new Feature();
        f.setName(name);
        f.setIcon(req.icon());
        return featureRepository.save(f);
    }

    public Feature update(Long id, FeatureRequest req) {
        Feature f = featureRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La característica no existe"));
        String name = req.name().trim();
        validateIcon(req.icon());
        if (featureRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una característica con ese nombre");
        }
        f.setName(name);
        f.setIcon(req.icon());
        return featureRepository.save(f);
    }

    /** Elimina la característica y la desasocia de las canchas que la tuvieran. */
    @Transactional
    public void delete(Long id) {
        Feature f = featureRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La característica no existe"));
        for (Court court : courtRepository.findByFeaturesId(id)) {
            court.getFeatures().removeIf(x -> x.getId().equals(id));
            courtRepository.save(court);
        }
        featureRepository.delete(f);
    }

    public Set<Feature> findAllByIds(Set<Long> ids) {
        List<Feature> found = featureRepository.findAllById(ids);
        if (found.size() != ids.size()) throw new IllegalArgumentException("Alguna característica seleccionada no existe");
        return new java.util.LinkedHashSet<>(found);
    }

    private void validateIcon(String icon) {
        if (!ALLOWED_ICONS.contains(icon)) throw new IllegalArgumentException("El ícono seleccionado no es válido");
    }
}
