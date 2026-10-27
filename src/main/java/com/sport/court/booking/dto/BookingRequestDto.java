package com.sport.court.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDto {

    @NotNull(message = "El identificador de la cancha es obligatorio")
    private Long courtId;

    @NotNull(message = "La fecha de inicio de reserva es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de finalización de reserva es obligatoria")
    private LocalDate endDate;

    private String startTime;

    private String endTime;

    private String notes;
}
