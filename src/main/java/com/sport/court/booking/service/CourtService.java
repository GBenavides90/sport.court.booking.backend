package com.sport.court.booking.service;

import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Court;
import com.sport.court.booking.exception.ConflictException;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final CategoryService categoryService;
    private final FeatureService featureService;

    /** Datos de entrada para crear / editar una cancha (HU3, HU12, HU17). */
    public record CourtInput(String name, String description, Long categoryId, Integer capacity,
                             Set<Long> featureIds, String imageUrl) {}

    public Court createCourt(CourtInput in) {
        validate(in);
        if (courtRepository.existsByName(in.name().trim())) {
            throw new ConflictException("Ya existe una cancha con ese nombre");
        }
        Court court = new Court();
        apply(court, in);
        return courtRepository.save(court);
    }

    /** HU12 — la categoría y las características se pueden modificar al editar la cancha. */
    public Court updateCourt(Long id, CourtInput in) {
        validate(in);
        Court court = courtRepository.findById(id).orElseThrow(() -> new NotFoundException("La cancha no existe"));
        if (courtRepository.existsByNameAndIdNot(in.name().trim(), id)) {
            throw new ConflictException("Ya existe una cancha con ese nombre");
        }
        apply(court, in);
        return courtRepository.save(court);
    }

    private void validate(CourtInput in) {
        if (in.name() == null || in.name().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (in.description() == null || in.description().isBlank()) throw new IllegalArgumentException("La descripción es obligatoria");
        if (in.categoryId() == null) throw new IllegalArgumentException("La categoría deportiva es obligatoria");
        if (in.capacity() == null || in.capacity() < 1) throw new IllegalArgumentException("La capacidad debe ser mayor a 0");
    }

    private void apply(Court court, CourtInput in) {
        Category category = categoryService.getById(in.categoryId());
        court.setName(in.name().trim());
        court.setDescription(in.description().trim());
        court.setCategory(category);
        court.setCapacity(in.capacity());
        if (in.imageUrl() != null) court.setImageUrl(in.imageUrl());
        Set<Long> ids = in.featureIds() == null ? Set.of() : in.featureIds();
        court.setFeatures(ids.isEmpty() ? new LinkedHashSet<>() : featureService.findAllByIds(ids));
    }

    /**
     * HU4 / HU8 / HU20 — Listado paginado.
     * random=true devuelve el orden aleatorio en memoria (evita RAND()/RANDOM() específico de dialecto).
     * categoryIds (opcional) filtra por una o varias categorías.
     */
    public Page<Court> getAllCourts(Pageable pageable, boolean random, Set<Long> categoryIds) {
        boolean filtered = categoryIds != null && !categoryIds.isEmpty();
        if (random) {
            List<Court> all = filtered ? courtRepository.findByCategoryIdIn(categoryIds) : courtRepository.findAll();
            Collections.shuffle(all);
            int start = (int) Math.min(pageable.getOffset(), all.size());
            int end = Math.min(start + pageable.getPageSize(), all.size());
            return new PageImpl<>(all.subList(start, end), pageable, all.size());
        }
        return filtered ? courtRepository.findByCategoryIdIn(categoryIds, pageable) : courtRepository.findAll(pageable);
    }

    public Page<Court> getAllCourts(Pageable pageable, boolean random) {
        return getAllCourts(pageable, random, null);
    }

    public Page<Court> getAllCourts(Pageable pageable) {
        return courtRepository.findAll(pageable);
    }

    public Optional<Court> getCourtById(Long id) {
        return courtRepository.findById(id);
    }

    public long count() {
        return courtRepository.count();
    }

    public void deleteCourt(Long id) {
        if (!courtRepository.existsById(id)) {
            throw new IllegalArgumentException("La cancha no existe");
        }
        courtRepository.deleteById(id);
    }
}
