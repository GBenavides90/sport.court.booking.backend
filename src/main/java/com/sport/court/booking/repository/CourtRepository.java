package com.sport.court.booking.repository;

import com.sport.court.booking.domain.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {
    boolean existsByName(String name);
}
