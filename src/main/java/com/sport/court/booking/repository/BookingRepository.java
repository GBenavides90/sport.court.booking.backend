package com.sport.court.booking.repository;

import com.sport.court.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** HU32 — Validación para evitar colisiones o reservas concurrentes (double booking) */
    @Query("SELECT b FROM Booking b WHERE b.court.id = :courtId " +
            "AND b.status = 'CONFIRMED' " +
            "AND b.startDate <= :endDate " +
            "AND b.endDate >= :startDate")
    List<Booking> findOverlappingBookings(
            @Param("courtId") Long courtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** HU23 / HU30 — Obtener reservas confirmadas de una cancha */
    List<Booking> findByCourtIdAndStatusOrderByStartDateAsc(Long courtId, String status);

    /** HU23 — Obtener reservas futuras o vigentes de una cancha */
    List<Booking> findByCourtIdAndStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Long courtId, String status, LocalDate date);

    /** HU33 — Historial de reservas del usuario autenticado ordenado cronológicamente */
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByUserIdOrderByStartDateDesc(Long userId);

    /** HU30 — Identificar IDs de canchas con reservas colisionantes en un rango de fechas */
    @Query("SELECT DISTINCT b.court.id FROM Booking b " +
            "WHERE b.status = 'CONFIRMED' " +
            "AND b.startDate <= :endDate " +
            "AND b.endDate >= :startDate")
    List<Long> findBookedCourtIdsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    boolean existsByCourtIdAndUserIdAndStatus(Long courtId, Long userId, String status);
}
