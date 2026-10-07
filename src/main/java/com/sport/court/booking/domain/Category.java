package com.sport.court.booking.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** HU12 / HU21 — Categoría (tipo de deporte) con título, descripción e imagen representativa. */
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Column(unique = true, nullable = false)
    private String title;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(length = 500)
    private String description;

    private String imageUrl;
}
