package com.sport.court.booking.dto;

import com.sport.court.booking.domain.Role;
import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull(message = "El rol es obligatorio") Role role) {}
