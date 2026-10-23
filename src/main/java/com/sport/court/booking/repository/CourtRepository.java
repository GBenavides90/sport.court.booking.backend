package com.sport.court.booking.repository;

import com.sport.court.booking.domain.Court;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /** HU20 — filtrado por una o varias categorías. */
    Page<Court> findByCategoryIdIn(Collection<Long> categoryIds, Pageable pageable);

    List<Court> findByCategoryIdIn(Collection<Long> categoryIds);

    boolean existsByCategoryId(Long categoryId);

    List<Court> findByCategoryId(Long categoryId);

    List<Court> findByFeaturesId(Long featureId);

    /** HU22 — búsqueda de canchas por nombre, descripción o categoría */
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Court c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "(c.category IS NOT NULL AND LOWER(c.category.title) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Court> searchByQuery(@org.springframework.data.repository.query.Param("q") String q);

    /** HU22 — sugerencias interactivas para autocompletar */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c.name FROM Court c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<String> findNamesByQuery(@org.springframework.data.repository.query.Param("q") String q);
}
