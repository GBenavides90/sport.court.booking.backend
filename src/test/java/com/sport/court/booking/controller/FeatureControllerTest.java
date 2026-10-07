package com.sport.court.booking.controller;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Court;
import com.sport.court.booking.domain.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU17 (TC-22..26) y HU18 (TC-27..29: datos que alimentan el bloque Características). */
class FeatureControllerTest extends AbstractIntegrationTest {

    private static String json(String name, String icon) {
        return "{\"name\":\"" + name + "\",\"icon\":\"" + icon + "\"}";
    }

    @Test
    @DisplayName("TC-22 Las operaciones de administración de características requieren rol ADMIN")
    void tc22_adminOnly() throws Exception {
        mvc.perform(post("/api/features").contentType(MediaType.APPLICATION_JSON).content(json("Wi-Fi", "wifi")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/features").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json("Wi-Fi", "wifi")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-23 Agregar una nueva característica con nombre e ícono")
    void tc23_create() throws Exception {
        mvc.perform(post("/api/features").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json("Wi-Fi", "wifi")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Wi-Fi"))
                .andExpect(jsonPath("$.icon").value("wifi"));
        assertThat(featureRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-23b Validaciones: nombre vacío, ícono inválido y nombre duplicado")
    void tc23b_validations() throws Exception {
        String admin = adminToken();
        mvc.perform(post("/api/features").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json("", "wifi")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/features").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json("Piscina", "no-existe")))
                .andExpect(status().isBadRequest());
        feature("Wi-Fi", "wifi");
        mvc.perform(post("/api/features").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json("wi-fi", "wifi")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-24 Editar una característica existente")
    void tc24_update() throws Exception {
        Feature f = feature("Wi-Fi", "wifi");
        mvc.perform(put("/api/features/{id}", f.getId()).header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json("Internet", "tv")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Internet"))
                .andExpect(jsonPath("$.icon").value("tv"));
    }

    @Test
    @DisplayName("TC-25 Eliminar una característica la desasocia de las canchas")
    void tc25_delete() throws Exception {
        Feature wifi = feature("Wi-Fi", "wifi");
        Feature lights = feature("Iluminación", "lighting");
        Court court = court("Cancha 1", category("Fútbol"), wifi, lights);

        mvc.perform(delete("/api/features/{id}", wifi.getId()).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());

        assertThat(featureRepository.findById(wifi.getId())).isEmpty();
        Court reloaded = courtRepository.findById(court.getId()).orElseThrow();
        assertThat(reloaded.getFeatures()).extracting(Feature::getName).containsExactly("Iluminación");
    }

    @Test
    @DisplayName("TC-25b Eliminar una característica inexistente responde 404")
    void tc25b_deleteUnknown() throws Exception {
        mvc.perform(delete("/api/features/{id}", 9999).header("Authorization", adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-26 Asociar una o más características a una cancha (crear y editar)")
    void tc26_associateToCourt() throws Exception {
        String admin = adminToken();
        Category cat = category("Fútbol");
        Feature wifi = feature("Wi-Fi", "wifi");
        Feature shower = feature("Duchas", "shower");

        var created = mvc.perform(multipart("/api/courts").header("Authorization", admin)
                        .param("name", "Cancha Central").param("description", "Desc")
                        .param("categoryId", cat.getId().toString()).param("capacity", "12")
                        .param("featureIds", wifi.getId().toString(), shower.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.features", hasSize(2)))
                .andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(multipart("/api/courts/{id}", id.longValue()).with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", admin)
                        .param("name", "Cancha Central").param("description", "Desc")
                        .param("categoryId", cat.getId().toString()).param("capacity", "12")
                        .param("featureIds", shower.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features", hasSize(1)))
                .andExpect(jsonPath("$.features[0].name").value("Duchas"));
    }

    @Test
    @DisplayName("TC-27/28/29 El detalle de la cancha expone todas las características con su ícono")
    void tc27_28_29_detailShowsFeatures() throws Exception {
        Court court = court("Cancha 1", category("Fútbol"),
                feature("Wi-Fi", "wifi"), feature("Duchas", "shower"), feature("Estacionamiento", "parking"));

        mvc.perform(get("/api/courts/{id}", court.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features", hasSize(3)))
                .andExpect(jsonPath("$.features[*].icon", hasItem("wifi")))
                .andExpect(jsonPath("$.features[*].icon", hasItem("shower")))
                .andExpect(jsonPath("$.features[*].icon", hasItem("parking")))
                .andExpect(jsonPath("$.features[*].name", hasItem("Duchas")));
    }

    @Test
    @DisplayName("Listado público de características e íconos disponibles")
    void publicListAndIcons() throws Exception {
        feature("Wi-Fi", "wifi");
        mvc.perform(get("/api/features")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/features/icons")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("wifi")));
    }
}
