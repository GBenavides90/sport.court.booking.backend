package com.sport.court.booking.controller;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU16 — Identificar administrador (TC-18..21). */
class AdminUserControllerTest extends AbstractIntegrationTest {

    private static final String ADMIN_JSON = "{\"role\":\"ADMIN\"}";
    private static final String USER_JSON = "{\"role\":\"USER\"}";

    @Test
    @DisplayName("TC-18 Asignar permiso de administrador a un usuario")
    void tc18_grantAdmin() throws Exception {
        String admin = adminToken();
        User target = createUser("nuevo@test.com", Role.USER);

        mvc.perform(put("/api/admin/users/{id}/role", target.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // El permiso es efectivo de inmediato, incluso con un token emitido antes del cambio
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(target)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-19 Retirar permiso de administrador a un usuario")
    void tc19_revokeAdmin() throws Exception {
        String admin = adminToken();
        User other = createUser("otro@test.com", Role.ADMIN);
        String otherToken = bearer(other);

        mvc.perform(put("/api/admin/users/{id}/role", other.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(USER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        mvc.perform(get("/api/admin/users").header("Authorization", otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-19b Un administrador no puede retirarse su propio permiso")
    void tc19b_cannotDemoteSelf() throws Exception {
        User admin = createUser("admin@test.com", Role.ADMIN);
        mvc.perform(put("/api/admin/users/{id}/role", admin.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(USER_JSON))
                .andExpect(status().isConflict());
        assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("TC-20 Usuario sin permiso intenta acceder a administración")
    void tc20_userDenied() throws Exception {
        User target = createUser("objetivo@test.com", Role.USER);
        String token = userToken();
        mvc.perform(get("/api/admin/users").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(put("/api/admin/users/{id}/role", target.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_JSON))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findById(target.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("TC-21 Administrador accede a las secciones administrativas y ve el listado de usuarios")
    void tc21_adminAccess() throws Exception {
        String admin = adminToken();
        createUser("uno@test.com", Role.USER);
        mvc.perform(get("/api/admin/users").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @DisplayName("TC-20b Usuario inexistente responde 404")
    void tc20b_unknownUser() throws Exception {
        mvc.perform(put("/api/admin/users/{id}/role", 999999).header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_JSON))
                .andExpect(status().isNotFound());
    }
}
