package com.sport.court.booking.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(unique = true, nullable = false)
    private String name;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(length = 1000)
    private String description;

    @NotBlank(message = "La categoría deportiva es obligatoria")
    private String category; // e.g. futbol, basquet, tenis

    @NotNull(message = "La capacidad es obligatoria")
    private Integer capacity;

    private String imageUrl;
}
