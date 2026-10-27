package com.sport.court.booking.controller;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Court;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU28 — Casos de prueba de Puntuación y Reseñas (TC-36..42). */
class ReviewControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("TC-38 Intento de puntuar sin autenticación responde 401")
    void tc38_unauthenticated() throws Exception {
        Category cat = category("Fútbol");
        Court c = court("Fútbol 5 Camp Nou", cat);

        mvc.perform(post("/api/courts/" + c.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5,\"comment\":\"Excelente cancha\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-39 / TC-40 / TC-41 Puntuar cancha y recálculo automático de la puntuación media")
    void tc40_addReviewAndRecalculate() throws Exception {
        String token = userToken();
        Category cat = category("Tenis");
        Court c = court("Cancha Roland Garros", cat);

        // Calificar con 4 estrellas (TC-39, TC-40)
        mvc.perform(post("/api/courts/" + c.getId() + "/reviews")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":4,\"comment\":\"Muy buen estado del césped\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(4))
                .andExpect(jsonPath("$.comment").value("Muy buen estado del césped"))
                .andExpect(jsonPath("$.userName").exists());

        // Verificar listado público de reseñas
        mvc.perform(get("/api/courts/" + c.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].score").value(4));

        // Puntuación media en tiempo real (TC-41)
        Court updated = courtRepository.findById(c.getId()).orElseThrow();
        assertThat(updated.getAverageRating()).isEqualTo(4.0);
        assertThat(updated.getReviewCount()).isEqualTo(1);

        // Resumen público (TC-42)
        mvc.perform(get("/api/courts/" + c.getId() + "/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
    }

    @Test
    @DisplayName("TC-39b Validación de puntuación fuera de rango (menor a 1 o mayor a 5)")
    void tc39b_invalidScore() throws Exception {
        String token = userToken();
        Category cat = category("Pádel");
        Court c = court("Pádel Star", cat);

        mvc.perform(post("/api/courts/" + c.getId() + "/reviews")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":6,\"comment\":\"Invalido\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/courts/" + c.getId() + "/reviews")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":0,\"comment\":\"Invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-43 Validar aceptación de alias rating y score en reseñas")
    void fix28_ratingAliasAccepted() throws Exception {
        String token = userToken();
        Category cat = category("Fútbol");
        Court c = court("Cancha Maracaná", cat);

        mvc.perform(post("/api/courts/" + c.getId() + "/reviews")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Excelente experiencia y servicio\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente experiencia y servicio"));
    }
}
