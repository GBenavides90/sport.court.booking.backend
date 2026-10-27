package com.sport.court.booking.controller;

import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.BookingRequestDto;
import com.sport.court.booking.dto.BookingResponseDto;
import com.sport.court.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Endpoints de gestión y realización de reservas (Sprint 4: HU30, HU31, HU32, HU33, HU35)")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Realizar y confirmar una nueva reserva (HU30, HU31, HU32)")
    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookingRequestDto request) {
        BookingResponseDto response = bookingService.createBooking(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar el historial de reservas del usuario autenticado (HU33)")
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getUserBookings(user));
    }

    @Operation(summary = "Consultar fechas ocupadas de una cancha específica (HU23, HU30)")
    @GetMapping("/court/{courtId}/occupied-dates")
    public ResponseEntity<Map<String, Object>> getOccupiedDates(@PathVariable Long courtId) {
        List<String> occupiedDates = bookingService.getOccupiedDates(courtId);
        return ResponseEntity.ok(Map.of("courtId", courtId, "occupiedDates", occupiedDates));
    }

    @Operation(summary = "Consultar canchas disponibles para un rango de fechas (HU30)")
    @GetMapping("/available")
    public ResponseEntity<List<Court>> getAvailableCourts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(bookingService.getAvailableCourts(startDate, endDate));
    }

    @Operation(summary = "Obtener el detalle de una reserva por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getBookingById(id, user));
    }
}
