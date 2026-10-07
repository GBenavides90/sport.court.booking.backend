package com.sport.court.booking.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

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

    /** HU12 — categoría / tipo de deporte (entidad Category). */
    @NotNull(message = "La categoría deportiva es obligatoria")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull(message = "La capacidad es obligatoria")
    private Integer capacity;

    private String imageUrl;

    /** HU17 / HU18 — características asociadas a la cancha. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "court_features",
            joinColumns = @JoinColumn(name = "court_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id"))
    private Set<Feature> features = new LinkedHashSet<>();
}
