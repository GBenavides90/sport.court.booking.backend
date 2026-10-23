package com.sport.court.booking.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

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

    /** HU12 — categoría / tipo de deporte (entidad Category). Requerida en creación/edición vía servicio. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @NotFound(action = NotFoundAction.IGNORE)
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

    /** HU28 — Puntuación media dinámica y total de valoraciones */
    @Column(nullable = false)
    private Double averageRating = 5.0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    /** HU26 — Bloque de políticas del producto (columnas con título y descripción) */
    @Column(length = 2000)
    private String rules = "Puntualidad en el inicio y término del turno de juego. Respetar al personal y a otros jugadores. Prohibido fumar, ingresar con mascotas o consumir bebidas alcohólicas dentro de la cancha.";

    @Column(length = 2000)
    private String cancellationPolicy = "Cancelaciones permitidas sin costo hasta 24 horas antes del horario reservado. Las solicitudes posteriores o no presentarse están sujetas a cobro del 100%. Reprogramaciones sujetas a disponibilidad.";

    @Column(length = 2000)
    private String healthPolicy = "Uso obligatorio de calzado deportivo adecuado a la superficie de la cancha. Contar con hidratación personal. El complejo dispone de botiquín de primeros auxilios y protocolo de emergencias.";
}
