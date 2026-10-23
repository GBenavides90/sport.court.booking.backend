package com.sport.court.booking.controller;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Court;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU24 / HU25 — Casos de prueba de Favoritos (TC-14..24). */
class FavoriteControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("TC-17 Intento de marcar favorito sin autenticación responde 401")
    void tc17_unauthenticated() throws Exception {
        Category cat = category("Tenis");
        Court c = court("Cancha Tenis 1", cat);

        mvc.perform(post("/api/favorites/" + c.getId() + "/toggle"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-15 / TC-16 / TC-20 Marcar y desmarcar cancha como favorita con un clic")
    void tc15_toggleAndListFavorites() throws Exception {
        String token = userToken();
        Category cat = category("Pádel");
        Court c1 = court("Pádel Central", cat);
        Court c2 = court("Pádel Cristal", cat);

        // Marcar c1 como favorita (TC-15)
        mvc.perform(post("/api/favorites/" + c1.getId() + "/toggle").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(true));

        // Listar favoritos (TC-20)
        mvc.perform(get("/api/favorites").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Pádel Central"));

        // Listar IDs de favoritos
        mvc.perform(get("/api/favorites/ids").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(c1.getId()));

        // Desmarcar c1 como favorita (TC-16)
        mvc.perform(post("/api/favorites/" + c1.getId() + "/toggle").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(false));

        // Lista queda vacía
        mvc.perform(get("/api/favorites").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("TC-21 Eliminar favorito directamente con endpoint DELETE")
    void tc21_deleteFavorite() throws Exception {
        String token = userToken();
        Category cat = category("Squash");
        Court c = court("Squash Pro", cat);

        mvc.perform(post("/api/favorites/" + c.getId()).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/favorites/" + c.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(false));

        mvc.perform(get("/api/favorites").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
