package com.sport.court.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailRequest(@NotBlank(message = "El correo electrónico es obligatorio") String email) {}
