package com.sport.court.booking.repository;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Favorite;
import com.sport.court.booking.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserOrderByCreatedAtDesc(User user);

    Optional<Favorite> findByUserAndCourt(User user, Court court);

    boolean existsByUserAndCourt(User user, Court court);

    void deleteByUserAndCourt(User user, Court court);

    @Query("SELECT f.court.id FROM Favorite f WHERE f.user.id = :userId")
    List<Long> findCourtIdsByUserId(@Param("userId") Long userId);
}
