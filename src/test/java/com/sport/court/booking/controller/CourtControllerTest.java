package com.sport.court.booking.controller;

import com.sport.court.booking.AbstractIntegrationTest;
import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Court;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU12 (TC-01..04) y HU20 (TC-36..41, lado servidor). */
class CourtControllerTest extends AbstractIntegrationTest {

    // ───────────── HU12 — Categorizar cancha ─────────────

    @Test
    @DisplayName("TC-01 Asignar una categoría a una cancha ya creada (editar)")
    void tc01_assignToExisting() throws Exception {
        Category futbol = category("Fútbol");
        Category tenis = category("Tenis");
        Court court = court("Cancha 1", futbol);

        mvc.perform(multipart("/api/courts/{id}", court.getId()).with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", adminToken())
                        .param("name", "Cancha 1").param("description", "Desc")
                        .param("categoryId", tenis.getId().toString()).param("capacity", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.title").value("Tenis"));
        assertThat(courtRepository.findById(court.getId()).orElseThrow().getCategory().getTitle()).isEqualTo("Tenis");
    }

    @Test
    @DisplayName("TC-02 Asignar categoría al registrar una cancha")
    void tc02_assignOnCreate() throws Exception {
        Category futbol = category("Fútbol");
        mvc.perform(multipart("/api/courts").header("Authorization", adminToken())
                        .param("name", "Cancha Nueva").param("description", "Desc")
                        .param("categoryId", futbol.getId().toString()).param("capacity", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category.id").value(futbol.getId().intValue()))
                .andExpect(jsonPath("$.category.title").value("Fútbol"));
    }

    @Test
    @DisplayName("TC-02b Registrar cancha sin categoría o con categoría inexistente es rechazado")
    void tc02b_requiresValidCategory() throws Exception {
        String admin = adminToken();
        mvc.perform(multipart("/api/courts").header("Authorization", admin)
                        .param("name", "X").param("description", "Desc").param("capacity", "10"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/courts").header("Authorization", admin)
                        .param("name", "X").param("description", "Desc")
                        .param("categoryId", "99999").param("capacity", "10"))
                .andExpect(status().isNotFound());
        assertThat(courtRepository.count()).isZero();
    }

    @Test
    @DisplayName("TC-03 Editar la categoría de una cancha desde Editar cancha (y validar nombre duplicado)")
    void tc03_editCategoryAndDuplicateName() throws Exception {
        Category futbol = category("Fútbol");
        Category padel = category("Pádel");
        Court a = court("Cancha A", futbol);
        court("Cancha B", futbol);

        mvc.perform(multipart("/api/courts/{id}", a.getId()).with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", adminToken())
                        .param("name", "Cancha B").param("description", "Desc")
                        .param("categoryId", padel.getId().toString()).param("capacity", "4"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-04 Consultar una cancha muestra su categoría")
    void tc04_getShowsCategory() throws Exception {
        Court court = court("Cancha 1", category("Básquet"));
        mvc.perform(get("/api/courts/{id}", court.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.title").value("Básquet"))
                .andExpect(jsonPath("$.category.description").exists());
    }

    @Test
    @DisplayName("Crear / editar / eliminar canchas requiere ADMIN")
    void courtWritesRequireAdmin() throws Exception {
        Category futbol = category("Fútbol");
        Court court = court("Cancha 1", futbol);
        mvc.perform(multipart("/api/courts").param("name", "N").param("description", "D")
                        .param("categoryId", futbol.getId().toString()).param("capacity", "1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/courts/{id}", court.getId()).header("Authorization", userToken()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/courts/{id}", court.getId()).header("Authorization", adminToken()))
                .andExpect(status().isOk());
        assertThat(courtRepository.findById(court.getId())).isEmpty();
    }

    // ───────────── HU20 — Filtrar por categoría ─────────────

    private void seedCourts() {
        Category futbol = category("Fútbol");
        Category tenis = category("Tenis");
        Category padel = category("Pádel");
        court("F1", futbol); court("F2", futbol); court("F3", futbol);
        court("T1", tenis); court("T2", tenis);
        court("P1", padel);
    }

    @Test
    @DisplayName("TC-36 La lista de categorías está disponible públicamente para el filtro")
    void tc36_categoriesList() throws Exception {
        seedCourts();
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].title", hasItems("Fútbol", "Tenis", "Pádel")));
    }

    @Test
    @DisplayName("TC-37 Filtrar por una categoría devuelve solo canchas de esa categoría")
    void tc37_filterOne() throws Exception {
        seedCourts();
        Long tenisId = categoryRepository.findByTitleIgnoreCase("Tenis").orElseThrow().getId();
        mvc.perform(get("/api/courts").param("categoryIds", tenisId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].category.title", everyItem(is("Tenis"))));
    }

    @Test
    @DisplayName("TC-38 Filtrar por varias categorías devuelve la unión (también en modo aleatorio)")
    void tc38_filterMany() throws Exception {
        seedCourts();
        Long tenis = categoryRepository.findByTitleIgnoreCase("Tenis").orElseThrow().getId();
        Long padel = categoryRepository.findByTitleIgnoreCase("Pádel").orElseThrow().getId();
        for (String random : new String[]{"false", "true"}) {
            mvc.perform(get("/api/courts").param("categoryIds", tenis.toString(), padel.toString())
                            .param("random", random))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.content[*].category.title", not(hasItem("Fútbol"))));
        }
    }

    @Test
    @DisplayName("TC-39 La cantidad de resultados del filtro y el total sin filtros están disponibles")
    void tc39_counts() throws Exception {
        seedCourts();
        Long futbol = categoryRepository.findByTitleIgnoreCase("Fútbol").orElseThrow().getId();
        mvc.perform(get("/api/courts").param("categoryIds", futbol.toString()))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/courts/count")).andExpect(jsonPath("$.total").value(6));
    }

    @Test
    @DisplayName("TC-40 Sin filtros se restaura el listado original")
    void tc40_clearFilters() throws Exception {
        seedCourts();
        mvc.perform(get("/api/courts").param("size", "100"))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content", hasSize(6)));
    }

    @Test
    @DisplayName("TC-41 La paginación respeta el tamaño de página con filtro (HU8 + HU20)")
    void tc41_filterWithPagination() throws Exception {
        seedCourts();
        Long futbol = categoryRepository.findByTitleIgnoreCase("Fútbol").orElseThrow().getId();
        mvc.perform(get("/api/courts").param("categoryIds", futbol.toString()).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));
        // Página fuera de rango en modo aleatorio no falla
        mvc.perform(get("/api/courts").param("random", "true").param("page", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("HU4 Modo aleatorio conserva todos los elementos (sin duplicar ni perder)")
    void randomKeepsAllItems() throws Exception {
        seedCourts();
        mvc.perform(get("/api/courts").param("random", "true").param("size", "10"))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content", hasSize(6)));
    }
}
