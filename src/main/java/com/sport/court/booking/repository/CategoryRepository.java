package com.sport.court.booking.repository;

import com.sport.court.booking.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByTitleIgnoreCase(String title);
    Optional<Category> findByTitleIgnoreCase(String title);
}
