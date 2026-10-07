package com.sport.court.booking.controller;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU13 (TC-05..09), HU14 (TC-10..13) y HU15 (TC-15..17). */
class AuthControllerTest extends AbstractIntegrationTest {

    private static String registerJson(String first, String last, String email, String password) {
        return """
                {"firstName":"%s","lastName":"%s","email":"%s","password":"%s"}
                """.formatted(first, last, email, password);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private org.springframework.test.web.servlet.ResultActions register(String body) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ───────────── HU13 — Registrar usuario ─────────────

    @Test
    @DisplayName("TC-05 Registrar usuario con datos válidos")
    void tc05_registerValid() throws Exception {
        register(registerJson("María", "González", "maria@test.com", "Segura123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@test.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmailIgnoreCase("maria@test.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("Segura123").startsWith("$2"); // hash BCrypt
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("TC-06 Registrar usuario con campos obligatorios vacíos")
    void tc06_registerEmptyFields() throws Exception {
        register(registerJson("", "", "", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("TC-07 Registrar usuario con correo inválido")
    void tc07_registerInvalidEmail() throws Exception {
        for (String bad : new String[]{"sin-arroba", "a@b", "a b@c.com", "@dominio.com"}) {
            register(registerJson("María", "González", bad, "Segura123"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").value("El correo electrónico no es válido"));
        }
    }

    @Test
    @DisplayName("TC-08 Registrar usuario con correo ya existente")
    void tc08_registerDuplicateEmail() throws Exception {
        createUser("repetido@test.com", Role.USER);
        register(registerJson("Luis", "Mora", "REPETIDO@test.com", "Segura123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("correo")));
    }

    @Test
    @DisplayName("TC-09 Registrar usuario con contraseña que no cumple las reglas")
    void tc09_registerWeakPassword() throws Exception {
        for (String weak : new String[]{"corta1A", "sinmayuscula1", "SINMINUSCULA1", "SinNumeros"}) {
            register(registerJson("María", "González", "maria@test.com", weak))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").exists());
        }
        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("TC-06b Nombre y apellido rechazan números y símbolos")
    void tc06b_registerInvalidNames() throws Exception {
        register(registerJson("M4ría", "G@nzález", "maria@test.com", "Segura123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists());
    }

    // ───────────── HU14 — Identificar usuario ─────────────

    @Test
    @DisplayName("TC-10 Iniciar sesión con credenciales válidas")
    void tc10_loginValid() throws Exception {
        createUser("ana@test.com", Role.USER);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ana@test.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.email").value("ana@test.com"))
                .andExpect(jsonPath("$.user.firstName").value("Ana"));
    }

    @Test
    @DisplayName("TC-11 Iniciar sesión con contraseña incorrecta")
    void tc11_loginWrongPassword() throws Exception {
        createUser("ana@test.com", Role.USER);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ana@test.com", "Incorrecta1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo electrónico o contraseña incorrectos"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("TC-12 Iniciar sesión con correo no registrado")
    void tc12_loginUnknownEmail() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nadie@test.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo electrónico o contraseña incorrectos"));
    }

    @Test
    @DisplayName("TC-13 Usuario autenticado obtiene su nombre e información (/me)")
    void tc13_meReturnsIdentity() throws Exception {
        User user = createUser("ana@test.com", Role.USER);
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.email").value("ana@test.com"));
    }

    // ───────────── HU15 — Cerrar sesión ─────────────

    @Test
    @DisplayName("TC-15/16/17 Logout revoca el token, bloquea lo protegido y el sitio sigue disponible como anónimo")
    void tc15_16_17_logoutFlow() throws Exception {
        createUser("ana@test.com", Role.USER);
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ana@test.com", PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.token");
        String header = "Bearer " + token;

        mvc.perform(get("/api/auth/me").header("Authorization", header)).andExpect(status().isOk());

        // TC-15: cerrar sesión
        mvc.perform(post("/api/auth/logout").header("Authorization", header)).andExpect(status().isNoContent());

        // TC-16: el mismo token ya no sirve para funcionalidades protegidas
        mvc.perform(get("/api/auth/me").header("Authorization", header)).andExpect(status().isUnauthorized());

        // TC-17: se puede seguir navegando el catálogo público como anónimo
        mvc.perform(get("/api/courts")).andExpect(status().isOk());
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-16b Sin token o con token inválido el acceso protegido responde 401")
    void tc16b_protectedWithoutToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer token.invalido.xyz"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }
}
