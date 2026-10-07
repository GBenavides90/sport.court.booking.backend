package com.sport.court.booking.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** HU17 / HU18 — Característica de una cancha, con nombre e ícono asociado (clave de ícono). */
@Entity
@Table(name = "features")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(unique = true, nullable = false)
    private String name;

    @NotBlank(message = "El ícono es obligatorio")
    @Column(nullable = false)
    private String icon;
}
