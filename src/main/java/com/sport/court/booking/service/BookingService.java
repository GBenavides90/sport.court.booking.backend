package com.sport.court.booking.service;

import com.sport.court.booking.domain.Booking;
import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.BookingRequestDto;
import com.sport.court.booking.dto.BookingResponseDto;
import com.sport.court.booking.exception.ConflictException;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.BookingRepository;
import com.sport.court.booking.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final EmailService emailService;

    /**
     * HU30 / HU31 / HU32 — Crear y confirmar una nueva reserva deportiva.
     * Con validación transaccional contra colisiones o concurrencia (double booking).
     */
    @Transactional
    public BookingResponseDto createBooking(User user, BookingRequestDto request) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario debe estar autenticado para realizar una reserva");
        }
        if (request.getCourtId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una cancha deportiva");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("El rango de fechas de la reserva es obligatorio");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No es posible realizar reservas en fechas pasadas");
        }

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new NotFoundException("La cancha deportiva seleccionada no existe"));

        // Verificar solapamiento de fechas
        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                court.getId(), request.getStartDate(), request.getEndDate());

        if (!conflicts.isEmpty()) {
            throw new ConflictException(String.format(
                    "La cancha '%s' ya tiene una reserva confirmada entre el %s y el %s. Por favor selecciona otro rango de fechas.",
                    court.getName(), request.getStartDate(), request.getEndDate()));
        }

        String bookingCode = generateUniqueBookingCode();

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .court(court)
                .user(user)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime() != null && !request.getStartTime().isBlank() ? request.getStartTime().trim() : "08:00")
                .endTime(request.getEndTime() != null && !request.getEndTime().isBlank() ? request.getEndTime().trim() : "10:00")
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Envío de confirmación por correo
        try {
            emailService.sendBookingConfirmation(savedBooking);
        } catch (Exception e) {
            log.error("Error al enviar confirmación de reserva #{}: {}", bookingCode, e.getMessage());
        }

        return BookingResponseDto.fromEntity(savedBooking);
    }

    /**
     * HU33 — Acceder al historial de reservas del usuario autenticado.
     */
    public List<BookingResponseDto> getUserBookings(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(BookingResponseDto::fromEntity)
                .toList();
    }

    /**
     * HU23 / HU30 — Obtener lista consolidada de fechas ocupadas (en formato ISO yyyy-MM-dd) para una cancha.
     */
    public List<String> getOccupiedDates(Long courtId) {
        if (!courtRepository.existsById(courtId)) {
            throw new NotFoundException("La cancha deportiva no existe");
        }

        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findByCourtIdAndStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(
                courtId, "CONFIRMED", today);

        Set<String> occupiedDates = new TreeSet<>();
        for (Booking b : bookings) {
            LocalDate current = b.getStartDate();
            while (!current.isAfter(b.getEndDate())) {
                occupiedDates.add(current.toString());
                current = current.plusDays(1);
            }
        }

        return new ArrayList<>(occupiedDates);
    }

    /**
     * HU30 — Obtener canchas disponibles para un rango de fechas.
     */
    public List<Court> getAvailableCourts(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return courtRepository.findAll();
        }
        List<Long> bookedIds = bookingRepository.findBookedCourtIdsBetween(startDate, endDate);
        if (bookedIds.isEmpty()) {
            return courtRepository.findAll();
        }
        return courtRepository.findAll().stream()
                .filter(c -> !bookedIds.contains(c.getId()))
                .toList();
    }

    /**
     * Consultar detalle de una reserva específica validando pertenencia o rol ADMIN.
     */
    public BookingResponseDto getBookingById(Long id, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La reserva no existe"));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("No tienes permisos para visualizar esta reserva");
        }
        return BookingResponseDto.fromEntity(booking);
    }

    private String generateUniqueBookingCode() {
        int randomPart = 1000 + new Random().nextInt(9000);
        long timePart = System.currentTimeMillis() % 100000;
        String code = "SCB-" + randomPart + "-" + timePart;
        while (bookingRepository.existsByBookingCode(code)) {
            randomPart = 1000 + new Random().nextInt(9000);
            timePart = System.currentTimeMillis() % 100000;
            code = "SCB-" + randomPart + "-" + timePart;
        }
        return code;
    }
}
