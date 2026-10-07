package com.sport.court.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** HU13 — datos de registro con reglas de validación. */
public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        @Pattern(regexp = "^[\\p{L}][\\p{L} '\\-]*$", message = "El nombre solo puede contener letras")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
        @Pattern(regexp = "^[\\p{L}][\\p{L} '\\-]*$", message = "El apellido solo puede contener letras")
        String lastName,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$", message = "El correo electrónico no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$",
                message = "La contraseña debe tener entre 8 y 64 caracteres, con mayúscula, minúscula y número")
        String password
) {}
