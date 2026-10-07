package com.sport.court.booking.controller;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU21 — Agregar categoría (TC-42..44). */
class CategoryControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("TC-42 Solo el administrador accede a Agregar categoría")
    void tc42_adminOnly() throws Exception {
        mvc.perform(multipart("/api/categories").param("title", "Vóley").param("description", "Playa"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/categories").header("Authorization", userToken())
                        .param("title", "Vóley").param("description", "Playa"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-43 Crear categoría con título, descripción e imagen")
    void tc43_createWithImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "voley.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/categories").file(image).header("Authorization", adminToken())
                        .param("title", "Vóley").param("description", "Canchas de vóley de arena"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Vóley"))
                .andExpect(jsonPath("$.description").value("Canchas de vóley de arena"))
                .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/uploads/")));

        // La categoría queda disponible para organizar canchas y para el filtro público
        assertThat(categoryRepository.existsByTitleIgnoreCase("vóley")).isTrue();
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("TC-43b Crear categoría sin imagen es válido (la imagen es opcional en el servidor)")
    void tc43b_createWithoutImage() throws Exception {
        mvc.perform(multipart("/api/categories").header("Authorization", adminToken())
                        .param("title", "Rugby").param("description", "Canchas de rugby"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }

    @Test
    @DisplayName("TC-44 Crear categoría sin datos obligatorios muestra validaciones")
    void tc44_missingFields() throws Exception {
        String admin = adminToken();
        mvc.perform(multipart("/api/categories").header("Authorization", admin).param("description", "Sin título"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El título es obligatorio"));
        mvc.perform(multipart("/api/categories").header("Authorization", admin).param("title", "Sin descripción"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La descripción es obligatoria"));
        mvc.perform(multipart("/api/categories").header("Authorization", admin)
                        .param("title", "   ").param("description", "x"))
                .andExpect(status().isBadRequest());
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    @DisplayName("TC-44b Título duplicado responde 409")
    void tc44b_duplicate() throws Exception {
        Category existing = category("Fútbol");
        mvc.perform(multipart("/api/categories").header("Authorization", adminToken())
                        .param("title", existing.getTitle().toUpperCase()).param("description", "otra"))
                .andExpect(status().isConflict());
    }
}
