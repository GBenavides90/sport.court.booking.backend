package com.sport.court.booking.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @JsonAlias({"rating", "score", "stars"})
        @NotNull(message = "La puntuación es obligatoria")
        @Min(value = 1, message = "La puntuación mínima es 1 estrella")
        @Max(value = 5, message = "La puntuación máxima es 5 estrellas")
        Integer score,
        String comment
) {}
