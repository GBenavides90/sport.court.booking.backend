package com.sport.court.booking.config;

import com.sport.court.booking.domain.Category;
import com.sport.court.booking.domain.Feature;
import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.repository.CategoryRepository;
import com.sport.court.booking.repository.FeatureRepository;
import com.sport.court.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Datos iniciales del Sprint 2:
 *  - Categorías base y características base (si las tablas están vacías).
 *  - Migración de las canchas existentes del Sprint 1 (columna de texto `category`) a la entidad Category.
 *  - Administrador inicial (HU16) configurable con ADMIN_EMAIL / ADMIN_PASSWORD.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final Map<String, String> LEGACY_TITLES = Map.of(
            "futbol", "Fútbol", "tenis", "Tenis", "padel", "Pádel", "squash", "Squash", "basquet", "Básquet");

    private final CategoryRepository categoryRepository;
    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(CategoryRepository categoryRepository, FeatureRepository featureRepository,
                           UserRepository userRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbc,
                           @Value("${app.admin.email:admin@sportcourt.com}") String adminEmail,
                           @Value("${app.admin.password:Admin#2026}") String adminPassword) {
        this.categoryRepository = categoryRepository;
        this.featureRepository = featureRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedCategories();
        seedFeatures();
        migrateLegacyCourtCategories();
        seedAdmin();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        categoryRepository.saveAll(List.of(
                category("Fútbol", "Canchas de fútbol 5, 7 y 11 con césped sintético o natural."),
                category("Tenis", "Canchas de tenis en arcilla y superficie dura."),
                category("Pádel", "Canchas de pádel con paredes de cristal."),
                category("Squash", "Canchas de squash cerradas y climatizadas."),
                category("Básquet", "Canchas de básquet techadas y al aire libre.")));
    }

    private void seedFeatures() {
        if (featureRepository.count() > 0) return;
        featureRepository.saveAll(List.of(
                new Feature(null, "Iluminación nocturna", "lighting"),
                new Feature(null, "Vestuarios", "lockers"),
                new Feature(null, "Duchas", "shower"),
                new Feature(null, "Estacionamiento", "parking"),
                new Feature(null, "Wi-Fi", "wifi"),
                new Feature(null, "Cafetería", "cafe")));
    }

    private void migrateLegacyCourtCategories() {
        try {
            // La columna de texto heredada ya no se usa: se permite NULL para que las canchas nuevas puedan guardarse.
            jdbc.execute("ALTER TABLE courts MODIFY category VARCHAR(255) NULL");
        } catch (DataAccessException e) {
            log.debug("Columna heredada courts.category no modificable (base nueva o dialecto distinto): {}", e.getMessage());
        }
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, category FROM courts WHERE category_id IS NULL AND category IS NOT NULL");
            for (Map<String, Object> row : rows) {
                String legacy = String.valueOf(row.get("category")).trim();
                String title = LEGACY_TITLES.getOrDefault(legacy.toLowerCase(),
                        legacy.isEmpty() ? "General" : Character.toUpperCase(legacy.charAt(0)) + legacy.substring(1));
                Category cat = categoryRepository.findByTitleIgnoreCase(title)
                        .orElseGet(() -> categoryRepository.save(category(title, "Canchas de " + title.toLowerCase() + ".")));
                jdbc.update("UPDATE courts SET category_id = ? WHERE id = ?", cat.getId(), row.get("id"));
            }
            if (!rows.isEmpty()) log.info("Migradas {} canchas existentes a la nueva entidad Categoría", rows.size());
        } catch (DataAccessException e) {
            // Base nueva: no existe la columna de texto heredada `category`. No hay nada que migrar.
            log.debug("Sin migración de categorías heredadas: {}", e.getMessage());
        }
    }

    private void seedAdmin() {
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) return;
        User admin = new User();
        admin.setFirstName("Administrador");
        admin.setLastName("Sport Court");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Administrador inicial creado: {}", email);
    }

    private Category category(String title, String description) {
        return new Category(null, title, description, null);
    }
}
