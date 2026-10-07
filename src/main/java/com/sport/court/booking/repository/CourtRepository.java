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

    List<Court> findByFeaturesId(Long featureId);
}
