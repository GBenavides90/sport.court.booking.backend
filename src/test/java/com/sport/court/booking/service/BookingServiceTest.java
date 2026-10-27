package com.sport.court.booking.service;

import com.sport.court.booking.domain.Booking;
import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.BookingRequestDto;
import com.sport.court.booking.dto.BookingResponseDto;
import com.sport.court.booking.exception.ConflictException;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.BookingRepository;
import com.sport.court.booking.repository.CourtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Court testCourt;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Giuseppe");
        testUser.setLastName("Benavides");
        testUser.setEmail("giuseppe@test.com");

        testCourt = new Court();
        testCourt.setId(10L);
        testCourt.setName("Cancha Central Pádel");
        testCourt.setCapacity(4);
    }

    @Test
    @DisplayName("HU32: Debe crear y confirmar una reserva exitosamente cuando no hay colisiones")
    void shouldCreateBookingSuccessfully() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(7);

        BookingRequestDto request = BookingRequestDto.builder()
                .courtId(10L)
                .startDate(start)
                .endDate(end)
                .startTime("10:00")
                .notes("Balones extra")
                .build();

        when(courtRepository.findById(10L)).thenReturn(Optional.of(testCourt));
        when(bookingRepository.findOverlappingBookings(10L, start, end)).thenReturn(Collections.emptyList());
        when(bookingRepository.existsByBookingCode(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(99L);
            return b;
        });

        BookingResponseDto response = bookingService.createBooking(testUser, request);

        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(10L, response.getCourtId());
        assertEquals("Cancha Central Pádel", response.getCourtName());
        assertEquals(start, response.getStartDate());
        assertEquals(end, response.getEndDate());
        assertNotNull(response.getBookingCode());
        assertTrue(response.getBookingCode().startsWith("SCB-"));

        verify(emailService, times(1)).sendBookingConfirmation(any(Booking.class));
    }

    @Test
    @DisplayName("HU32: Debe lanzar ConflictException si el rango de fechas ya está reservado (concurrencia)")
    void shouldThrowConflictWhenDatesOverlap() {
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = LocalDate.now().plusDays(4);

        BookingRequestDto request = BookingRequestDto.builder()
                .courtId(10L)
                .startDate(start)
                .endDate(end)
                .startTime("09:00")
                .build();

        Booking existingBooking = Booking.builder()
                .id(50L)
                .court(testCourt)
                .startDate(start)
                .endDate(end)
                .build();

        when(courtRepository.findById(10L)).thenReturn(Optional.of(testCourt));
        when(bookingRepository.findOverlappingBookings(10L, start, end)).thenReturn(List.of(existingBooking));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                bookingService.createBooking(testUser, request));

        assertTrue(ex.getMessage().contains("ya tiene una reserva confirmada"));
        verify(bookingRepository, never()).save(any());
        verify(emailService, never()).sendBookingConfirmation(any());
    }

    @Test
    @DisplayName("HU30: Debe rechazar reservas con fecha de fin anterior a la de inicio")
    void shouldThrowWhenStartDateAfterEndDate() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(2);

        BookingRequestDto request = BookingRequestDto.builder()
                .courtId(10L)
                .startDate(start)
                .endDate(end)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking(testUser, request));
    }

    @Test
    @DisplayName("HU30: Debe rechazar reservas en fechas pasadas")
    void shouldThrowWhenDateIsInPast() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now().plusDays(1);

        BookingRequestDto request = BookingRequestDto.builder()
                .courtId(10L)
                .startDate(start)
                .endDate(end)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking(testUser, request));
    }

    @Test
    @DisplayName("HU33: Debe listar el historial de reservas del usuario autenticado")
    void shouldGetUserBookings() {
        Booking b1 = Booking.builder()
                .id(1L)
                .bookingCode("SCB-001")
                .court(testCourt)
                .user(testUser)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(3))
                .status("CONFIRMED")
                .build();

        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(b1));

        List<BookingResponseDto> list = bookingService.getUserBookings(testUser);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("SCB-001", list.get(0).getBookingCode());
    }

    @Test
    @DisplayName("HU23 / HU30: Debe calcular todas las fechas individuales ocupadas para una cancha")
    void shouldGetOccupiedDates() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);

        Booking b = Booking.builder()
                .court(testCourt)
                .startDate(start)
                .endDate(end)
                .status("CONFIRMED")
                .build();

        when(courtRepository.existsById(10L)).thenReturn(true);
        when(bookingRepository.findByCourtIdAndStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(
                eq(10L), eq("CONFIRMED"), any(LocalDate.class))).thenReturn(List.of(b));

        List<String> occupied = bookingService.getOccupiedDates(10L);

        assertEquals(3, occupied.size());
        assertTrue(occupied.contains(start.toString()));
        assertTrue(occupied.contains(start.plusDays(1).toString()));
        assertTrue(occupied.contains(end.toString()));
    }
}
