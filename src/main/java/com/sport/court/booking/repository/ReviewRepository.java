package com.sport.court.booking.repository;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Review;
import com.sport.court.booking.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCourtOrderByCreatedAtDesc(Court court);

    Optional<Review> findByCourtAndUser(Court court, User user);

    boolean existsByCourtAndUser(Court court, User user);

    @Query("SELECT AVG(r.score) FROM Review r WHERE r.court.id = :courtId")
    Double getAverageScoreByCourtId(@Param("courtId") Long courtId);

    long countByCourtId(Long courtId);
}
