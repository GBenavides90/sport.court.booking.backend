package com.sport.court.booking.repository;

import com.sport.court.booking.domain.Court;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    boolean existsByName(String name);

    /** HU4 — Retorna canchas en orden verdaderamente aleatorio (ORDER BY RAND()) */
    @Query("SELECT c FROM Court c ORDER BY RAND()")
    Page<Court> findAllRandom(Pageable pageable);
}
